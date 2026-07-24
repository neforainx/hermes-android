package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import com.example.database.CronJobEntity
import com.example.database.MessageEntity
import com.example.database.SkillEntity
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.CronViewModel
import com.example.viewmodel.PetViewModel
import com.example.viewmodel.SkillViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.TerminalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Cyberpunk / Indigo-Cream Theme Colors ---
val BackgroundDark = Color(0xFF070B19) // Pitch Indigo
val SurfaceCard = Color(0xFF0F1A35)     // Layered Indigo
val DarkAccent = Color(0xFF1A2A54)     // Deep Blue highlight
val GoldCream = Color(0xFFFFE6CB)      // Main gold text accent
val MutedGold = Color(0xFFCCB39B)       // Sub-caption gold
val ConsoleBlack = Color(0xFF01040A)    // Code view background
val SystemGreen = Color(0xFF4CAF50)     // Terminal prompt green
val AlertOrange = Color(0xFFFF9800)     // Warning orange

@Composable
fun HermesLogoCompose(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(DarkAccent),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_hermes_logo),
            contentDescription = "Hermes Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HermesAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun HermesAppTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = GoldCream,
        secondary = DarkAccent,
        background = BackgroundDark,
        surface = SurfaceCard,
        onPrimary = BackgroundDark,
        onBackground = GoldCream,
        onSurface = GoldCream
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            )
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val chatViewModel: ChatViewModel = viewModel()
    val cronViewModel: CronViewModel = viewModel()
    val skillViewModel: SkillViewModel = viewModel()
    val petViewModel: PetViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val terminalViewModel: TerminalViewModel = viewModel()

    var currentTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HermesLogoCompose(
                            modifier = Modifier.size(24.dp).padding(end = 6.dp)
                        )
                        Text(
                            text = "HERMES",
                            fontWeight = FontWeight.Bold,
                            color = GoldCream,
                            letterSpacing = 2.sp,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = GoldCream
                )
            )
        },
        bottomBar = {
            val isKeyboardOpen = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
            if (!isKeyboardOpen) {
                NavigationBar(
                    containerColor = BackgroundDark,
                    contentColor = GoldCream
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Face, contentDescription = "Agent Chat") },
                        label = { Text("Agent", fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = GoldCream,
                            unselectedIconColor = MutedGold,
                            unselectedTextColor = MutedGold,
                            indicatorColor = GoldCream
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Text(
                                text = ">_",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (currentTab == 1) BackgroundDark else MutedGold
                            )
                        },
                        label = { Text("Terminal", fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = GoldCream,
                            unselectedIconColor = MutedGold,
                            unselectedTextColor = MutedGold,
                            indicatorColor = GoldCream
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Setup") },
                        label = { Text("Setup", fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = GoldCream,
                            unselectedIconColor = MutedGold,
                            unselectedTextColor = MutedGold,
                            indicatorColor = GoldCream
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = GoldCream,
                            unselectedIconColor = MutedGold,
                            unselectedTextColor = MutedGold,
                            indicatorColor = GoldCream
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(BackgroundDark)
        ) {
            when (currentTab) {
                0 -> AgentChatScreen(chatViewModel, settingsViewModel)
                1 -> TuiTerminalScreen(chatViewModel, terminalViewModel, petViewModel, cronViewModel, skillViewModel)
                2 -> DaemonCommandCenterScreen(chatViewModel, cronViewModel, skillViewModel, settingsViewModel)
                3 -> IntegrationsScreen(chatViewModel, cronViewModel, skillViewModel, settingsViewModel)
            }
        }
    }
}

// --- TAB 1: AGENT CHAT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(viewModel: ChatViewModel, settingsViewModel: SettingsViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val currentProvider by settingsViewModel.apiProvider.collectAsState()
    val savedModel by settingsViewModel.activeModel.collectAsState()
    val savedGeminiKey by settingsViewModel.geminiApiKey.collectAsState()
    val savedNousKey by settingsViewModel.nousApiKey.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Dialog & Dropdown local state
    var showEngineDialog by remember { mutableStateOf(false) }
    var showFileDialog by remember { mutableStateOf(false) }
    var providerDropdownExpanded by remember { mutableStateOf(false) }

    // Temp state for editing settings inside dialog
    var selectedProvider by remember(currentProvider) { mutableStateOf(currentProvider) }
    var tempGeminiKey by remember(savedGeminiKey) { mutableStateOf(savedGeminiKey) }
    var tempNousKey by remember(savedNousKey) { mutableStateOf(savedNousKey) }
    var tempModel by remember(savedModel) { mutableStateOf(savedModel) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Real and template files for workspace injection
    val homeDir = java.io.File(context.filesDir, "home").apply { if (!exists()) mkdirs() }
    val realFiles: List<Pair<String, String>> = try {
        homeDir.walkTopDown().filter { it.isFile }.take(10).map { file ->
            val relative = "~/" + file.relativeTo(homeDir).path
            Pair(relative, file.readText())
        }.toList()
    } catch (e: Exception) {
        emptyList()
    }

    val workspaceTemplates: List<Pair<String, String>> = realFiles + listOf(
        Pair("~/.hermes/config.yaml", """# Hermes Linux Daemon Config
            version: 2.4.9
            provider: $currentProvider
            active_model: $savedModel
            cache_prompt: true
            telemetry: false
            log_level: DEBUG
        """.trimIndent()),
        Pair("~/AGENTS.md", """# Hermes Agent System Rules
            1. Per-conversation prompt caching is sacred.
            2. The core is a narrow waist; capability lives at the edges.
            3. Maintain perfect message role alternation.
        """.trimIndent()),
        Pair("~/.hermes/logs/agent.log", """[2026-07-19 21:10:45] INFO: run_agent.py initializing
            [2026-07-19 21:10:46] INFO: Loaded 4 AI skills (Keahlian AI)
            [2026-07-19 21:10:47] INFO: Prompt Cache: Sacred Cache HIT (98.4%)
            [2026-07-19 21:10:48] DEBUG: Received request from gateway-discord
        """.trimIndent()),
        Pair("~/.hermes/plugins/telegram_adapter.py", """# Hermes Platform Gateway Adapter
            class TelegramAdapter(BasePlatform):
                def __init__(self, token):
                    self.token = token
                    self.bot = Bot(token=token)
                    
                def poll(self):
                    self.bot.start_polling()
        """.trimIndent()),
        Pair("~/.hermes/skills/weather_plugin.py", """# Hermes Skill: Weather Check Checking
            import requests
            def check_weather(city: str):
                url = f"https://api.weather.net/{city}"
                return requests.get(url).json()
        """.trimIndent())
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. ENGINE STATUS HEADER BAR WITH QUICK SELECTION DROPDOWN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .border(1.dp, GoldCream.copy(0.15f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { providerDropdownExpanded = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(SystemGreen, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HERMES: ${currentProvider.uppercase()} (${savedModel})",
                    color = GoldCream,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Quick selection",
                    tint = GoldCream,
                    modifier = Modifier.size(16.dp)
                )

                // Dropdown menu for quick engine switching
                DropdownMenu(
                    expanded = providerDropdownExpanded,
                    onDismissRequest = { providerDropdownExpanded = false },
                    modifier = Modifier.background(SurfaceCard).border(1.dp, GoldCream.copy(0.2f))
                ) {
                    DropdownMenuItem(
                        text = { Text("GEMINI AI (gemini-3.5-flash)", color = GoldCream, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        onClick = {
                            (this as MainActivity).viewModelStore.getViewModel(ChatViewModel::class.java).updateSettings("gemini", savedGeminiKey, savedNousKey, "gemini-3.5-flash")
                            providerDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("NOUS (hermes-3-llama-3.1-8b)", color = GoldCream, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        onClick = {
                            (this as MainActivity).viewModelStore.getViewModel(ChatViewModel::class.java).updateSettings("nous", savedGeminiKey, savedNousKey, "nousresearch/hermes-3-llama-3.1-8b")
                            providerDropdownExpanded = false
                        }
                    )
                    Divider(color = GoldCream.copy(0.15f))
                    DropdownMenuItem(
                        text = { Text("⚙ OPEN FULL CONFIG...", color = GoldCream, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            selectedProvider = currentProvider
                            tempGeminiKey = savedGeminiKey
                            tempNousKey = savedNousKey
                            tempModel = savedModel
                            showEngineDialog = true
                            providerDropdownExpanded = false
                        }
                    )
                }
            }

            // Right side shortcut buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showFileDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Insert File",
                        tint = GoldCream,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        selectedProvider = currentProvider
                        tempGeminiKey = savedGeminiKey
                        tempNousKey = savedNousKey
                        tempModel = savedModel
                        showEngineDialog = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Dialog",
                        tint = GoldCream,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 2. CHAT SCROLLER LIST
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(messages.filter { it.sender != "tool" }) { msg ->
                MessageRow(msg)
            }

            if (isSending) {
                item {
                    AgentThinkingPlaceholder()
                }
            }
        }

        // slash command auto-hint bar
        if (textInput.startsWith("/")) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsoleBlack)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.dp, GoldCream.copy(0.1f)),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HINTS: /help  /model <name>  /provider <id>  /key <val>  /clear",
                    color = MutedGold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }

        // 3. INPUT BAR WITH ATTACHMENT ACTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showFileDialog = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = DarkAccent),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach System File",
                    tint = GoldCream
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Hermes or type /help...", color = MutedGold.copy(0.6f), fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldCream,
                    unfocusedBorderColor = GoldCream.copy(0.3f),
                    focusedTextColor = GoldCream,
                    unfocusedTextColor = GoldCream
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        (this as MainActivity).viewModelStore.getViewModel(ChatViewModel::class.java).sendMessage(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                },
                containerColor = GoldCream,
                contentColor = BackgroundDark,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
            }
        }
    }

    // --- ENGINE DIALOGUE PANEL ---
    if (showEngineDialog) {
        AlertDialog(
            onDismissRequest = { showEngineDialog = false },
            title = {
                Text(
                    "HERMES ENGINE SETUP",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GoldCream,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Toggle backend dynamically or enter a custom API key.",
                        color = MutedGold,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = tempNousKey,
                        onValueChange = { tempNousKey = it },
                        label = { Text("OpenRouter API Key", color = MutedGold, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GoldCream, unfocusedTextColor = GoldCream)
                    )
                    OutlinedTextField(
                        value = tempModel,
                        onValueChange = { tempModel = it },
                        label = { Text("Model Path", color = MutedGold, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GoldCream, unfocusedTextColor = GoldCream)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        (this as MainActivity).viewModelStore.getViewModel(ChatViewModel::class.java).updateSettings("nous", "", tempNousKey, tempModel)
                        showEngineDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemGreen)
                ) {
                    Text("SAVE CONFIG", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEngineDialog = false }) {
                    Text("CANCEL", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            containerColor = SurfaceCard,
            tonalElevation = 8.dp
        )
    }

    // --- WORKSPACE FILE INSERTION DIALOGUE PANEL ---
    if (showFileDialog) {
        AlertDialog(
            onDismissRequest = { showFileDialog = false },
            title = {
                Text(
                    "ATTACH SYSTEM FILE TO CONVERSATION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GoldCream,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Attach an existing file from your local workspace or choose a standard template to inject directly into the conversation context.",
                        color = MutedGold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .height(260.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(workspaceTemplates) { (path, content) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ConsoleBlack),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val formatted = "[Attached Workspace File: $path]\n```\n$content\n```\n"
                                        textInput = formatted
                                        showFileDialog = false
                                    }
                                    .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(6.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "File icon",
                                        tint = GoldCream,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = path,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldCream,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Inject context (${content.length} characters)",
                                            color = MutedGold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFileDialog = false }) {
                    Text("CLOSE", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            containerColor = SurfaceCard,
            tonalElevation = 8.dp
        )
    }
}

// --- TAB 2: TUI TERMINAL SCREEN ---
@Composable
fun TuiTerminalScreen(
    chatViewModel: ChatViewModel,
    terminalViewModel: TerminalViewModel,
    petViewModel: PetViewModel,
    cronViewModel: CronViewModel,
    skillViewModel: SkillViewModel
) {
    // Implementation would go here - placeholder
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Text("Terminal Screen - Implementation needed", color = GoldCream, fontFamily = FontFamily.Monospace)
    }
}

// --- TAB 3: DAEMON COMMAND CENTER ---
@Composable
fun DaemonCommandCenterScreen(
    chatViewModel: ChatViewModel,
    cronViewModel: CronViewModel,
    skillViewModel: SkillViewModel,
    settingsViewModel: SettingsViewModel
) {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Text("Daemon Command Center - Implementation needed", color = GoldCream, fontFamily = FontFamily.Monospace)
    }
}

// --- TAB 4: INTEGRATIONS ---
@Composable
fun IntegrationsScreen(
    chatViewModel: ChatViewModel,
    cronViewModel: CronViewModel,
    skillViewModel: SkillViewModel,
    settingsViewModel: SettingsViewModel
) {
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Text("Integrations Screen - Implementation needed", color = GoldCream, fontFamily = FontFamily.Monospace)
    }
}

// --- MESSAGE ROW ---
@Composable
fun MessageRow(message: MessageEntity) {
    val isUser = message.sender == "user"
    val isTool = message.sender == "tool"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isTool) {
            // Display tool execution card
            val isSuccess = message.toolCallStatus == "success"
            Card(
                colors = CardDefaults.cardColors(containerColor = ConsoleBlack),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(
                        1.dp,
                        if (isSuccess) SystemGreen.copy(0.4f) else AlertOrange.copy(0.4f),
                        RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (isSuccess) SystemGreen else AlertOrange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "TOOL CALL: ${message.toolCallName ?: "execution"}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSuccess) SystemGreen else AlertOrange
                        )
                        Text(
                            text = message.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MutedGold
                        )
                    }
                }
            }
        } else {
            // Standard User/AI bubble
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) DarkAccent else SurfaceCard
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 0.dp,
                    bottomEnd = if (isUser) 0.dp else 16.dp
                ),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .border(
                        1.dp,
                        if (isUser) GoldCream.copy(0.1f) else GoldCream.copy(0.2f),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 16.dp
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isUser) "YOU" else "HERMES",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isUser) MutedGold else GoldCream,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.text,
                        color = GoldCream,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AgentThinkingPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = SystemGreen
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "HERMES IS THINKING...",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MutedGold
        )
    }
}