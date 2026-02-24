package com.example.aiinterview.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

// ──────────────────────────────────────────────────────────────────────────────
// Groq API DTOs — OpenAI-compatible format
// Docs: https://console.groq.com/docs/openai
// ──────────────────────────────────────────────────────────────────────────────

// ── Request ───────────────────────────────────────────────────────────────────

data class GroqRequest(
    @SerializedName("model")       val model       : String,
    @SerializedName("messages")    val messages    : List<GroqMessage>,
    @SerializedName("temperature") val temperature : Float = 0.7f,
    @SerializedName("max_tokens")  val maxTokens   : Int   = 1024
)

data class GroqMessage(
    @SerializedName("role")    val role    : String,  // "system" | "user" | "assistant"
    @SerializedName("content") val content : String
)

// ── Response ──────────────────────────────────────────────────────────────────

data class GroqResponse(
    @SerializedName("id")      val id      : String,
    @SerializedName("choices") val choices : List<GroqChoice>,
    @SerializedName("usage")   val usage   : GroqUsage?
)

data class GroqChoice(
    @SerializedName("index")         val index        : Int,
    @SerializedName("message")       val message      : GroqMessage,
    @SerializedName("finish_reason") val finishReason : String?
)

data class GroqUsage(
    @SerializedName("prompt_tokens")     val promptTokens     : Int,
    @SerializedName("completion_tokens") val completionTokens : Int,
    @SerializedName("total_tokens")      val totalTokens      : Int
)

// ──────────────────────────────────────────────────────────────────────────────
// Retrofit interface
// Authorization header injected via OkHttp interceptor in AppModules.kt
// ──────────────────────────────────────────────────────────────────────────────

interface GroqApiService {

    // ✅ Groq endpoint — no extra headers needed (handled by interceptor)
    @POST("chat/completions")
    suspend fun sendMessage(@Body request: GroqRequest): GroqResponse
}
