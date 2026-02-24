package com.example.aiinterview.data.repository

import com.example.aiinterview.data.api.GroqApiService
import com.example.aiinterview.data.api.GroqMessage
import com.example.aiinterview.data.api.GroqRequest
import com.example.aiinterview.data.api.PromptTemplates
import com.example.aiinterview.data.local.dao.ChatSessionDao
import com.example.aiinterview.data.local.entity.ChatSessionEntity
import com.example.aiinterview.domain.entity.ChatSession
import com.example.aiinterview.domain.entity.InterviewQuestion
import com.example.aiinterview.domain.entity.ScoreResult
import com.example.aiinterview.domain.repository.InterviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ──────────────────────────────────────────────────────────────────────────────
// Groq model options (all free):
//   "llama-3.3-70b-versatile"     ← best quality, recommended
//   "llama-3.1-8b-instant"        ← fastest responses
//   "mixtral-8x7b-32768"          ← good balance
//   "gemma2-9b-it"                ← Google Gemma model
// ──────────────────────────────────────────────────────────────────────────────

private const val GROQ_MODEL       = "llama-3.3-70b-versatile"
private const val MAX_TOKENS_Q     = 300
private const val MAX_TOKENS_SCORE = 600

@Singleton
class InterviewRepositoryImpl @Inject constructor(
    private val api : GroqApiService,   // ✅ GroqApiService — not ClaudeApiService
    private val dao : ChatSessionDao
) : InterviewRepository {

    // ── Groq API — get question ───────────────────────────────────────────────

    override suspend fun getQuestion(topic: String): Result<InterviewQuestion> = runCatching {
        val resp = api.sendMessage(
            GroqRequest(
                model     = GROQ_MODEL,
                maxTokens = MAX_TOKENS_Q,
                messages  = listOf(
                    // ✅ Groq uses "system" role — separate from user message
                    GroqMessage(
                        role    = "system",
                        content = PromptTemplates.SYSTEM_PROMPT
                    ),
                    GroqMessage(
                        role    = "user",
                        content = PromptTemplates.questionPrompt(topic)
                    )
                )
            )
        )

        // ✅ Extract text from Groq response — choices[0].message.content
        val raw = resp.choices.firstOrNull()?.message?.content
            ?: error("Groq returned an empty response")

        android.util.Log.d("GROQ_API", "📥 Question response: $raw")

        val questionText = raw.lineSequence()
            .firstOrNull { it.trimStart().startsWith("QUESTION:") }
            ?.removePrefix("QUESTION:")
            ?.trim()
            ?: raw.trim()   // graceful fallback

        InterviewQuestion(text = questionText, topic = topic)
    }

    // ── Groq API — score answer ───────────────────────────────────────────────

    override suspend fun scoreAnswer(
        question : InterviewQuestion,
        answer   : String
    ): Result<ScoreResult> = runCatching {
        val resp = api.sendMessage(
            GroqRequest(
                model     = GROQ_MODEL,
                maxTokens = MAX_TOKENS_SCORE,
                messages  = listOf(
                    GroqMessage(
                        role    = "system",
                        content = PromptTemplates.SYSTEM_PROMPT
                    ),
                    GroqMessage(
                        role    = "user",
                        content = PromptTemplates.scoringPrompt(question.text, answer)
                    )
                )
            )
        )

        // ✅ Extract text from Groq response
        val raw = resp.choices.firstOrNull()?.message?.content
            ?: error("Groq returned empty scoring response")

        android.util.Log.d("GROQ_API", "📥 Score response: $raw")

        parseScore(raw)
    }

    // ── Parser — unchanged, works for any AI response ─────────────────────────

    private fun parseScore(raw: String): ScoreResult {
        val lines = raw.lines()

        fun field(prefix: String) = lines
            .firstOrNull { it.trimStart().startsWith(prefix) }
            ?.substringAfter(prefix)
            ?.trim()

        fun bullets(header: String): List<String> {
            val idx = lines.indexOfFirst { it.trimStart().startsWith(header) }
            if (idx == -1) return emptyList()
            return lines.drop(idx + 1)
                .takeWhile { it.trimStart().startsWith("-") }
                .map { it.trimStart().removePrefix("-").trim() }
                .filter { it.isNotBlank() }
        }

        return ScoreResult(
            score        = field("SCORE:")?.toIntOrNull()?.coerceIn(1, 10) ?: 5,
            label        = field("LABEL:") ?: "Developing",
            strengths    = bullets("STRENGTHS:").ifEmpty { listOf("Answered the question") },
            improvements = bullets("IMPROVEMENTS:").ifEmpty { listOf("Provide more technical depth") },
            summary      = field("SUMMARY:") ?: raw.take(200)
        )
    }

    // ── Room persistence — unchanged ──────────────────────────────────────────

    override suspend fun saveSession(
        topic    : String,
        question : String,
        answer   : String,
        score    : ScoreResult
    ): Long = dao.insert(
        ChatSessionEntity(
            topic    = topic,
            question = question,
            answer   = answer,
            score    = score.score,
            label    = score.label,
            summary  = score.summary
        )
    )

    override fun getAllSessions(): Flow<List<ChatSession>> =
        dao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override suspend fun deleteSession(id: Long)  = dao.deleteById(id)
    override suspend fun averageScore(): Float?    = dao.averageScore()
    override suspend fun sessionCount(): Int       = dao.count()

    // ── Mapper — unchanged ────────────────────────────────────────────────────

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id        = id,
        createdAt = createdAt,
        topic     = topic,
        question  = question,
        answer    = answer,
        score     = score,
        label     = label,
        summary   = summary
    )
}
