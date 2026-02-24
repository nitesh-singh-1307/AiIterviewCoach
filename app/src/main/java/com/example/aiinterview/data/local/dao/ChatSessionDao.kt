package com.example.aiinterview.data.local.dao

import androidx.room.*
import com.example.aiinterview.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity): Long

    /** Live-updating stream; Room re-emits whenever the table changes. */
    @Query("SELECT * FROM chat_sessions ORDER BY created_at DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<ChatSessionEntity>

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun count(): Int

    @Query("SELECT AVG(score) FROM chat_sessions")
    suspend fun averageScore(): Float?
}
