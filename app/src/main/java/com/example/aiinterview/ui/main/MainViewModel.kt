package com.example.aiinterview.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiinterview.domain.entity.ChatSession
import com.example.aiinterview.domain.entity.InterviewQuestion
import com.example.aiinterview.domain.entity.ScoreResult
import com.example.aiinterview.usecase.DeleteSessionUseCase
import com.example.aiinterview.usecase.GetAllSessionsUseCase
import com.example.aiinterview.usecase.GetQuestionUseCase
import com.example.aiinterview.usecase.SaveSessionUseCase
import com.example.aiinterview.usecase.ScoreAnswerUseCase
import com.example.aiinterview.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// State
// ──────────────────────────────────────────────────────────────────────────────

enum class ChatPhase {
    IDLE,             // Initial — waiting for topic + start
    LOADING_Q,        // Calling Claude for a question
    AWAITING_ANSWER,  // Question shown; user typing
    LOADING_SCORE,    // Evaluating user's answer
    SCORED,           // Result shown; can start another
}

enum class Sender { USER, AI }

data class ChatBubble(
    val id          : Long = System.nanoTime(),
    val sender      : Sender,
    val text        : String,
    val scoreResult : ScoreResult? = null   // only for AI score bubbles
)

data class ChatState(
    val phase           : ChatPhase           = ChatPhase.IDLE,
    val topicInput      : String              = "",
    val answerInput     : String              = "",
    val messages        : List<ChatBubble>    = emptyList(),
    val pastSessions    : List<ChatSession>   = emptyList(),
    val historyExpanded : Boolean             = false,
    val sessionStats    : SessionStats        = SessionStats(),
    val currentQuestion : InterviewQuestion?  = null
)

data class SessionStats(
    val total        : Int   = 0,
    val averageScore : Float = 0f
)

// ──────────────────────────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getQuestionUseCase  : GetQuestionUseCase,
    private val scoreAnswerUseCase  : ScoreAnswerUseCase,
    private val saveSessionUseCase  : SaveSessionUseCase,
    private val getAllSessionsUseCase: GetAllSessionsUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    /** Single-shot events the screen consumes once (toasts, scrolls, keyboard). */
    val events = Channel<UiEvent>(Channel.BUFFERED)

    private var apiJob: Job? = null

    init { observeHistory() }

    // ── History observer ──────────────────────────────────────────────────────

    private fun observeHistory() {
        viewModelScope.launch {
            getAllSessionsUseCase().collect { sessions ->
                val avg = if (sessions.isEmpty()) 0f
                else sessions.map { it.score }.average().toFloat()
                _state.update {
                    it.copy(
                        pastSessions = sessions,
                        sessionStats = SessionStats(sessions.size, avg)
                    )
                }
            }
        }
    }

    // ── User inputs ───────────────────────────────────────────────────────────

    fun onTopicChanged(v: String)  = _state.update { it.copy(topicInput  = v) }
    fun onAnswerChanged(v: String) = _state.update { it.copy(answerInput = v) }
    fun onToggleHistory()          = _state.update { it.copy(historyExpanded = !it.historyExpanded) }
    fun onVoiceResult(text: String)= _state.update { it.copy(answerInput = text) }

    // ── Start Practice ────────────────────────────────────────────────────────

    fun startPractice() {
        apiJob?.cancel()
        apiJob = viewModelScope.launch {
            val topic = _state.value.topicInput.trim().ifBlank { "Kotlin & Jetpack Compose" }
            _state.update { it.copy(phase = ChatPhase.LOADING_Q) }
            events.send(UiEvent.DismissKeyboard)

            getQuestionUseCase(topic)
                .onSuccess { q ->
                    _state.update {
                        it.copy(
                            phase           = ChatPhase.AWAITING_ANSWER,
                            currentQuestion = q,
                            messages        = it.messages + ChatBubble(sender = Sender.AI, text = q.text),
                            answerInput     = ""
                        )
                    }
                    events.send(UiEvent.ScrollToBottom)
                }
                .onFailure { err ->
                    _state.update { it.copy(phase = ChatPhase.IDLE) }
                    events.send(UiEvent.ShowToast(err.toUiMessage()))
                }
        }
    }

    // ── Submit Answer ─────────────────────────────────────────────────────────

    fun submitAnswer() {
        val s      = _state.value
        val answer = s.answerInput.trim()
        val q      = s.currentQuestion ?: return
        if (answer.isBlank()) return

        apiJob?.cancel()
        apiJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    phase       = ChatPhase.LOADING_SCORE,
                    messages    = it.messages + ChatBubble(sender = Sender.USER, text = answer),
                    answerInput = ""
                )
            }
            events.send(UiEvent.DismissKeyboard)
            events.send(UiEvent.ScrollToBottom)

            scoreAnswerUseCase(q, answer)
                .onSuccess { score ->
                    // Persist immediately — user never loses data
                    saveSessionUseCase(q.topic, q.text, answer, score)

                    _state.update {
                        it.copy(
                            phase    = ChatPhase.SCORED,
                            messages = it.messages + ChatBubble(
                                sender      = Sender.AI,
                                text        = score.summary,
                                scoreResult = score
                            )
                        )
                    }
                    events.send(UiEvent.ScrollToBottom)
                }
                .onFailure { err ->
                    _state.update { it.copy(phase = ChatPhase.AWAITING_ANSWER) }
                    events.send(UiEvent.ShowToast(err.toUiMessage()))
                }
        }
    }

    // ── Next question (same topic) ────────────────────────────────────────────

    fun practiceAnother() {
        _state.update { it.copy(phase = ChatPhase.LOADING_Q) }
        viewModelScope.launch {
            val topic = _state.value.topicInput.trim().ifBlank { "Kotlin & Jetpack Compose" }
            getQuestionUseCase(topic)
                .onSuccess { q ->
                    _state.update {
                        it.copy(
                            phase           = ChatPhase.AWAITING_ANSWER,
                            currentQuestion = q,
                            messages        = it.messages + ChatBubble(sender = Sender.AI, text = q.text),
                            answerInput     = ""
                        )
                    }
                    events.send(UiEvent.ScrollToBottom)
                }
                .onFailure { err ->
                    _state.update { it.copy(phase = ChatPhase.SCORED) }
                    events.send(UiEvent.ShowToast(err.toUiMessage()))
                }
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { deleteSessionUseCase(id) }
    }

    fun resetSession() {
        apiJob?.cancel()
        _state.update { it.copy(phase = ChatPhase.IDLE, messages = emptyList(), answerInput = "", currentQuestion = null) }
    }

    // ── Error message translation ─────────────────────────────────────────────

    private fun Throwable.toUiMessage() = when {
        message?.contains("401")     == true -> "❌ Invalid API key — update ANTHROPIC_API_KEY in AppModules.kt"
        message?.contains("429")     == true -> "⏳ Rate limit reached. Please wait a moment."
        message?.contains("timeout") == true -> "📡 Request timed out. Check your connection."
        message?.contains("network") == true -> "📡 Network error. Check your connection."
        else -> "⚠️ ${message?.take(100) ?: "Unknown error"}"
    }
}
