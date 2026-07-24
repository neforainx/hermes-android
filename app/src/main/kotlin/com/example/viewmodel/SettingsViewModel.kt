package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

// --- ViewModel 5: SettingsViewModel ---
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("hermes_settings", Context.MODE_PRIVATE)

    // API Provider & Model Settings
    val apiProvider = MutableStateFlow(sharedPrefs.getString("api_provider", "nous") ?: "nous")
    val geminiApiKey = MutableStateFlow(sharedPrefs.getString("gemini_api_key", "") ?: "")
    val nousApiKey = MutableStateFlow(sharedPrefs.getString("nous_api_key", "") ?: "")
    val activeModel = MutableStateFlow(
        sharedPrefs.getString("active_model", null) ?: "nousresearch/hermes-3-llama-3.1-8b"
    )

    // Custom Provider Settings
    val customApiBaseUrl = MutableStateFlow(
        sharedPrefs.getString("custom_api_base_url", "https://api.openai.com/v1/") ?: ""
    )
    val customApiKey = MutableStateFlow(sharedPrefs.getString("custom_api_key", "") ?: "")
    val customModel = MutableStateFlow(sharedPrefs.getString("custom_model", "gpt-4o") ?: "")

    // Soul.md Personality
    val soulMd = MutableStateFlow(
        sharedPrefs.getString("soul_md", "You are Hermes, a sophisticated and friendly personal AI agent. You execute terminal commands, schedule background cron jobs, manage skills/plugins, and look after your desktop pixel pet. Speak elegantly and concisely.") ?: ""
    )

    // Sandbox / Environment Settings
    val sandboxType = MutableStateFlow(sharedPrefs.getString("sandbox_type", "Embedded Termux") ?: "Embedded Termux")
    val dockerImage = MutableStateFlow(sharedPrefs.getString("docker_image", "ubuntu:22.04") ?: "ubuntu:22.04")
    val sshHost = MutableStateFlow(sharedPrefs.getString("ssh_host", "root@127.0.0.1") ?: "root@127.0.0.1")
    val sshPort = MutableStateFlow(sharedPrefs.getString("ssh_port", "22") ?: "22")
    val sshPassword = MutableStateFlow(sharedPrefs.getString("ssh_password", "") ?: "")

    // Gateway Platforms
    val telegramEnabled = MutableStateFlow(sharedPrefs.getBoolean("telegram_enabled", true))
    val discordEnabled = MutableStateFlow(sharedPrefs.getBoolean("discord_enabled", false))
    val termuxHardwareEnabled = MutableStateFlow(sharedPrefs.getBoolean("termux_hardware_enabled", true))

    // Gateway Parameter Fields
    val telegramToken = MutableStateFlow(sharedPrefs.getString("telegram_token", "") ?: "")
    val telegramChatId = MutableStateFlow(sharedPrefs.getString("telegram_chat_id", "") ?: "")
    val discordWebhookUrl = MutableStateFlow(sharedPrefs.getString("discord_webhook_url", "") ?: "")
    val discordChannelId = MutableStateFlow(sharedPrefs.getString("discord_channel_id", "") ?: "")
    val vibrateDurationMs = MutableStateFlow(sharedPrefs.getString("vibrate_duration_ms", "500") ?: "500")
    val ttsLanguageAccent = MutableStateFlow(sharedPrefs.getString("tts_language_accent", "en-US") ?: "en-US")

    // Terminal Font Size
    val terminalFontSize = MutableStateFlow(sharedPrefs.getInt("terminal_font_size", 11))

    // --- Update Functions ---

    fun updateSettings(provider: String, geminiKey: String, nousKey: String, model: String) {
        sharedPrefs.edit()
            .putString("api_provider", provider)
            .putString("gemini_api_key", geminiKey)
            .putString("nous_api_key", nousKey)
            .putString("active_model", model)
            .apply()

        apiProvider.value = provider
        geminiApiKey.value = geminiKey
        nousApiKey.value = nousKey
        activeModel.value = model

        logToTerminal("SYSTEM: Updated API config: $provider / $model")
    }

    fun updateCustomSettings(baseUrl: String, apiKey: String, model: String) {
        sharedPrefs.edit()
            .putString("custom_api_base_url", baseUrl)
            .putString("custom_api_key", apiKey)
            .putString("custom_model", model)
            .apply()

        customApiBaseUrl.value = baseUrl
        customApiKey.value = apiKey
        customModel.value = model

        logToTerminal("SYSTEM: Updated Custom Provider config: $baseUrl / $model")
    }

    fun updateSandboxConfig(type: String, image: String, host: String, port: String, password: String = "") {
        sharedPrefs.edit()
            .putString("sandbox_type", type)
            .putString("docker_image", image)
            .putString("ssh_host", host)
            .putString("ssh_port", port)
            .putString("ssh_password", password)
            .apply()

        sandboxType.value = type
        dockerImage.value = image
        sshHost.value = host
        sshPort.value = port
        sshPassword.value = password

        logToTerminal("SYSTEM: Updated Sandbox Environment to: $type")
    }

    fun updateGatewayConfig(tgEnabled: Boolean, dsEnabled: Boolean, hwEnabled: Boolean) {
        sharedPrefs.edit()
            .putBoolean("telegram_enabled", tgEnabled)
            .putBoolean("discord_enabled", dsEnabled)
            .putBoolean("termux_hardware_enabled", hwEnabled)
            .apply()

        telegramEnabled.value = tgEnabled
        discordEnabled.value = dsEnabled
        termuxHardwareEnabled.value = hwEnabled

        logToTerminal("SYSTEM: Updated Gateway Platforms Configuration")
    }

    fun updateGatewayFields(tgToken: String, tgChatId: String, dsUrl: String, dsChanId: String, vibMs: String, ttsAcc: String) {
        sharedPrefs.edit()
            .putString("telegram_token", tgToken)
            .putString("telegram_chat_id", tgChatId)
            .putString("discord_webhook_url", dsUrl)
            .putString("discord_channel_id", dsChanId)
            .putString("vibrate_duration_ms", vibMs)
            .putString("tts_language_accent", ttsAcc)
            .apply()

        telegramToken.value = tgToken
        telegramChatId.value = tgChatId
        discordWebhookUrl.value = dsUrl
        discordChannelId.value = dsChanId
        vibrateDurationMs.value = vibMs
        ttsLanguageAccent.value = ttsAcc

        logToTerminal("SYSTEM: Updated Gateway Parameter Fields")
    }

    fun updateSoulMd(content: String) {
        sharedPrefs.edit().putString("soul_md", content).apply()
        soulMd.value = content
        logToTerminal("SYSTEM: Saved SOUL.md personality matrix successfully.")
    }

    fun setTerminalFontSize(sizeSp: Int) {
        val clamped = sizeSp.coerceIn(8, 28)
        sharedPrefs.edit().putInt("terminal_font_size", clamped).apply()
        terminalFontSize.value = clamped
    }

    private fun logToTerminal(line: String) {
        // This would be called from ViewModels - actual logging delegated to TerminalViewModel
        // For now just a placeholder - actual implementation delegates to TerminalViewModel
    }
}