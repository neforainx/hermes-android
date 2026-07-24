package com.example.ipc

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.net.*
import java.nio.channels.*

class HermesSocketClient(
    private val context: Context,
    private val socketPath: String = "hermes.sock",
    private val port: Int = 5175
) {
    private var unixSocket: Socket? = null
    private var tcpSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val responseChannel = Channel<String>(Channel.UNLIMITED)
    private val json = Json { ignoreUnknownKeys = true }
    private var connected = false

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return try {
            // Try Unix socket first (faster, local only)
            if (connectUnix()) {
                true
            } else {
                connectTcp()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun connectUnix(): Boolean {
        try {
            val socketFile = File(context.filesDir, "hermes.sock")
            if (!socketFile.exists()) return false
            
            unixSocket = Socket()
            val address = java.net.InetSocketAddress("127.0.0.1", 5175)
            // Unix domain socket not directly supported in older Android
            // Fall back to TCP for compatibility
            return false
        } catch (e: Exception) {
            false
        }
    }

    private fun connectTcp(): Boolean {
        try {
            tcpSocket = Socket()
            tcpSocket?.soTimeout = 10000
            tcpSocket?.connect(InetSocketAddress("127.0.0.1", 5175), 5000)
            
            tcpSocket?.let { socket ->
                writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
                
                // Start listening for responses
                scope.launch {
                    reader?.forEachLine { line ->
                        responseChannel.trySend(line)
                    }
                }
                
                connected = true
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var tcpSocket: Socket? = null
    private var unixSocket: Socket? = null

    suspend fun sendCommand(cmd: String, data: Map<String, Any> = emptyMap()): String = withContext(Dispatchers.IO) {
        if (!connected) {
            if (!connect()) return "{\"error\": \"Not connected\"}"
        }
        
        val request = json.encodeToString(buildMap {
            put("cmd", cmd)
            put("data", data)
        })
        
        return try {
            writer?.println(request)
            writer?.flush()
            
            // Wait for response with timeout
            withTimeout(10000) {
                responseChannel.receive()
            }
        } catch (e: Exception) {
            connected = false
            "{\"error\": \"${e.message}\"}"
        }
    }

    // High-level API methods
    
    suspend fun chat(message: String, provider: String = "nous", model: String? = null): String = withContext(Dispatchers.IO) {
        val response = sendCommand("chat", mapOf(
            "message" to message,
            "provider" to provider,
            "model" to model
        ))
        parseChatResponse(response)
    }

    suspend fun terminal(cmd: String, sessionId: String = "default"): TerminalResult = withContext(Dispatchers.IO) {
        val response = sendCommand("terminal", mapOf("cmd" to cmd, "session_id" to sessionId))
        parseTerminalResponse(response)
    }

    suspend fun cron(action: String, data: Map<String, Any> = emptyMap()): String = withContext(Dispatchers.IO) {
        sendCommand("cron", buildMap {
            put("action", action)
            putAll(data)
        })
    }

    suspend fun skill(skill: String, action: String, params: Map<String, Any> = emptyMap()): String = withContext(Dispatchers.IO) {
        sendCommand("skill", buildMap {
            put("skill", skill)
            put("action", action)
            putAll(params)
        })
    }

    suspend fun settings(action: String, key: String? = null, value: Any? = null): String = withContext(Dispatchers.IO) {
        sendCommand("settings", buildMap {
            put("action", action)
            key?.let { put("key", it) }
            value?.let { put("value", it) }
        })
    }

    suspend fun session(action: String, sessionId: String? = null, name: String? = null): String = withContext(Dispatchers.IO) {
        sendCommand("session", buildMap {
            put("action", action)
            sessionId?.let { put("session_id", it) }
            name?.let { put("name", it) }
        })
    }

    suspend fun status(): String = withContext(Dispatchers.IO) {
        sendCommand("status", emptyMap())
    }

    // Response parsers
    private fun parseChatResponse(json: String): String {
        return try {
            val obj = Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, Any>>(json)
            obj["response"] as? String ?: obj["error"] as? String ?: "No response"
        } catch (e: Exception) {
            "Error parsing response: ${e.message}"
        }
    }

    private fun parseTerminalResponse(json: String): TerminalResult {
        return try {
            val obj = json.decodeFromString<Map<String, Any>>()
            TerminalResult(
                stdout = obj["stdout"] as? String ?: "",
                stderr = obj["stderr"] as? String ?: "",
                returnCode = obj["returncode"] as? Int ?: -1,
                sessionId = obj["session_id"] as? String ?: "default"
            )
        } catch (e: Exception) {
            TerminalResult("", "Parse error: ${e.message}", -1, "default")
        }
    }

    fun disconnect() {
        scope.cancel()
        try { unixSocket?.close() } catch (e: Exception) {}
        try { tcpSocket?.close() } catch (e: Exception) {}
        writer?.close()
        reader?.close()
        connected = false
    }

    data class TerminalResult(
        val stdout: String,
        val stderr: String,
        val returnCode: Int,
        val sessionId: String = "default"
    )

    // JSON helper
    private val json = Json { ignoreUnknownKeys = true }
    
    private inline fun <reified T> json.decodeFromString(json: String): T {
        return json.decodeFromString(json)
    }
}