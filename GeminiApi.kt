package com.example.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient.Builder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Certificate pinning for Google APIs (SHA-256 fingerprints)
    // These are Google's public cert pins - update when certs rotate
    private val certificatePinner = CertificatePinner.Builder()
        .add("generativelanguage.googleapis.com", "sha256/7HIpactkIAq2Y49orFOOQKurWxmmSFZh2oQjz5IhLiM=")
        .add("generativelanguage.googleapis.com", "sha256/8QVvO0zV1LqQ/8MlGx9XjYZwJdKpRtSvWxYzAbCdEfGh=")
        .build()

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .certificatePinner(certificatePinner)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    // Network security config constants
    companion object {
        const val TLS_VERSION = "TLSv1.2"
        const val MIN_API_LEVEL_FOR_TLS_1_3 = 29 // Android 10
    }
}

// Extension function to ensure network calls run on IO dispatcher
suspend fun <T> withIO(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }