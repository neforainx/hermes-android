package com.example.service

import android.app.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.channels.*

class HermesDaemonService : Service() {

    private var daemonProcess: Process? = null
    private var socketThread: Thread? = null
    private var serverSocket: ServerSocket? = null
    private var unixSocket: File? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clients = mutableMapOf<Socket, PrintWriter>()
    private val SOCKET_PATH: String by lazy { "${filesDir.absolutePath}/hermes.sock" }
    private val DAEMON_PORT = 5175

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        scope.launch { bootstrapAndStart() }
    }

    private suspend fun bootstrapAndStart() = withContext(Dispatchers.IO) {
        bootstrapTermux()
        startHermesDaemon()
        startSocketServers()
    }

    private fun bootstrapTermux() {
        val rootfsDir = File(filesDir, "termux-rootfs")
        if (!rootfsDir.exists()) {
            Log.i("HERMES", "Extracting Termux rootfs...")
            extractTermuxRootfs(rootfsDir)
        }
        setupProotEnv(rootfsDir)
    }

    private fun extractTermuxRootfs(targetDir: File) {
        targetDir.mkdirs()
        val archiveFile = File(targetDir, "rootfs.tar.gz")
        
        // Copy from assets
        assets.open("termux-rootfs.tar.gz").use { input ->
            FileOutputStream(archiveFile).use { output ->
                input.copyTo(output)
            }
        }
        
        // Extract tar.gz
        try {
            Runtime.getRuntime().exec("tar -xzf ${archiveFile.absolutePath} -C $targetDir").waitFor()
            archiveFile.delete()
            Log.i("HERMES", "Termux rootfs extracted to $targetDir")
        } catch (e: Exception) {
            Log.e("HERMES", "Failed to extract rootfs", e)
        }
    }

    private fun setupProotEnv(rootfsDir: File) {
        val proot = File(rootfsDir, "bin/proot")
        if (!proot.exists()) {
            downloadProot(proot)
        }
        proot.setExecutable(true)
        
        // Create necessary directories
        File(rootfsDir, "home").mkdirs()
        File(rootfsDir, "tmp").mkdirs()
        File(rootfsDir, "var/log").mkdirs()
    }

    private fun downloadProot(target: File) {
        try {
            val url = URL("https://github.com/termux/proot/releases/download/v5.4.0/proot-aarch64")
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            
            FileOutputStream(target).use { output ->
                inputStreamReader(connection.getInputStream()).use { input ->
                    input.copyTo(output)
                }
            }
            target.setExecutable(true)
            Log.i("HERMES", "Downloaded proot to ${target.absolutePath}")
        } catch (e: Exception) {
            Log.e("HERMES", "Failed to download proot", e)
        }
    }

    private fun startHermesDaemon() {
        val pythonScript = File(filesDir, "hermes-daemon.py")
        if (!pythonScript.exists()) {
            copyAssetToFile("hermes-daemon.py", pythonScript)
        }

        val rootfsDir = File(filesDir, "termux-rootfs")
        val proot = File(rootfsDir, "bin/proot")
        
        val pb = ProcessBuilder(
            proot.absolutePath,
            "--link2symlink",
            "-0",
            "-r", rootfsDir.absolutePath,
            "-b", "/dev:/dev",
            "-b", "/proc:/proc",
            "-b", "/sys:/sys",
            "-b", "${filesDir}:/home",
            "-w", "/home",
            "python3", "hermes-daemon.py"
        )
        
        pb.directory(rootfsDir)
        pb.redirectErrorStream(true)
        
        try {
            daemonProcess = pb.start()
            Log.i("HERMES", "Hermes daemon started with PID: ${daemonProcess!!.pid()}")
            
            // Monitor daemon output
            scope.launch {
                daemonProcess!!.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        Log.d("HERMES_DAEMON", line)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HERMES", "Failed to start Hermes daemon", e)
        }
    }

    private fun startSocketServers() {
        // Unix Domain Socket
        scope.launch {
            startUnixSocketServer()
        }
        
        // TCP Socket
        scope.launch {
            startTcpSocketServer()
        }
    }

    private suspend fun startUnixSocketServer() = withContext(Dispatchers.IO) {
        val socketFile = File(SOCKET_PATH)
        if (socketFile.exists()) socketFile.delete()
        
        // Use Java NIO for Unix domain socket (Android 10+)
        try {
            val serverSocket = java.nio.channels.ServerSocketChannel.open()
            serverSocket.bind(java.net.UnixDomainSocketAddress.of(SOCKET_PATH))
            socketFile.setReadable(true, false)
            socketFile.setWritable(true, false)
            socketFile.setExecutable(false, false)
            
            while (!Thread.interrupted()) {
                val clientSocket = serverSocket.accept()
                scope.launch { handleUnixClient(clientSocket) }
            }
        } catch (e: Exception) {
            Log.e("HERMES", "Unix socket server error", e)
        }
    }

    private suspend fun startTcpSocketServer() = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(DAEMON_PORT)
            serverSocket!!.setReuseAddress(true)
            
            while (!Thread.interrupted()) {
                val client = serverSocket!!.accept()
                scope.launch { handleTcpClient(client) }
            }
        } catch (e: IOException) {
            Log.e("HERMES", "TCP socket server error", e)
        }
    }

    private suspend fun handleUnixClient(channel: java.nio.channels.SocketChannel) = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(Channels.newInputStream(channel)))
        val writer = PrintWriter(Channels.newOutputStream(channel), true)
        
        reader.forEachLine { line ->
            val response = processRequest(line)
            writer.println(response)
            writer.flush()
        }
    }

    private suspend fun handleTcpClient(socket: Socket) = withContext(Dispatchers.IO) {
        BufferedReader(InputStreamReader(socket.getInputStream())).use { reader ->
            PrintWriter(socket.getOutputStream(), true).use { writer ->
                reader.forEachLine { line ->
                    val response = processRequest(line)
                    writer.println(response)
                    writer.flush()
                }
            }
        }
    }

    private fun processRequest(line: String): String {
        return try {
            val request = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<Map<String, Any>>(line)
            
            val cmd = request["cmd"] as? String ?: return "{\"error\": \"Missing cmd\"}"
            val data = request["data"] as? Map<String, Any> ?: emptyMap()
            
            when (cmd) {
                "chat" -> handleChat(data)
                "terminal" -> handleTerminal(data)
                "cron" -> handleCron(data)
                "skill" -> handleSkill(data)
                "settings" -> handleSettings(data)
                "status" -> "{\"status\": \"ok\", \"uptime\": ${System.currentTimeMillis()}}"
                else -> "{\"error\": \"Unknown command: $cmd\"}"
            }
        } catch (e: Exception) {
            "{\"error\": \"${e.message}\"}"
        }
    }

    private fun handleChat(data: Map<String, Any>): String {
        // Delegate to ChatViewModel via broadcast or callback
        return "{\"status\": \"queued\", \"message\": \"Chat delegated to UI\"}"
    }

    private fun handleTerminal(data: Map<String, Any>): String {
        val cmd = data["cmd"] as? String ?: return "{\"error\": \"Missing cmd\"}"
        val sessionId = data["session_id"] as? String ?: "default"
        
        // Execute in proot environment
        val rootfsDir = File(filesDir, "termux-rootfs")
        val proot = File(rootfsDir, "bin/proot")
        
        val pb = ProcessBuilder(
            proot.absolutePath,
            "--link2symlink", "-0",
            "-r", rootfsDir.absolutePath,
            "-b", "/dev:/dev",
            "-b", "/proc:/proc",
            "-b", "/sys:/sys",
            "-b", "${filesDir}:/home",
            "-w", "/home",
            "bash", "-c", cmd
        )
        pb.directory(File(filesDir, "termux-rootfs"))
        pb.redirectErrorStream(true)
        
        try {
            val process = pb.start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            return kotlinx.serialization.json.Json.encodeToString(mapOf(
                "stdout" to stdout,
                "stderr" to stderr,
                "returncode" to exitCode
            ))
        } catch (e: Exception) {
            return "{\"error\": \"${e.message}\"}"
        }
    }

    private fun handleCron(data: Map<String, Any>): String {
        return "{\"status\": \"cron not implemented yet\"}"
    }

    private fun handleSkill(data: Map<String, Any>): String {
        return "{\"status\": \"skill not implemented yet\"}"
    }

    private fun handleSettings(data: Map<String, Any>): String {
        return "{\"status\": \"settings not implemented yet\"}"
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        try { serverSocket?.close() } catch (e: Exception) {}
        daemonProcess?.destroy()
        super.onDestroy()
    }

    private fun copyAssetToFile(assetName: String, target: File) {
        assets.open(assetName).use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hermes Daemon",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hermes AI Agent Background Daemon"
                setShowBadge(false)
            }
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hermes AI Agent")
            .setContentText("Daemon running on port $DAEMON_PORT")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "hermes_daemon"
        const val SOCKET_PATH = "hermes.sock"
        const val DAEMON_PORT = 5175
    }
}