package com.example.aiinterview.ui.main

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiinterviewcoach.presentation.theme.*
import com.example.aiinterview.domain.entity.ChatSession
import com.example.aiinterview.ui.components.ChatBubbleItem
import com.example.aiinterview.ui.components.ScoreChip
import com.example.aiinterview.ui.components.ShimmerBox
import com.example.aiinterview.ui.components.TypingIndicator
import com.example.aiinterview.utils.SpeechState
import com.example.aiinterview.utils.UiEvent
import com.example.aiinterview.utils.rememberSpeechHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import java.text.SimpleDateFormat
import java.util.*

// ──────────────────────────────────────────────────────────────────────────────
// MainScreen — single-activity, everything happens here
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state     by viewModel.state.collectAsState()
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()
    val context    = LocalContext.current
    val focusMgr   = LocalFocusManager.current
    val haptic     = LocalHapticFeedback.current

    // ── Voice input ───────────────────────────────────────────────────────────
    val (speechState, startListening) = rememberSpeechHelper { text ->
        viewModel.onVoiceResult(text)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // ── One-shot events ───────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.receiveAsFlow().collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast    -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is UiEvent.DismissKeyboard -> focusMgr.clearFocus()
                is UiEvent.ScrollToBottom  -> {
                    if (state.messages.isNotEmpty())
                        scope.launch { listState.animateScrollToItem(state.messages.lastIndex) }
                }
            }
        }
    }

    // Auto-scroll when messages grow
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty())
            scope.launch { listState.animateScrollToItem(state.messages.lastIndex) }
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(BgDeep)) {
        GridBackground()

        Column(Modifier.fillMaxSize()) {

            // Top bar
            TopBar(
                phase   = state.phase,
                stats   = state.sessionStats,
                onReset = viewModel::resetSession
            )

            // Collapsible history panel
            AnimatedVisibility(
                visible = state.pastSessions.isNotEmpty(),
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                HistoryPanel(
                    sessions  = state.pastSessions,
                    expanded  = state.historyExpanded,
                    onToggle  = viewModel::onToggleHistory,
                    onDelete  = viewModel::deleteSession
                )
            }

            // Chat list — fills all remaining vertical space
            Box(Modifier.weight(1f)) {
                if (state.messages.isEmpty() && state.phase == ChatPhase.IDLE) {
                    EmptyStateView()
                } else {
                    ChatListView(
                        state     = state,
                        listState = listState
                    )
                }
            }

            // Input panel
            InputPanel(
                state           = state,
                speechState     = speechState,
                onTopicChanged  = viewModel::onTopicChanged,
                onAnswerChanged = viewModel::onAnswerChanged,
                onStart         = { focusMgr.clearFocus(); viewModel.startPractice() },
                onSubmit        = { focusMgr.clearFocus(); viewModel.submitAnswer() },
                onNext          = viewModel::practiceAnother,
                onMic           = startListening
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Top bar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(phase: ChatPhase, stats: SessionStats, onReset: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "dot")
    val dotAlpha by inf.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot"
    )
    val isActive = phase != ChatPhase.IDLE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSurface)
            .drawBehind { drawLine(BorderColor, Offset(0f, size.height), Offset(size.width, size.height), 0.7f) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape)
            .background(if (isActive) AccentGreen.copy(alpha = dotAlpha) else TextMuted))

        Column(Modifier.weight(1f)) {
            Text("AI Interview Coach", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            if (stats.total > 0) {
                Text(
                    "${stats.total} sessions · avg ${"%.1f".format(stats.averageScore)}/10",
                    color = TextMuted, style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Phase chip
        val (chipLabel, chipColor) = when (phase) {
            ChatPhase.IDLE            -> "ready"       to TextMuted
            ChatPhase.LOADING_Q       -> "fetching…"   to AccentAmber
            ChatPhase.AWAITING_ANSWER -> "your turn"   to AccentCyan
            ChatPhase.LOADING_SCORE   -> "evaluating…" to AccentAmber
            ChatPhase.SCORED          -> "scored"      to AccentGreen
        }
        Text(
            chipLabel, color = chipColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(chipColor.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )

        if (phase != ChatPhase.IDLE) {
            IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Refresh, "Reset", tint = TextSecondary, modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// History panel (collapsible)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryPanel(
    sessions : List<ChatSession>,
    expanded : Boolean,
    onToggle : () -> Unit,
    onDelete : (Long) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(BgSurface)
            .drawBehind { drawLine(BorderColor, Offset(0f, size.height), Offset(size.width, size.height), 0.7f) }
    ) {
        // Toggle header
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null, tint = TextSecondary, modifier = Modifier.size(18.dp)
            )
            Text("Past Sessions", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(AccentBlue.copy(0.15f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text("${sessions.size}", color = AccentBlue, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Collapsible session list
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(tween(220)),
            exit    = shrinkVertically(tween(180))
        ) {
            Column(
                Modifier
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                sessions.forEach { session ->
                    HistoryRow(session, onDelete = { onDelete(session.id) })
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(session: ChatSession, onDelete: () -> Unit) {
    val date = remember(session.createdAt) {
        SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault()).format(Date(session.createdAt))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScoreChip(score = session.score, modifier = Modifier)
        Column(Modifier.weight(1f)) {
            Text(
                session.topic, color = TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${session.label} · $date",
                color = TextMuted, style = MaterialTheme.typography.labelSmall
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Delete, "Delete", tint = TextMuted, modifier = Modifier.size(15.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Chat list
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatListView(state: ChatState, listState: LazyListState) {
    LazyColumn(
        state             = listState,
        contentPadding    = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier          = Modifier.fillMaxSize()
    ) {
        items(state.messages, key = { it.id }) { bubble ->
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn(tween(280)) + slideInVertically(tween(280, easing = EaseOutQuart)) { it / 3 }
            ) {
                ChatBubbleItem(bubble)
            }
        }

        // Typing indicator while loading
        if (state.phase == ChatPhase.LOADING_Q || state.phase == ChatPhase.LOADING_SCORE) {
            item("typing") {
                AnimatedVisibility(visible = true, enter = fadeIn(tween(200))) {
                    TypingIndicator(
                        label = when (state.phase) {
                            ChatPhase.LOADING_Q     -> "Generating your question…"
                            ChatPhase.LOADING_SCORE -> "Evaluating your answer…"
                            else                    -> "…"
                        }
                    )
                }
            }
        }
        item("bottom") { Spacer(Modifier.height(8.dp)) }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Empty state
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateView() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⌨", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Senior Android Interview",
            color = TextPrimary, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose a topic and tap Start Practice.\nClaude will ask one deep technical question.",
            color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Coroutines", "Compose", "Architecture", "HAL").forEach { tag ->
                Text(
                    tag, color = AccentBlue, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(0.5.dp, AccentBlue.copy(0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Input panel — morphs between phases via AnimatedContent
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InputPanel(
    state           : ChatState,
    speechState     : SpeechState,
    onTopicChanged  : (String) -> Unit,
    onAnswerChanged : (String) -> Unit,
    onStart         : () -> Unit,
    onSubmit        : () -> Unit,
    onNext          : () -> Unit,
    onMic           : () -> Unit
) {
    Surface(
        color    = BgSurface,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(BorderColor, Offset(0f, 0f), Offset(size.width, 0f), 0.7f) }
    ) {
        Box(Modifier.padding(14.dp)) {
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 5 })
                        .togetherWith(fadeOut(tween(130)))
                },
                label = "inputPanel"
            ) { phase ->
                when (phase) {
                    ChatPhase.IDLE -> IdleInput(state.topicInput, onTopicChanged, onStart)
                    ChatPhase.LOADING_Q, ChatPhase.LOADING_SCORE -> LoadingInput()
                    ChatPhase.AWAITING_ANSWER -> AnswerInput(
                        answer          = state.answerInput,
                        onAnswerChanged = onAnswerChanged,
                        onSubmit        = onSubmit,
                        onMic           = onMic,
                        micAvailable    = speechState.isAvailable,
                        micListening    = speechState.isListening
                    )
                    ChatPhase.SCORED -> ScoredInput(onNext)
                }
            }
        }
    }
}

// ── Phase panels ─────────────────────────────────────────────────────────────

@Composable
private fun IdleInput(topic: String, onChanged: (String) -> Unit, onStart: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value            = topic,
            onValueChange    = onChanged,
            placeholder      = { Text("Topic: Coroutines, Compose, HAL, Architecture…", color = TextMuted, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon      = { Icon(Icons.Outlined.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            modifier         = Modifier.fillMaxWidth(),
            shape            = RoundedCornerShape(10.dp),
            colors           = fieldColors(AccentBlue, BorderColor),
            singleLine       = true,
            keyboardOptions  = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions  = KeyboardActions(onDone = { onStart() })
        )
        Button(
            onClick  = onStart,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = BgDeep)
        ) {
            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Start Practice", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AnswerInput(
    answer          : String,
    onAnswerChanged : (String) -> Unit,
    onSubmit        : () -> Unit,
    onMic           : () -> Unit,
    micAvailable    : Boolean,
    micListening    : Boolean
) {
    val micInf = rememberInfiniteTransition(label = "mic")
    val micPulse by micInf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "micPulse"
    )

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("YOUR ANSWER", color = AccentCyan, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("${answer.length} chars", color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }

        OutlinedTextField(
            value           = answer,
            onValueChange   = onAnswerChanged,
            placeholder     = { Text("Speak or type your answer… STAR method works well.", color = TextMuted, style = MaterialTheme.typography.bodyMedium) },
            modifier        = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 190.dp),
            shape           = RoundedCornerShape(10.dp),
            colors          = fieldColors(AccentCyan, BorderColor),
            maxLines        = 8,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            // Mic button
            if (micAvailable) {
                IconButton(
                    onClick  = onMic,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (micListening) AccentRed.copy(micPulse * 0.22f) else BgCardAlt)
                        .border(0.5.dp, if (micListening) AccentRed else BorderColor, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        if (micListening) Icons.Filled.Mic else Icons.Outlined.Mic,
                        "Voice input",
                        tint     = if (micListening) AccentRed else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Submit button
            Button(
                onClick  = onSubmit,
                enabled  = answer.isNotBlank(),
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = AccentGreen,
                    contentColor           = BgDeep,
                    disabledContainerColor = BgCardAlt,
                    disabledContentColor   = TextMuted
                )
            ) {
                Icon(Icons.Filled.Send, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Submit Answer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun LoadingInput() {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().height(48.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.6f).height(48.dp))
    }
}

@Composable
private fun ScoredInput(onNext: () -> Unit) {
    OutlinedButton(
        onClick  = onNext,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(0.7.dp, AccentBlue)
    ) {
        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(17.dp), tint = AccentBlue)
        Spacer(Modifier.width(8.dp))
        Text("Next Question", color = AccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun fieldColors(focused: Color, unfocused: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = focused,
        unfocusedBorderColor    = unfocused,
        focusedTextColor        = TextPrimary,
        unfocusedTextColor      = TextPrimary,
        cursorColor             = focused,
        focusedContainerColor   = BgCard,
        unfocusedContainerColor = BgCard
    )

@Composable
private fun GridBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        val c    = Color(0xFF0D1117)
        var x = 0f; while (x < size.width)  { drawLine(c, Offset(x, 0f), Offset(x, size.height), 0.6f); x += step }
        var y = 0f; while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y), 0.6f); y += step }
    }
}

private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
