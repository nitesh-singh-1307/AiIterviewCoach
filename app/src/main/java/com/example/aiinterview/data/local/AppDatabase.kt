package com.example.aiinterview.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aiinterview.data.local.dao.ChatSessionDao
import com.example.aiinterview.data.local.entity.ChatSessionEntity

@Database(
    entities  = [ChatSessionEntity::class],
    version   = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao

    companion object {
        const val DB_NAME = "ai_interview_coach.db"
    }
}
