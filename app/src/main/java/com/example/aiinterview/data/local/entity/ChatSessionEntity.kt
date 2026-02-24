package com.example.aiinterview.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for one completed practice session.
 * Stored after Claude returns a score so data is never lost.
 */
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "topic")
    val topic: String,

    @ColumnInfo(name = "question")
    val question: String,

    @ColumnInfo(name = "answer")
    val answer: String,

    @ColumnInfo(name = "score")
    val score: Int,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "summary")
    val summary: String
)
