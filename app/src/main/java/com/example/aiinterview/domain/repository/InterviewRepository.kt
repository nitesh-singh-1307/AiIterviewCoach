package com.example.aiinterview.domain.repository

import com.example.aiinterview.domain.entity.ChatSession
import com.example.aiinterview.domain.entity.InterviewQuestion
import com.example.aiinterview.domain.entity.ScoreResult
import kotlinx.coroutines.flow.Flow

/**
 * Single contract between domain and data layer.
 * The data layer owns the implementation; domain never knows about
 * Retrofit, Room, or any Android API directly.
 */
interface InterviewRepository {

    // ── Claude API ────────────────────────────────────────────────────────────
    /** Ask Claude for exactly one interview question on [topic]. */
    suspend fun getQuestion(topic: String): Result<InterviewQuestion>

    /** Ask Claude to evaluate [answer] against [question]; returns a score. */
    suspend fun scoreAnswer(question: InterviewQuestion, answer: String): Result<ScoreResult>

    // ── Persistence (Room) ────────────────────────────────────────────────────
    suspend fun saveSession(
        topic    : String,
        question : String,
        answer   : String,
        score    : ScoreResult
    ): Long

    /** Live-updating stream of all past sessions, newest first. */
    fun getAllSessions(): Flow<List<ChatSession>>

    suspend fun deleteSession(id: Long)
    suspend fun averageScore(): Float?
    suspend fun sessionCount(): Int
}
