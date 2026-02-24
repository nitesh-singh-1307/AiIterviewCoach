package com.example.aiinterview.di

import android.content.Context
import androidx.room.Room
import com.example.aiinterview.BuildConfig
import com.example.aiinterview.data.api.GroqApiService
import com.example.aiinterview.data.local.AppDatabase
import com.example.aiinterview.data.local.dao.ChatSessionDao
import com.example.aiinterview.data.repository.InterviewRepositoryImpl
import com.example.aiinterview.domain.repository.InterviewRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// ──────────────────────────────────────────────────────────────────────────────
// NetworkModule — Groq API setup
//
// 1. Get your free API key from https://console.groq.com
// 2. Add to app/build.gradle.kts:
//    buildConfigField("String", "GROQ_API_KEY", "\"your-key-here\"")
// ──────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    // ✅ Groq uses Bearer token — NOT x-api-key
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = chain.proceed(request)

                // Log errors for debugging
                if (!response.isSuccessful) {
                    val errorBody = response.peekBody(Long.MAX_VALUE).string()
                    android.util.Log.e("GROQ_API", "❌ HTTP ${response.code}: $errorBody")
                }
                response
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            // ✅ Groq base URL — NOT api.anthropic.com
            .baseUrl("https://api.groq.com/openai/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    // ✅ GroqApiService — NOT ClaudeApiService
    fun provideGroqApiService(retrofit: Retrofit): GroqApiService =
        retrofit.create(GroqApiService::class.java)
}

// ──────────────────────────────────────────────────────────────────────────────
// DatabaseModule — unchanged
// ──────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChatSessionDao(db: AppDatabase): ChatSessionDao = db.chatSessionDao()
}

// ──────────────────────────────────────────────────────────────────────────────
// RepositoryModule — unchanged
// ──────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindInterviewRepository(
        impl: InterviewRepositoryImpl
    ): InterviewRepository
}
