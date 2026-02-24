package com.example.aiinterview.usecase

import com.example.aiinterview.domain.entity.ChatSession
import com.example.aiinterview.domain.entity.InterviewQuestion
import com.example.aiinterview.domain.entity.ScoreResult
import com.example.aiinterview.domain.repository.InterviewRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// Each use case is a single-method class that can be independently tested.
// ──────────────────────────────────────────────────────────────────────────────

class GetQuestionUseCase @Inject constructor(
    private val repo: InterviewRepository
) {
    /** Sanitises topic, then delegates to the repository. */
    suspend operator fun invoke(topic: String): Result<InterviewQuestion> {
        val safeTopic = topic.trim().ifBlank { "Kotlin and Jetpack Compose" }
        return repo.getQuestion(safeTopic)
    }
}

class ScoreAnswerUseCase @Inject constructor(
    private val repo: InterviewRepository
) {
    suspend operator fun invoke(
        question : InterviewQuestion,
        answer   : String
    ): Result<ScoreResult> {
        if (answer.isBlank())
            return Result.failure(IllegalArgumentException("Answer must not be empty"))
        return repo.scoreAnswer(question, answer)
    }
}

class SaveSessionUseCase @Inject constructor(
    private val repo: InterviewRepository
) {
    suspend operator fun invoke(
        topic    : String,
        question : String,
        answer   : String,
        score    : ScoreResult
    ): Long = repo.saveSession(topic, question, answer, score)
}

class GetAllSessionsUseCase @Inject constructor(
    private val repo: InterviewRepository
) {
    operator fun invoke(): Flow<List<ChatSession>> = repo.getAllSessions()
}

class DeleteSessionUseCase @Inject constructor(
    private val repo: InterviewRepository
) {
    suspend operator fun invoke(id: Long) = repo.deleteSession(id)
}
