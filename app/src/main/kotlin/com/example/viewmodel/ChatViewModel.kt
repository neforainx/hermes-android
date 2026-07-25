package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ipc.HermesSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// Sederhana: simpan pesan chat di memori
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user", "hermes"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- ViewModel 1: ChatViewModel (Chat + AI logic only) ---
class ChatViewModel(
    application: Application,
    private val settingsViewModel: SettingsViewModel,
    private val terminalViewModel: TerminalViewModel
) : AndroidViewModel(application) {

    private val hermesClient = HermesSocketClient(application)

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
            }
        }

        // Connect to Hermes daemon
        viewModelScope.launch {
            hermesClient.connect()
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        hermesClient.disconnect()
    }

    // In-memory chat history (tanpa Room)
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

    fun updateSandboxConfig(type: String, image: String, host: String, port: String, password: String = "") {
        settingsViewModel.updateSandboxConfig(type, image, host, port, password)
    }

    fun updateGatewayConfig(tgEnabled: Boolean, dsEnabled: Boolean, hwEnabled: Boolean) {
        settingsViewModel.updateGatewayConfig(tgEnabled, dsEnabled, hwEnabled)
    }

    fun updateGatewayFields(tgToken: String, tgChatId: String, dsUrl: String, dsChanId: String, vibMs: String, ttsAcc: String) {
        settingsViewModel.updateGatewayFields(tgToken, tgChatId, dsUrl, dsChanId, vibMs, ttsAcc)
    }

    fun setTerminalFontSize(sizeSp: Int) {
        settingsViewModel.setTerminalFontSize(sizeSp)
    }

    // Expose settings as StateFlow for UI binding
    val apiProvider = settingsViewModel.apiProvider
    val geminiApiKey = settingsViewModel.geminiApiKey
    val nousApiKey = settingsViewModel.nousApiKey
    val activeModel = settingsViewModel.activeModel
    val soulMd = settingsViewModel.soulMd
    val customApiBaseUrl = settingsViewModel.customApiBaseUrl
    val customApiKey = settingsViewModel.customApiKey
    val customModel = settingsViewModel.customModel
    val sandboxType = settingsViewModel.sandboxType
    val dockerImage = settingsViewModel.dockerImage
    val sshHost = settingsViewModel.sshHost
    val sshPort = settingsViewModel.sshPort
    val sshPassword = settingsViewModel.sshPassword
    val telegramEnabled = settingsViewModel.telegramEnabled
    val discordEnabled = settingsViewModel.discordEnabled
    val termuxHardwareEnabled = settingsViewModel.termuxHardwareEnabled
    val telegramToken = settingsViewModel.telegramToken
    val telegramChatId = settingsViewModel.telegramChatId
    val discordWebhookUrl = settingsViewModel.discordWebhookUrl
    val discordChannelId = settingsViewModel.discordChannelId
    val vibrateDurationMs = settingsViewModel.vibrateDurationMs
    val ttsLanguageAccent = settingsViewModel.ttsLanguageAccent
    val terminalFontSize = settingsViewModel.terminalFontSize

    // Terminal logs stream (delegated to TerminalViewModel)
    val terminalLogs = terminalViewModel.terminalLogs

    fun logToTerminal(line: String) {
        terminalViewModel.logToTerminal(line)
    }

    fun sendMessage(inputText: String) {
        if (inputText.isBlank() || isSending.value) return

        viewModelScope.launch {
            try {
                if (inputText.trim().startsWith("/")) {
                    // Parse slash commands locally
                    val trimmed = inputText.trim()
                    val parts = trimmed.split(" ", limit = 2)
                    val cmd = parts[0].lowercase()
                    val arg = if (parts.size > 1) parts[1].trim() else ""

                    // Save user command message
                    val userMsg = ChatMessage(sender = "user", text = inputText)
                    _messages.add(userMsg)
                    logToTerminal("USER CMD: $inputText")

                    delay(400)

                    when (cmd) {
                        "/help" -> {
                            val helpText = "PERINTAH UTAMA HERMES AI:\n" +
                                    "• /help - Menampilkan daftar perintah ini\n" +
                                    "• /model <nama_model> - Memilih model AI yang aktif\n" +
                                    "• /provider <gemini|nous> - Mengganti penyedia AI (Gemini atau Nous)\n" +
                                    "• /key <api_key> - Mengatur kunci API untuk penyedia saat ini\n" +
                                    "• /clear - Menghapus seluruh riwayat percakapan\n" +
                                    "• /file - Petunjuk cara melampirkan berkas"
                            _messages.add(ChatMessage(sender = "hermes", text = helpText))
                            logToTerminal("SYSTEM: Rendered Slash Help Catalog")
                        }
                        "/provider" -> {
                            if (arg != "gemini" && arg != "nous") {
                                _messages.add(ChatMessage(sender = "hermes", text = "ERROR: Penyedia harus berupa 'gemini' atau 'nous'."))
                            } else {
                                val defaultModel = if (arg == "gemini") "gemini-3.5-flash" else "nousresearch/hermes-3-llama-3.1-8b"
                                updateSettings(arg, settingsViewModel.geminiApiKey.value, settingsViewModel.nousApiKey.value, defaultModel)
                                _messages.add(ChatMessage(sender = "hermes", text = "SISTEM: Mengganti penyedia AI ke: ${arg.uppercase()}"))
                            }
                        }
                        "/model" -> {
                            if (arg.isBlank()) {
                                _messages.add(ChatMessage(sender = "hermes", text = "ERROR: Harap tentukan nama model. Saat ini: ${activeModel.value}"))
                            } else {
                                updateSettings(settingsViewModel.apiProvider.value, settingsViewModel.geminiApiKey.value, settingsViewModel.nousApiKey.value, arg)
                                _messages.add(ChatMessage(sender = "hermes", text = "SISTEM: Mengaktifkan model AI baru: '$arg'"))
                            }
                        }
                        "/key" -> {
                            if (arg.isBlank()) {
                                _messages.add(ChatMessage(sender = "hermes", text = "ERROR: Key cannot be empty."))
                            } else {
                                if (settingsViewModel.apiProvider.value == "gemini") {
                                    updateSettings("gemini", arg, settingsViewModel.nousApiKey.value, settingsViewModel.activeModel.value)
                                } else {
                                    updateSettings("nous", settingsViewModel.geminiApiKey.value, arg, settingsViewModel.activeModel.value)
                                }
                                _messages.add(ChatMessage(sender = "hermes", text = "SYSTEM: Updated API credentials securely for current provider."))
                            }
                        }
                        "/clear" -> {
                            _messages.clear()
                            logToTerminal("SYSTEM: Purged conversation history.")
                        }
                        "/clear-sessions" -> {
                            terminalViewModel.clearAllTerminalSessions()
                            _messages.add(ChatMessage(sender = "hermes", text = "SISTEM: Reset all terminal sessions and purged logs successfully."))
                            logToTerminal("SYSTEM: Reset all terminal sessions.")
                        }
                        "/file" -> {
                            _messages.add(ChatMessage(sender = "hermes", text = "SYSTEM: Click the paperclip attachment icon on the chat bar to select and attach workspace files."))
                        }
                        else -> {
                            _messages.add(ChatMessage(sender = "hermes", text = "ERROR: Command '$cmd' not recognized. Type /help for options."))
                        }
                    }
                    return@launch
                }

                isSending.value = true

                // 1. Save user message
                val userMsg = ChatMessage(sender = "user", text = inputText)
                _messages.add(userMsg)
                logToTerminal("USER: $inputText")

                // 2. Prepare Prompt and Run API call based on Provider
                val responseText = withContext(Dispatchers.IO) {
                    try {
                        val provider = settingsViewModel.apiProvider.value
                        val model = if (provider == "custom") settingsViewModel.customModel.value else settingsViewModel.activeModel.value
                        val instructionText = settingsViewModel.soulMd.value

                        if (provider == "gemini") {
                            var key = settingsViewModel.geminiApiKey.value.trim()
                            if (key.isEmpty()) {
                                key = BuildConfig.GEMINI_API_KEY
                            }
                            if (key.isEmpty()) {
                                "Hello! I am Hermes, your Personal AI Agent. To activate my actual Gemini intelligence, please configure your `GEMINI_API_KEY` securely in AI Studio's Secrets panel or settings."
                            } else {
                                try {
                                    val history = _messages.filter { it.sender != "tool" }
                                    val contentsPayload = history.map {
                                        Content(
                                            parts = listOf(Part(text = it.text)),
                                            role = if (it.sender == "user") "user" else "model"
                                        )
                                    }
                                    val request = GenerateContentRequest(
                                        contents = contentsPayload,
                                        systemInstruction = Content(
                                            parts = listOf(Part(text = instructionText))
                                        )
                                    )
                                    val response = RetrofitClient.service.generateContent(
                                        model = model,
                                        apiKey = key,
                                        request = request
                                    )
                                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                        ?: "No response from Gemini API."
                                } catch (e: Exception) {
                                    logToTerminal("ERROR: Gemini API failed: ${e.message}")
                                    "Failed to connect to Gemini API. Please check your API key and network connection."
                                }
                            }
                        } else if (provider == "nous") {
                            val key = settingsViewModel.nousApiKey.value.trim()
                            if (key.isEmpty()) {
                                "Hello! I am Hermes, your Personal AI Agent. To connect using Nous Research (OpenRouter), please specify your OpenRouter API Key in the settings."
                            } else {
                                try {
                                    val history = _messages.filter { it.sender != "tool" }
                                    val messagesPayload = mutableListOf<OpenRouterMessage>()

                                    // Add system prompt first
                                    messagesPayload.add(
                                        OpenRouterMessage(
                                            role = "system",
                                            content = instructionText
                                        )
                                    )

                                    // Add history
                                    messagesPayload.addAll(
                                        history.map {
                                            OpenRouterMessage(
                                                role = if (it.sender == "user") "user" else "assistant",
                                                content = it.text
                                            )
                                        }
                                    )

                                    val request = OpenRouterRequest(
                                        model = model,
                                        messages = messagesPayload
                                    )

                                    val response = OpenRouterClient.service.generateContent(
                                        authorization = "Bearer $key",
                                        request = request
                                    )

                                    response.choices?.firstOrNull()?.message?.content
                                        ?: response.error?.message
                                        ?: "No response from OpenRouter API."
                                } catch (e: Exception) {
                                    logToTerminal("ERROR: OpenRouter API failed: ${e.message}")
                                    "Failed to connect to OpenRouter API. Please check your API key and network connection."
                                }
                            }
                        } else if (provider == "custom") {
                            val key = settingsViewModel.customApiKey.value.trim()
                            val baseUrl = settingsViewModel.customApiBaseUrl.value.trim()
                            if (baseUrl.isEmpty() || key.isEmpty()) {
                                "Hello! I am Hermes, your Personal AI Agent. To connect using your Custom Provider, please specify both the Base URL and API Key in your Profile settings."
                            } else {
                                try {
                                    val history = _messages.filter { it.sender != "tool" }
                                    val messagesPayload = mutableListOf<OpenRouterMessage>()

                                    // Add system prompt first
                                    messagesPayload.add(
                                        OpenRouterMessage(
                                            role = "system",
                                            content = instructionText
                                        )
                                    )

                                    // Add history
                                    messagesPayload.addAll(
                                        history.map {
                                            OpenRouterMessage(
                                                role = if (it.sender == "user") "user" else "assistant",
                                                content = it.text
                                            )
                                        }
                                    )

                                    val request = OpenRouterRequest(
                                        model = model,
                                        messages = messagesPayload
                                    )

                                    val service = getCustomService(baseUrl)
                                    val response = service.generateContent(
                                        authorization = "Bearer $key",
                                        request = request
                                    )

                                    response.choices?.firstOrNull()?.message?.content
                                        ?: response.error?.message
                                        ?: "No response from Custom Provider API."
                                } catch (e: Exception) {
                                    logToTerminal("ERROR: Custom Provider API failed: ${e.message}")
                                    "Failed to connect to Custom Provider API. Please check your API key and base URL."
                                }
                            }
                        } else {
                            "Unknown AI Provider: $provider"
                        }
                    } catch (e: Exception) {
                        logToTerminal("ERROR: ${e.message ?: e.javaClass.simpleName}")
                        "Failed to get response. Please try again."
                    }
                }

                // 3. Save Model response
                _messages.add(ChatMessage(sender = "hermes", text = responseText))
                logToTerminal("HERMES: ${responseText.take(60)}...")

                isSending.value = false
            } catch (e: Exception) {
                logToTerminal("ERROR: ${e.message ?: e.javaClass.simpleName}")
                isSending.value = false
            }
        }
    }

    fun clearHistory() {
        _messages.clear()
        logToTerminal("SYSTEM: Chat history cleared.")
    }

    fun getCustomService(baseUrl: String): OpenRouterApiService {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(RetrofitClient.okHttpClient) // Use shared client with cert pinning
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(OpenRouterApiService::class.java)
    }
}
