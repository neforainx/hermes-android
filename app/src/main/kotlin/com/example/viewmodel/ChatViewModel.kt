package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.network.RetrofitClient
import com.example.network.OpenRouterClient
import com.example.network.OpenRouterApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val application: Application,
    private val settingsViewModel: com.example.viewmodel.SettingsViewModel,
    private val terminalViewModel: com.example.viewmodel.TerminalViewModel
) : AndroidViewModel(application) {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: StateFlow<List<ChatMessage>> = _messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSending = MutableStateFlow(false)

    fun updateSettings(provider: String, geminiKey: String, nousKey: String, model: String) {
        settingsViewModel.updateSettings(provider, geminiKey, nousKey, model)
    }

    fun updateCustomSettings(baseUrl: String, apiKey: String, model: String) {
        settingsViewModel.updateCustomSettings(baseUrl, apiKey, model)
    }

    fun updateSoulMd(content: String) {
        settingsViewModel.updateSoulMd(content)
    }

    fun updateTerminalFontSize(sizeSp: Int) {
        settingsViewModel.setTerminalFontSize(sizeSp)
    }

    val terminalLogs = terminalViewModel.terminalLogs

    fun logToTerminal(line: String) {
        terminalViewModel.logToTerminal(line)
    }

    fun speak(text: String) {
        val tts = android.speech.tts.TextToSpeech(application) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
            }
        }
        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
        tts.shutdown()
    }

    fun sendMessage(inputText: String) {
        if (inputText.isBlank() || isSending.value) return
        if (inputText.trim().startsWith("/")) {
            handleSlashCommand(inputText.trim())
            return
        }

        viewModelScope.launch {
            try {
                isSending.value = true

                val userMsg = ChatMessage(sender = "user", text = inputText)
                _messages.add(userMsg)
                logToTerminal("USER: $inputText")

                val responseText = runCatching {
                    withContext(Dispatchers.IO) { callProvider(inputText) }
                }.getOrElse { e ->
                    logToTerminal("ERROR: ${e.message ?: e::class.java.simpleName}")
                    "Failed to get response. Please try again."
                }

                _messages.add(ChatMessage(sender = "hermes", text = responseText))
                logToTerminal("HERMES: ${responseText.take(60)}...")
            } catch (e: Exception) {
                logToTerminal("ERROR: ${e.message ?: e::class.java.simpleName}")
            } finally {
                isSending.value = false
            }
        }
    }

    private fun handleSlashCommand(inputText: String) {
        val trimmed = inputText.trim()
        val parts = trimmed.split(" ", limit = 2)
        val cmd = parts.firstOrNull()?.lowercase() ?: ""
        val arg = parts.getOrNull(1)?.trim() ?: ""

        val reply = when (cmd) {
            "/help" -> buildString {
                appendLine("PERINTAH UTAMA HERMES AI:")
                appendLine("• /help - Menampilkan daftar perintah ini")
                appendLine("• /model <nama_model> - Memilih model AI yang aktif")
                appendLine("• /provider <gemini|nous> - Mengganti penyedia AI (Gemini atau Nous)")
                appendLine("• /key <api_key> - Mengatur kunci API untuk penyedia saat ini")
                appendLine("• /clear - Menghapus seluruh riwayat percakapan")
                appendLine("• /file - Petunjuk cara melampirkan berkas")
            }
            "/clear" -> {
                _messages.clear()
                logToTerminal("SYSTEM: Purged conversation history.")
                return
            }
            "/clear-sessions" -> {
                terminalViewModel.clearAllTerminalSessions()
                "SISTEM: Reset all terminal sessions and purged logs successfully."
            }
            "/provider" -> {
                if (arg !in listOf("gemini", "nous")) {
                    "ERROR: Penyedia harus berupa 'gemini' atau 'nous'."
                } else {
                    val model = if (arg == "gemini") "gemini-2.5-flash" else "nousresearch/hermes-3-llama-3.1-8b"
                    settingsViewModel.updateSettings(arg, settingsViewModel.geminiApiKey.value, settingsViewModel.nousApiKey.value, model)
                    _messages.add(ChatMessage(sender = "hermes", text = "SISTEM: Switching AI provider to: ${arg.uppercase()}"))
                    return
                }
            }
            "/model" -> {
                if (arg.isBlank()) "ERROR: Please specify a model. Current: ${settingsViewModel.activeModel.value}"
                else {
                    settingsViewModel.updateSettings(settingsViewModel.apiProvider.value, settingsViewModel.geminiApiKey.value, settingsViewModel.nousApiKey.value, arg)
                    "SISTEM: Active model updated to '$arg'"
                }
            }
            "/key" -> {
                if (arg.isBlank()) "ERROR: Key cannot be empty."
                else {
                    if (settingsViewModel.apiProvider.value == "gemini") settingsViewModel.updateSettings("gemini", arg, settingsViewModel.nousApiKey.value, settingsViewModel.activeModel.value)
                    else settingsViewModel.updateSettings("nous", settingsViewModel.geminiApiKey.value, arg, settingsViewModel.activeModel.value)
                    "SYSTEM: Updated API credentials for current provider."
                }
            }
            "/file" -> "SYSTEM: Click the paperclip attachment icon on the chat bar to select and attach workspace files."
            else -> "ERROR: Command '$cmd' not recognized. Type /help for options."
        }

        if (reply.isNotBlank()) {
            _messages.add(ChatMessage(sender = "hermes", text = reply))
            logToTerminal("SYSTEM: $reply")
        }
    }

    private suspend fun callProvider(inputText: String): String {
        val provider = settingsViewModel.apiProvider.value
        val model = if (provider == "custom") settingsViewModel.customModel.value else settingsViewModel.activeModel.value
        val systemInstruction = settingsViewModel.soulMd.value

        return when (provider) {
            "gemini" -> {
                val key = settingsViewModel.geminiApiKey.value.trim().ifEmpty { com.example.BuildConfig.GEMINI_API_KEY }
                if (key.isBlank()) {
                    "Hello! I am Hermes, your Personal AI Agent. To activate my actual Gemini intelligence, please configure your `GEMINI_API_KEY` securely in AI Studio's Secrets panel or settings."
                } else {
                    val history = _messages.filter { it.sender != "tool" }.takeLast(20)
                    val contentsPayload = history.map {
                        com.example.network.Content(
                            parts = listOf(com.example.network.Part(text = it.text)),
                            role = if (it.sender == "user") "user" else "model"
                        )
                    }
                    val request = com.example.network.GenerateContentRequest(
                        contents = contentsPayload,
                        systemInstruction = com.example.network.Content(parts = listOf(com.example.network.Part(text = systemInstruction)))
                    )
                    val response = RetrofitClient.service.generateContent(model = model, apiKey = key, request = request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No response from Gemini API."
                }
            }
            "nous" -> {
                val key = settingsViewModel.nousApiKey.value.trim()
                if (key.isBlank()) {
                    "Hello! I am Hermes, your Personal AI Agent. To connect using Nous Research (OpenRouter), please specify your OpenRouter API Key in the settings."
                } else {
                    val messagesPayload = mutableListOf<com.example.network.OpenRouterMessage>().apply {
                        add(com.example.network.OpenRouterMessage(role = "system", content = systemInstruction))
                        addAll(_messages.filter { it.sender != "tool" }.takeLast(20).map {
                            com.example.network.OpenRouterMessage(
                                role = if (it.sender == "user") "user" else "assistant",
                                content = it.text
                            )
                        })
                    }
                    val request = com.example.network.OpenRouterRequest(model = model, messages = messagesPayload)
                    val response = OpenRouterClient.service.generateContent(authorization = "Bearer $key", request = request)
                    response.choices?.firstOrNull()?.message?.content
                        ?: response.error?.message
                        ?: "No response from OpenRouter API."
                }
            }
            "custom" -> {
                val key = settingsViewModel.customApiKey.value.trim()
                val baseUrl = settingsViewModel.customApiBaseUrl.value.trim()
                if (baseUrl.isBlank() || key.isBlank()) {
                    "Hello! I am Hermes, your Personal AI Agent. To connect using your Custom Provider, please specify both the Base URL and API Key in your Profile settings."
                } else {
                    val messagesPayload = mutableListOf<com.example.network.OpenRouterMessage>().apply {
                        add(com.example.network.OpenRouterMessage(role = "system", content = systemInstruction))
                        addAll(_messages.filter { it.sender != "tool" }.takeLast(20).map {
                            com.example.network.OpenRouterMessage(
                                role = if (it.sender == "user") "user" else "assistant",
                                content = it.text
                            )
                        })
                    }
                    val request = com.example.network.OpenRouterRequest(model = model, messages = messagesPayload)
                    val service = buildCustomService(baseUrl)
                    val response = service.generateContent(authorization = "Bearer $key", request = request)
                    response.choices?.firstOrNull()?.message?.content
                        ?: response.error?.message
                        ?: "No response from Custom Provider API."
                }
            }
            else -> "Unknown AI Provider: $provider"
        }
    }

    private fun buildCustomService(baseUrl: String): OpenRouterApiService {
        val finalBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return retrofit2.Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(com.example.network.RetrofitClient.okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenRouterApiService::class.java)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val settingsViewModel = ViewModelProvider(
                    app,
                    ViewModelProvider.Factory
                )[com.example.viewmodel.SettingsViewModel::class.java]
                val terminalViewModel = ViewModelProvider(
                    app,
                    ViewModelProvider.Factory
                )[com.example.viewmodel.TerminalViewModel::class.java]
                ChatViewModel(app, settingsViewModel, terminalViewModel)
            }
        }
    }
}
