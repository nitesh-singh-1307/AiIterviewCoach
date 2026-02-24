package com.example.aiinterview.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiinterview.ui.main.ChatBubble
import com.example.aiinterview.ui.main.Sender
import com.aiinterviewcoach.presentation.theme.*
import com.example.aiinterview.domain.entity.ScoreResult
import com.example.aiinterview.domain.entity.ScoreTier

// ──────────────────────────────────────────────────────────────────────────────
// ChatBubble — Material3 styled, user right / AI left
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatBubbleItem(bubble: ChatBubble) {
    val isUser = bubble.sender == Sender.USER
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        if (!isUser) { AiAvatar(); Spacer(Modifier.width(8.dp)) }

        Column(
            modifier              = Modifier.widthIn(max = 310.dp),
            horizontalAlignment   = if (isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text     = if (isUser) "You" else "AI Interviewer",
                color    = TextMuted,
                style    = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            if (bubble.scoreResult != null) {
                ScoreCard(score = bubble.scoreResult)
            } else {
                val shape = RoundedCornerShape(
                    topStart    = 16.dp,
                    topEnd      = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd   = if (isUser) 4.dp  else 16.dp
                )
                Surface(
                    shape  = shape,
                    color  = if (isUser) UserBubble else BgCard,
                    border = if (isUser) null else BorderStroke(0.5.dp, BorderColor),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text     = bubble.text,
                        color    = TextPrimary,
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        if (isUser) { Spacer(Modifier.width(8.dp)); UserAvatar() }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Score card — rich animated result
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreCard(score: ScoreResult) {
    val accent = score.tier.color()
    val shape  = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)

    Surface(
        shape          = shape,
        color          = BgCard,
        border         = BorderStroke(0.5.dp, accent.copy(alpha = 0.4f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScoreDial(score = score.score, color = accent, size = 56.dp)
                Column {
                    Text(score.label, color = accent, style = MaterialTheme.typography.titleMedium)
                    Text("${score.score} / 10", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Summary
            Text(score.summary, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            if (score.strengths.isNotEmpty())
                FeedbackBlock("✅  Strengths",    score.strengths,    AccentGreen)
            if (score.improvements.isNotEmpty())
                FeedbackBlock("📈  To Improve",   score.improvements, AccentAmber)
        }
    }
}

@Composable
fun ScoreDial(score: Int, color: Color, size: Dp) {
    val progress by animateFloatAsState(
        targetValue   = score / 10f,
        animationSpec = tween(900, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
        label         = "scoreDial"
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
//        Canvas(Modifier.fillMaxSize()) {
//            val sw   = 4.dp.toPx()
//            val half = sw / 2f
//            val rect = Size(this.size.width - sw, this.size.height - sw)
//            drawArc(color.copy(0.15f), -90f, 360f,false, Stroke(sw),Offset(half, half), rect)
//            drawArc(color,-90f, 360f * progress, false, Stroke(sw, cap = StrokeCap.Round), Offset(half, half), rect)
//        }

        Canvas(Modifier.fillMaxSize()) {
            val sw   = 4.dp.toPx()
            val half = sw / 2f

            // ✅ Track (background circle)
            drawArc(
                color      = color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                style      = Stroke(width = sw),
                topLeft    = Offset(half, half),
                size       = Size(this.size.width - sw, this.size.height - sw)
            )

            // ✅ Progress arc
            drawArc(
                color      = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter  = false,
                style      = Stroke(width = sw, cap = StrokeCap.Round),
                topLeft    = Offset(half, half),
                size       = Size(this.size.width - sw, this.size.height - sw)
            )
        }

        Text("$score", color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun FeedbackBlock(title: String, items: List<String>, bulletColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color  = TextSecondary,
            style  = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .padding(top = 7.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(bulletColor)
                )
                Text(item, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Typing indicator — 3 animated dots while Claude thinks
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun TypingIndicator(label: String) {
    val inf = rememberInfiniteTransition(label = "typing")
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AiAvatar()
        Surface(
            shape  = RoundedCornerShape(12.dp, 12.dp, 12.dp, 3.dp),
            color  = BgCard,
            border = BorderStroke(0.5.dp, BorderColor)
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..2).forEach { i ->
                        val alpha by inf.animateFloat(
                            0.2f, 1f,
                            infiniteRepeatable(
                                tween(560, i * 160, easing = FastOutSlowInEasing),
                                RepeatMode.Reverse
                            ),
                            label = "dot$i"
                        )
                        Box(
                            Modifier.size(6.dp).clip(CircleShape).background(AccentBlue.copy(alpha = alpha))
                        )
                    }
                }
                Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Circular loading overlay — shown during API calls
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun LoadingOverlay(visible: Boolean, label: String = "Loading…") {
    if (!visible) return
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier  = Modifier.size(18.dp),
                color     = AccentBlue,
                strokeWidth = 2.dp
            )
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Shimmer loading skeleton
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "shimmer")
    val x by inf.animateFloat(
        -400f, 1200f,
        infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmerX"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BgCard)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(0.07f), Color.Transparent),
                        start = Offset(x, 0f), end = Offset(x + 300f, size.height)
                    )
                )
            }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Avatars
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun AiAvatar() {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF1B3A6B), AccentBlue.copy(0.6f))))
            .border(0.5.dp, AccentBlue.copy(0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("⚡", fontSize = 13.sp) }
}

@Composable
fun UserAvatar() {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(UserBubble)
            .border(0.5.dp, AccentCyan.copy(0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("👤", fontSize = 13.sp) }
}

// ──────────────────────────────────────────────────────────────────────────────
// Score chip (used in history rows)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreChip(score: Int, modifier: Modifier = Modifier) {
    val color = when {
        score >= 9 -> AccentGreen
        score >= 7 -> AccentCyan
        score >= 5 -> AccentAmber
        else       -> AccentRed
    }
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(0.5.dp, color.copy(0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$score/10", color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Extension: ScoreTier → accent color
// ──────────────────────────────────────────────────────────────────────────────

fun ScoreTier.color() = when (this) {
    ScoreTier.OUTSTANDING -> AccentGreen
    ScoreTier.STRONG      -> AccentCyan
    ScoreTier.DEVELOPING  -> AccentAmber
    ScoreTier.WEAK        -> AccentRed
}
