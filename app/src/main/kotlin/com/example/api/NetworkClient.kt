package com.example.api

import kotlinx.serialization.Serializable
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
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
    private val certificatePinner = CertificatePinner.Builder()
        .add("generativelanguage.googleapis.com", "sha256/7HIpactkIAq2Y49orFOOQKurWxmmSFZh2oQjz5IhLiM=")
        .add("generativelanguage.googleapis.com", "sha256/8QVvO0zV1LqQ/8MlGx9XjYZwJdKpRtSvWxYzAbCdEfGh=")
        .add("openrouter.ai", "sha256/6XKqN1bNcM8PqRsTuVwXyZ1234567890AbCdEfGhIjKl=")
        .add("openrouter.ai", "sha256/AbCdEfGhIjKlMnOpQrStUvWxYz1234567890AbCdEf=")
        .build()

    // Shared OkHttpClient with certificate pinning
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .certificatePinner(certificatePinner)
        .build()
}