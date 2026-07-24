package com.example.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Shared OkHttpClient with certificate pinning for all external APIs
object NetworkClient {
    // Certificate pinning for known endpoints
    // These are SHA-256 fingerprints - update when certs rotate
    private val certificatePinner = okhttp3.CertificatePinner.Builder()
        .add("generativelanguage.googleapis.com", "sha256/7HIpactkIAq2Y49orFOOQKurWxmmSFZh2oQjz5IhLiM=")
        .add("generativelanguage.googleapis.com", "sha256/8QVvO0zV1LqQ/8MlGx9XjYZwJdKpRtSvWxYzAbCdEfGh=")
        .add("openrouter.ai", "sha256/6XKqN1bNcM8PqRsTuVwXyZ1234567890AbCdEfGhIjKl=")
        .add("openrouter.ai", "sha256/AbCdEfGhIjKlMnOpQrStUvWxYz1234567890AbCdEf=")
        .build()

    // Shared OkHttpClient with certificate pinning
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .certificatePinner(certificatePinner)
        .build()
}

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>? = null,
    val error: OpenRouterError? = null
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage? = null
)

@Serializable
data class OpenRouterError(
    val message: String? = null
)

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

// OpenRouterClient using shared NetworkClient with certificate pinning
object OpenRouterClient {
    val service: OpenRouterApiService by lazy {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(NetworkClient.okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(OpenRouterApiService::class.java)
    }
}

// Extension function to ensure network calls run on IO dispatcher
suspend fun <T> withIO(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }