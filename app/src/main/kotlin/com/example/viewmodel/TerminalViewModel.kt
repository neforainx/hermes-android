package com.example.viewmodel

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.example.ipc.HermesSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// --- ViewModel 6: TerminalViewModel ---
class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val hermesClient = HermesSocketClient(application)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Terminal sessions
    data class TerminalSession(
        val id: String,
        var name: String,
        val createdAt: Long = System.currentTimeMillis(),
        var currentDir: String = "/home",
        val logs: MutableStateFlow<List<String>> = MutableStateFlow(listOf(
            "HERMES BOOT SYSTEM v2.4.9 - INITIALIZED",
            "CONNECTING TO GATEWAY ADAPTER...",
            "SUCCESS: CONNECTED TO LOCALHOST:5175",
            "CORE LOADED. STANDBY FOR AGENT DISPATCH."
        ))
    )

    val terminalSessions = MutableStateFlow<List<TerminalSession>>(
        listOf(TerminalSession("1", "main"))
    )
    val activeSessionId = MutableStateFlow<String>("1")

    // Terminal console logs stream for the TUI Terminal screen
    val terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "HERMES BOOT SYSTEM v2.4.9 - INITIALIZED",
            "CONNECTING TO GATEWAY ADAPTER...",
            "SUCCESS: CONNECTED TO LOCALHOST:5175",
            "CORE LOADED. STANDBY FOR AGENT DISPATCH."
        )
    )

    // Sanitize sensitive data before logging to terminal
    private fun sanitizeForLog(line: String): String {
        return line
            .replace(Regex("(?i)(api[_-]?key|token|secret|password|authorization)[\"':\\s]+\\S+"), "$1=***REDACTED***")
            .replace(Regex("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b"), "****-****-****-****")
            .replace(Regex("\\b\\d{10,}\\b"), "**********")
    }

    fun logToTerminal(line: String) {
        val sanitized = sanitizeForLog(line)
        logToActiveTerminal(sanitized)
        val current = terminalLogs.value.toMutableList()
        current.add("[${System.currentTimeMillis() % 100000}] $sanitized")
        if (current.size > 100) current.removeAt(0)
        terminalLogs.value = current
    }

    private fun logToActiveTerminal(line: String) {
        val activeSession = terminalSessions.value.firstOrNull { it.id == activeSessionId.value }
        if (activeSession != null) {
            val currentLogs = activeSession.logs.value.toMutableList()
            currentLogs.add("[${System.currentTimeMillis() % 100000}] $line")
            if (currentLogs.size > 500) currentLogs.removeAt(0)
            activeSession.logs.value = currentLogs
        }
    }

    fun addNewTerminalSession() {
        val newId = (terminalSessions.value.size + 1).toString()
        val newSession = TerminalSession(newId, "session $newId")
        terminalSessions.value = terminalSessions.value + newSession
        activeSessionId.value = newId
        logToTerminal("SYSTEM: Created new terminal session: $newId")
    }

    fun removeTerminalSession(sessionId: String) {
        if (terminalSessions.value.size > 1) {
            terminalSessions.value = terminalSessions.value.filter { it.id != sessionId }
            if (activeSessionId.value == sessionId) {
                activeSessionId.value = terminalSessions.value.first().id
            }
            logToTerminal("SYSTEM: Removed terminal session: $sessionId")
        }
    }

    fun clearAllTerminalSessions() {
        val firstSession = TerminalSession("1", "main")
        terminalSessions.value = listOf(firstSession)
        activeSessionId.value = "1"
        terminalLogs.value = listOf(
            "HERMES BOOT SYSTEM v2.4.9 - INITIALIZED",
            "CONNECTING TO GATEWAY ADAPTER...",
            "SUCCESS: CONNECTED TO LOCALHOST:5175",
            "CORE LOADED. STANDBY FOR AGENT DISPATCH."
        )
        logToTerminal("SYSTEM: All terminal sessions cleared.")
    }

    fun setActiveTerminalDir(dir: String) {
        val session = terminalSessions.value.firstOrNull { it.id == activeSessionId.value }
        session?.currentDir = dir
    }

    fun increaseTerminalFontSize() {
        // SettingsViewModel handles this
    }

    fun decreaseTerminalFontSize() {
        // SettingsViewModel handles this
    }

    fun clearAllTerminalLogs() {
        val current = terminalLogs.value.toMutableList()
        current.clear()
        current.add("HERMES BOOT SYSTEM v2.4.9 - LOGS CLEARED")
        terminalLogs.value = current
    }

    // Process CLI commands
    fun processCliCommand(inputText: String) {
        if (inputText.isBlank()) return

        val parts = inputText.trim().split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val arg = if (parts.size > 1) parts[1].trim() else ""

        // Save user command message to DB
        val userMsg = MessageEntity(sender = "user", text = inputText)
        DatabaseProvider.getDatabase(getApplication()).messageDao.insertMessage(userMsg)
        logToTerminal("USER CMD: $inputText")

        scope.launch {
            when (cmd) {
                "/help" -> {
                    val helpText = "PERINTAH TERMINAL HERMES:\n" +
                            "• /help - Menampilkan daftar perintah ini\n" +
                            "• /new-session - Membuat sesi terminal baru\n" +
                            "• /switch <id> - Beralih ke sesi terminal\n" +
                            "• /list-sessions - Daftar semua sesi\n" +
                            "• /clear-logs - Hapus log terminal\n" +
                            "• /clear-sessions - Reset semua sesi\n" +
                            "• /pwd - Direktori kerja aktif\n" +
                            "• /cd <path> - Ganti direktori\n" +
                            "• /ls - List files\n" +
                            "• /vibrate <ms> - Getar perangkat\n" +
                            "• /toast <msg> - Tampilkan toast\n" +
                            "• /battery - Cek status baterai\n" +
                            "• /wifi - Info WiFi\n" +
                            "• /home - Kembali ke home\n" +
                            "• <command> - Jalankan command bash di proot"
                    val hermesMsg = MessageEntity(sender = "hermes", text = helpText)
                    DatabaseProvider.getDatabase(getApplication()).messageDao.insertMessage(hermesMsg)
                    logToTerminal("SYSTEM: Rendered Terminal Help Catalog")
                }
                "/new-session" -> addNewTerminalSession()
                "/switch" -> {
                    if (arg.isBlank()) {
                        val sessions = terminalSessions.value.joinToString("\n") { "${it.id}: ${it.name}" }
                        val msg = MessageEntity(sender = "hermes", text = "Sesi tersedia:\n$sessions")
                        DatabaseProvider.getDatabase(getApplication()).messageDao.insertMessage(msg)
                    } else if (terminalSessions.value.any { it.id == arg }) {
                        activeSessionId.value = arg
                        logToTerminal("SYSTEM: Switched to session $arg")
                    } else {
                        logToTerminal("ERROR: Session $arg not found")
                    }
                }
                "/list-sessions" -> {
                    val sessions = terminalSessions.value.joinToString("\n") { "${it.id}: ${it.name} (${it.currentDir})" }
                    val msg = MessageEntity(sender = "hermes", text = "Sesi terminal:\n$sessions")
                    DatabaseProvider.getDatabase(getApplication()).messageDao.insertMessage(msg)
                }
                "/clear-logs" -> clearAllTerminalLogs()
                "/clear-sessions" -> clearAllTerminalSessions()
                "/pwd" -> {
                    val session = terminalSessions.value.firstOrNull { it.id == activeSessionId.value }
                    logToTerminal("PWD: ${session?.currentDir ?: "/home"}")
                }
                "/cd" -> {
                    if (arg.isBlank()) {
                        logToTerminal("ERROR: cd requires a path")
                    } else {
                        setActiveTerminalDir(arg)
                        logToTerminal("CD: $arg")
                    }
                }
                "/ls" -> {
                    // Execute ls in proot
                    scope.launch {
                        val result = hermesClient.terminal("ls -la", activeSessionId.value)
                        if (result.stdout.isNotBlank()) logToTerminal(result.stdout)
                        if (result.stderr.isNotBlank()) logToTerminal("ERR: ${result.stderr}")
                    }
                }
                "/vibrate" -> {
                    val ms = arg.toIntOrNull() ?: 500
                    vibrateDevice(ms)
                    logToTerminal("VIBRATE: ${ms}ms")
                }
                "/toast" -> {
                    if (arg.isBlank()) {
                        logToTerminal("ERROR: toast requires a message")
                    } else {
                        showToast(arg)
                        logToTerminal("TOAST: $arg")
                    }
                }
                "/battery" -> {
                    scope.launch {
                        val batteryInfo = getBatteryInfo()
                        logToTerminal("BATTERY: $batteryInfo")
                    }
                }
                "/wifi" -> {
                    scope.launch {
                        val result = hermesClient.terminal("termux-wifi-connectioninfo", activeSessionId.value)
                        if (result.stdout.isNotBlank()) logToTerminal(result.stdout)
                        if (result.stderr.isNotBlank()) logToTerminal("ERR: ${result.stderr}")
                    }
                }
                "/home" -> {
                    setActiveTerminalDir("/home")
                    logToTerminal("CD: /home")
                }
                else -> {
                    // Execute arbitrary command in proot/Termux
                    scope.launch {
                        val result = hermesClient.terminal(inputText, activeSessionId.value)
                        if (result.stdout.isNotBlank()) logToTerminal(result.stdout)
                        if (result.stderr.isNotBlank()) logToTerminal("ERR: ${result.stderr}")
                    }
                }
            }
        }
    }

    private fun vibrateDevice(ms: Int) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms.toLong())
        }
    }

    private fun showToast(message: String) {
        val context = getApplication()
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun getBatteryInfo(): String {
        val context = getApplication()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, filter)
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val technology = batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "unknown"
            val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val statusStr = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }
            val healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }

            return "Level: ${percent}% | Status: $statusStr | Health: $healthStr | Tech: $technology | Temp: ${temperature / 10.0}°C | Voltage: ${voltage}mV"
        }
        return "Battery info unavailable"
    }

    override fun onCleared() {
        super.onCleared()
        hermesClient.disconnect()
        scope.cancel()
    }
}