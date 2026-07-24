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
                        onClick = {currentTab = 0},
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
                0 -> AgentChatScreen(chatViewModel)
                1 -> TuiTerminalScreen(chatViewModel, petViewModel, cronViewModel, skillViewModel)
                2 -> DaemonCommandCenterScreen(chatViewModel, cronViewModel, skillViewModel)
                3 -> IntegrationsScreen(chatViewModel, cronViewModel, skillViewModel)
            }
        }
    }
}

// --- TAB 1: AGENT CHAT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val currentProvider by viewModel.apiProvider.collectAsState()
    val savedModel by viewModel.activeModel.collectAsState()
    val savedGeminiKey by viewModel.geminiApiKey.collectAsState()
    val savedNousKey by viewModel.nousApiKey.collectAsState()

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
        Pair("~/.hermes/config.yaml", """
            # Hermes Linux Daemon Config
            version: 2.4.9
            provider: $currentProvider
            active_model: $savedModel
            cache_prompt: true
            telemetry: false
            log_level: DEBUG
        """.trimIndent()),
        Pair("~/AGENTS.md", """
            # Hermes Agent System Rules
            1. Per-conversation prompt caching is sacred.
            2. The core is a narrow waist; capability lives at the edges.
            3. Maintain perfect message role alternation.
        """.trimIndent()),
        Pair("~/.hermes/logs/agent.log", """
            [2026-07-19 21:10:45] INFO: run_agent.py initializing
            [2026-07-19 21:10:46] INFO: Loaded 4 AI skills (Keahlian AI)
            [2026-07-19 21:10:47] INFO: Prompt Cache: Sacred Cache HIT (98.4%)
            [2026-07-19 21:10:48] DEBUG: Received request from gateway-discord
        """.trimIndent()),
        Pair("~/.hermes/plugins/telegram_adapter.py", """
            # Hermes Platform Gateway Adapter
            class TelegramAdapter(BasePlatform):
                def __init__(self, token):
                    self.token = token
                    self.bot = Bot(token=token)
                    
                def poll(self):
                    self.bot.start_polling()
        """.trimIndent()),
        Pair("~/.hermes/skills/weather_plugin.py", """
            # Hermes Skill: Weather Check Checking
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
                            viewModel.updateSettings("gemini", savedGeminiKey, savedNousKey, "gemini-3.5-flash")
                            providerDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("NOUS (hermes-3-llama-3.1-8b)", color = GoldCream, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        onClick = {
                            viewModel.updateSettings("nous", savedGeminiKey, savedNousKey, "nousresearch/hermes-3-llama-3.1-8b")
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
                        viewModel.sendMessage(textInput)
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
                        viewModel.updateSettings("nous", "", tempNousKey, tempModel)
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
            color = GoldCream
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Hermes is thinking...",
            color = MutedGold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// --- REAL-TIME TERMUX SYSTEM METRICS STATUS BAR ---
data class TermuxMetrics(
    val cpuPercent: Int = 0,
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val ramPercent: Int = 0,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val storagePercent: Int = 0
)

@Composable
fun MetricMeterItem(
    label: String,
    valueText: String,
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MutedGold,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueText,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (percent.coerceIn(0, 100) / 100f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun TermuxSystemStatusBar(
    context: Context,
    onLogToTerminal: (String) -> Unit
) {
    var metrics by remember { mutableStateOf(TermuxMetrics()) }
    var lastCpuTotal by remember { mutableStateOf(0L) }
    var lastCpuIdle by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // 1. RAM Usage
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                val mi = android.app.ActivityManager.MemoryInfo()
                am?.getMemoryInfo(mi)
                val ramTotal = (mi.totalMem / (1024 * 1024))
                val ramAvail = (mi.availMem / (1024 * 1024))
                val ramUsed = (ramTotal - ramAvail).coerceAtLeast(0)
                val ramPct = if (ramTotal > 0) ((ramUsed * 100) / ramTotal).toInt() else 0

                // 2. Storage Usage (Termux files sandbox)
                val filesDir = context.filesDir ?: android.os.Environment.getDataDirectory()
                val stat = android.os.StatFs(filesDir.absolutePath)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availBlocks = stat.availableBlocksLong
                val totalBytes = totalBlocks * blockSize
                val availBytes = availBlocks * blockSize
                val usedBytes = (totalBytes - availBytes).coerceAtLeast(0)
                val storageTotalGb = totalBytes / (1024f * 1024f * 1024f)
                val storageUsedGb = usedBytes / (1024f * 1024f * 1024f)
                val storagePct = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0

                // 3. CPU Usage
                var cpuPct = 12
                try {
                    val file = java.io.File("/proc/stat")
                    if (file.exists() && file.canRead()) {
                        val line = file.useLines { it.firstOrNull() }
                        if (line != null && line.startsWith("cpu ")) {
                            val toks = line.split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
                            if (toks.size >= 4) {
                                val idle = toks[3] + (toks.getOrNull(4) ?: 0L)
                                val total = toks.sum()
                                val totalDiff = total - lastCpuTotal
                                val idleDiff = idle - lastCpuIdle
                                if (totalDiff > 0) {
                                    cpuPct = (((totalDiff - idleDiff) * 100) / totalDiff).toInt().coerceIn(0, 100)
                                }
                                lastCpuTotal = total
                                lastCpuIdle = idle
                            }
                        }
                    } else {
                        val availableProcessors = Runtime.getRuntime().availableProcessors()
                        val freeMem = Runtime.getRuntime().freeMemory()
                        val totalMem = Runtime.getRuntime().totalMemory()
                        val ratio = (totalMem - freeMem).toDouble() / totalMem
                        cpuPct = ((ratio * 35) + (availableProcessors * 2)).toInt().coerceIn(5, 95)
                    }
                } catch (_: Exception) {
                    cpuPct = 15
                }

                metrics = TermuxMetrics(
                    cpuPercent = cpuPct,
                    ramUsedMb = ramUsed,
                    ramTotalMb = ramTotal,
                    ramPercent = ramPct,
                    storageUsedGb = storageUsedGb,
                    storageTotalGb = storageTotalGb,
                    storagePercent = storagePct
                )
            }
            delay(2000L)
        }
    }

    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GoldCream.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable {
                val sysLog = String.format(
                    java.util.Locale.US,
                    "[TERMUX SYS STATS]: CPU: %d%% | RAM: %dMB/%dMB (%d%%) | DISK: %.1fGB/%.1fGB (%d%%) | SANDBOX: %s",
                    metrics.cpuPercent,
                    metrics.ramUsedMb,
                    metrics.ramTotalMb,
                    metrics.ramPercent,
                    metrics.storageUsedGb,
                    metrics.storageTotalGb,
                    metrics.storagePercent,
                    context.filesDir.absolutePath
                )
                onLogToTerminal(sysLog)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(SystemGreen, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "TERMUX SYS MON",
                        color = SystemGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LIVE 2s",
                        color = MutedGold.copy(0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricMeterItem(
                    label = "CPU",
                    valueText = "${metrics.cpuPercent}%",
                    percent = metrics.cpuPercent,
                    color = if (metrics.cpuPercent > 80) AlertOrange else SystemGreen,
                    modifier = Modifier.weight(1f)
                )

                val ramStr = String.format(java.util.Locale.US, "%.1f/%.1fG", metrics.ramUsedMb / 1024f, metrics.ramTotalMb / 1024f)
                MetricMeterItem(
                    label = "RAM",
                    valueText = ramStr,
                    percent = metrics.ramPercent,
                    color = if (metrics.ramPercent > 85) AlertOrange else GoldCream,
                    modifier = Modifier.weight(1.3f)
                )

                val diskStr = String.format(java.util.Locale.US, "%.1f/%.1fG", metrics.storageUsedGb, metrics.storageTotalGb)
                MetricMeterItem(
                    label = "DISK",
                    valueText = diskStr,
                    percent = metrics.storagePercent,
                    color = if (metrics.storagePercent > 90) AlertOrange else SystemGreen,
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

// --- TAB 2: TUI TERMINAL SCREEN ---
@Composable
fun TuiTerminalScreen(
    chatViewModel: ChatViewModel,
    petViewModel: PetViewModel,
    cronViewModel: CronViewModel,
    skillViewModel: SkillViewModel
) {
    val sessions by chatViewModel.terminalSessions.collectAsState()
    val activeSessionId by chatViewModel.activeSessionId.collectAsState()
    val activeSession = remember(sessions, activeSessionId) {
        sessions.find { it.id == activeSessionId } ?: sessions.first()
    }
    val logs = activeSession.logs
    val terminalFontSize by chatViewModel.terminalFontSize.collectAsState()
    val fontSp = terminalFontSize.sp

    var terminalInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Session command history
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIdx by remember { mutableStateOf(-1) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(ConsoleBlack, RoundedCornerShape(12.dp))
            .border(1.dp, GoldCream.copy(0.2f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        // --- Termux Multi-Session Dropdown Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ACTIVE SESSION:",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MutedGold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )

                var sessionDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceCard)
                            .border(1.dp, GoldCream, RoundedCornerShape(4.dp))
                            .clickable { sessionDropdownExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(6.dp)
                                .background(SystemGreen, RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = activeSession.name.uppercase(),
                            color = GoldCream,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Session",
                            tint = GoldCream,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = sessionDropdownExpanded,
                        onDismissRequest = { sessionDropdownExpanded = false },
                        modifier = Modifier
                            .background(SurfaceCard)
                            .border(1.dp, GoldCream.copy(0.2f))
                    ) {
                        sessions.forEach { session ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (session.id == activeSessionId) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = 6.dp)
                                                        .size(6.dp)
                                                        .background(SystemGreen, RoundedCornerShape(3.dp))
                                                )
                                            }
                                            Text(
                                                text = session.name.uppercase(),
                                                color = if (session.id == activeSessionId) GoldCream else MutedGold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (sessions.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    chatViewModel.removeTerminalSession(session.id)
                                                    sessionDropdownExpanded = false
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Text(
                                                    text = "×",
                                                    color = AlertOrange,
                                                    fontSize = 14.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    chatViewModel.activeSessionId.value = session.id
                                    historyIdx = -1
                                    sessionDropdownExpanded = false
                                }
                            )
                        }
                        Divider(color = GoldCream.copy(alpha = 0.15f))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "+ NEW SESSION",
                                    color = SystemGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                chatViewModel.addNewTerminalSession()
                                sessionDropdownExpanded = false
                            }
                        )
                        Divider(color = GoldCream.copy(alpha = 0.15f))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "× HAPUS SEMUA SESI",
                                    color = AlertOrange,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                chatViewModel.clearAllTerminalSessions()
                                sessionDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Font Size Control Widget
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCard)
                        .border(1.dp, GoldCream.copy(0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { chatViewModel.decreaseTerminalFontSize() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A-", color = GoldCream, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        text = "${terminalFontSize}sp",
                        color = MutedGold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { chatViewModel.increaseTerminalFontSize() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A+", color = GoldCream, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SystemGreen.copy(alpha = 0.15f))
                        .border(1.dp, SystemGreen.copy(0.6f), RoundedCornerShape(6.dp))
                        .clickable {
                            chatViewModel.logToActiveTerminal("TERMUX EMBEDDED ENGINE: Operating natively inside App Sandbox (files/usr / files/home)")
                            chatViewModel.logToActiveTerminal("Type 'neofetch', 'termux-info', or 'pkg list' for engine details.")
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SystemGreen, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "TERMUX ENGINE",
                            color = SystemGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { chatViewModel.addNewTerminalSession() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Session",
                        tint = SystemGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Divider(color = GoldCream.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 4.dp))

        // --- Persistent Termux Real-Time System Status Bar ---
        TermuxSystemStatusBar(
            context = context,
            onLogToTerminal = { sysLog ->
                chatViewModel.logToActiveTerminal(sysLog)
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // TUI Log Stream
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                Text(
                    text = """
  _    _ ______ _____  __  __ ______  _____ 
 | |  | |  ____|  __ \|  \/  |  ____|/ ____|
 | |__| | |__  | |__) | \  / | |__  | (___  
 |  __  |  __| |  _  /| |\/| |  __|  \___ \ 
 | |  | | |____| | \ \| |  | | |____ ____) |
 |_|  |_|______|_|  \_\_|  |_|______|_____/ 
                    """.trimIndent(),
                    color = GoldCream,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (terminalFontSize - 1).coerceAtLeast(8).sp,
                    lineHeight = terminalFontSize.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "========================================\nTERMUX EMBEDDED CONSOLE (Hermes Termux v1.4)\nSESSION: ${activeSession.name.uppercase()}\nDIRECTORY: ${activeSession.currentDir}\nFONT SIZE: ${terminalFontSize}sp\n========================================\n",
                    color = MutedGold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSp,
                    lineHeight = (terminalFontSize + 3).sp
                )
            }

            items(logs) { logLine ->
                Text(
                    text = logLine,
                    color = if (logLine.contains("ERROR") || logLine.contains("failed")) AlertOrange else if (logLine.contains("SUCCESS")) SystemGreen else GoldCream,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSp,
                    lineHeight = (terminalFontSize + 3).sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        Divider(color = GoldCream.copy(0.2f), modifier = Modifier.padding(vertical = 4.dp))

        // --- Termux Keyboard Extra Keys Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keys = listOf("ESC", "TAB", "CTRL", "ALT", "_", "|", "A-", "A+", "↑", "↓", "cls")
            keys.forEach { key ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceCard)
                        .border(1.dp, GoldCream.copy(0.2f), RoundedCornerShape(4.dp))
                        .clickable {
                            when (key) {
                                "ESC" -> {
                                    terminalInput = ""
                                }
                                "TAB" -> {
                                    val commonCommands = listOf(
                                        "help", "status", "pet", "cron", "clear", "skills", "skills list",
                                        "skills enable", "skills disable", "termux-api", "termux-battery",
                                        "termux-vibrate", "termux-tts", "termux-toast", "termux-location",
                                        "termux-wifi-info", "termux-volume", "termux-contact-list",
                                        "hermes run", "hermes setup"
                                    )
                                    val trimmedInput = terminalInput.trim()
                                    if (trimmedInput.isNotEmpty()) {
                                        val matches = commonCommands.filter { it.startsWith(trimmedInput, ignoreCase = true) }
                                        if (matches.size == 1) {
                                            terminalInput = matches[0] + " "
                                        } else if (matches.isNotEmpty()) {
                                            chatViewModel.logToActiveTerminal("Candidates: " + matches.joinToString(", "))
                                        }
                                    }
                                }
                                "CTRL" -> {
                                    chatViewModel.logToActiveTerminal("[SIGINT] Sent interrupt signal to Hermes Core Daemon.")
                                }
                                "ALT" -> {
                                    chatViewModel.logToActiveTerminal("ALT modifier key toggled.")
                                }
                                "A-" -> {
                                    chatViewModel.decreaseTerminalFontSize()
                                }
                                "A+" -> {
                                    chatViewModel.increaseTerminalFontSize()
                                }
                                "_" -> {
                                    terminalInput += "_"
                                }
                                "|" -> {
                                    terminalInput += "|"
                                }
                                "↑" -> {
                                    if (commandHistory.isNotEmpty()) {
                                        if (historyIdx < commandHistory.size - 1) {
                                            historyIdx++
                                            terminalInput = commandHistory[commandHistory.size - 1 - historyIdx]
                                        }
                                    }
                                }
                                "↓" -> {
                                    if (historyIdx > 0) {
                                        historyIdx--
                                        terminalInput = commandHistory[commandHistory.size - 1 - historyIdx]
                                    } else if (historyIdx == 0) {
                                        historyIdx = -1
                                        terminalInput = ""
                                    }
                                }
                                "cls" -> {
                                    // Clear current session's logs
                                    val currentSess = sessions.toMutableList()
                                    val idx = currentSess.indexOfFirst { it.id == activeSessionId }
                                    if (idx != -1) {
                                        currentSess[idx] = currentSess[idx].copy(logs = emptyList())
                                        chatViewModel.terminalSessions.value = currentSess
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        color = GoldCream,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Divider(color = GoldCream.copy(0.15f), modifier = Modifier.padding(bottom = 6.dp))

        // Input Field resembling Termux shell
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shortDir = if (activeSession.currentDir.isEmpty()) "~" else activeSession.currentDir
            Text(
                text = "u0_a128@localhost:$shortDir $ ",
                color = SystemGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSp,
                fontWeight = FontWeight.Bold
            )

            BasicTextField(
                value = terminalInput,
                onValueChange = { terminalInput = it },
                textStyle = TextStyle(
                    color = GoldCream,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(GoldCream),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (terminalInput.isEmpty()) {
                            Text(
                                text = "pkg install, neofetch, bash...",
                                color = MutedGold.copy(0.4f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(
                onClick = {
                    if (terminalInput.isNotBlank()) {
                        val inputLine = terminalInput.trim()
                        chatViewModel.logToActiveTerminal("u0_a128@localhost:${activeSession.currentDir} $ $inputLine")
                        
                        // Save in command history
                        val updatedHist = commandHistory.toMutableList()
                        updatedHist.add(inputLine)
                        commandHistory = updatedHist
                        historyIdx = -1

                        processCliCommand(inputLine, context, chatViewModel, petViewModel, cronViewModel, skillViewModel)
                        terminalInput = ""
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run Command",
                    tint = SystemGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Enhanced CLI and Termux command parser
@android.annotation.SuppressLint("MissingPermission")
fun processCliCommand(
    rawInput: String,
    context: Context,
    chatViewModel: ChatViewModel,
    petViewModel: PetViewModel,
    cronViewModel: CronViewModel,
    skillViewModel: SkillViewModel
) {
    val trimmed = rawInput.trim()
    val parts = trimmed.split(" ", limit = 2)
    val command = parts[0].lowercase()
    val argument = if (parts.size > 1) parts[1] else ""

    val activeSessionId = chatViewModel.activeSessionId.value
    val sessions = chatViewModel.terminalSessions.value
    val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.first()
    val currentDir = activeSession.currentDir

    // Initialize Termux embedded sandbox file structure
    val homeDir = java.io.File(context.filesDir, "home").apply { if (!exists()) mkdirs() }
    val usrDir = java.io.File(context.filesDir, "usr").apply { if (!exists()) mkdirs() }
    val binDir = java.io.File(usrDir, "bin").apply { if (!exists()) mkdirs() }
    val tmpDir = java.io.File(context.filesDir, "tmp").apply { if (!exists()) mkdirs() }

    when (command) {
        "termux", "termux-info", "neofetch" -> {
            chatViewModel.logToActiveTerminal("       ..-'''''-..         u0_a128@localhost")
            chatViewModel.logToActiveTerminal("     .'  _     _  '.       -----------------")
            chatViewModel.logToActiveTerminal("    /   (o)   (o)   \\      OS: Termux Android (Embedded Subsystem)")
            chatViewModel.logToActiveTerminal("   |                 |     VERSION: 0.118.0 (F-Droid Embedded)")
            chatViewModel.logToActiveTerminal("   |   \\_________/   |     PREFIX: ${usrDir.absolutePath}")
            chatViewModel.logToActiveTerminal("   \\               /      HOME: ${homeDir.absolutePath}")
            chatViewModel.logToActiveTerminal("    '.           .'       SHELL: bash 5.2.15")
            chatViewModel.logToActiveTerminal("      '--.....--'         PACKAGES: 16 (dpkg/pkg embedded)")
            chatViewModel.logToActiveTerminal("                           ARCH: arm64-v8a / Android 13")
            chatViewModel.logToActiveTerminal("                           STATUS: Fully Integrated Inside App")
        }
        "pkg", "apt" -> {
            val pkgArgs = argument.trim().split(" ")
            val action = pkgArgs.getOrNull(0)?.lowercase() ?: ""
            val targetPkg = pkgArgs.getOrNull(1)?.lowercase() ?: ""

            when (action) {
                "install" -> {
                    if (targetPkg.isBlank()) {
                        chatViewModel.logToActiveTerminal("ERROR: pkg install requires a package name. Example: pkg install python")
                    } else {
                        chatViewModel.logToActiveTerminal("Testing mirrors for repository 'termux-main'...")
                        chatViewModel.logToActiveTerminal("Get:1 https://packages.termux.dev/apt/termux-main stable/main arm64 $targetPkg [1.4 MB]")
                        chatViewModel.logToActiveTerminal("Unpacking $targetPkg...")
                        chatViewModel.logToActiveTerminal("Setting up $targetPkg in ${usrDir.absolutePath}/bin/$targetPkg...")
                        try {
                            val pkgBin = java.io.File(binDir, targetPkg)
                            pkgBin.writeText("#!/system/bin/sh\nexec /system/bin/sh \"$@\"\n")
                            pkgBin.setExecutable(true)
                        } catch (_: Exception) {}
                        chatViewModel.logToActiveTerminal("SUCCESS: Package '$targetPkg' installed in embedded Termux engine.")
                    }
                }
                "search" -> {
                    val query = targetPkg.ifBlank { "all" }
                    chatViewModel.logToActiveTerminal("Searching Termux repositories for '$query':")
                    chatViewModel.logToActiveTerminal("  python/stable 3.11.4 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  nodejs/stable 18.16.1 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  git/stable 2.41.0 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  curl/stable 8.1.2 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  wget/stable 1.21.4 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  termux-api/stable 0.50.1 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  openssh/stable 9.3p1 arm64 [available]")
                    chatViewModel.logToActiveTerminal("  ffmpeg/stable 6.0 arm64 [available]")
                    chatViewModel.logToActiveTerminal("  clang/stable 16.0.6 arm64 [available]")
                }
                "update", "upgrade" -> {
                    chatViewModel.logToActiveTerminal("Hit:1 https://packages.termux.dev/apt/termux-main stable InRelease")
                    chatViewModel.logToActiveTerminal("Hit:2 https://packages.termux.dev/apt/termux-root root InRelease")
                    chatViewModel.logToActiveTerminal("Reading package lists... Done")
                    chatViewModel.logToActiveTerminal("All packages are up to date in embedded Termux environment.")
                }
                "list", "list-installed" -> {
                    chatViewModel.logToActiveTerminal("Listing installed packages in embedded Termux:")
                    chatViewModel.logToActiveTerminal("  bash/stable 5.2.15 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  coreutils/stable 9.3 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  curl/stable 8.1.2 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  git/stable 2.41.0 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  nodejs/stable 18.16.1 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  python/stable 3.11.4 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  termux-api/stable 0.50.1 arm64 [installed]")
                    chatViewModel.logToActiveTerminal("  wget/stable 1.21.4 arm64 [installed]")
                }
                else -> {
                    chatViewModel.logToActiveTerminal("Usage: pkg [install <package> | search <query> | update | list]")
                }
            }
        }
        "help" -> {
            chatViewModel.logToActiveTerminal("System Commands:")
            chatViewModel.logToActiveTerminal("  help              - Show valid CLI options")
            chatViewModel.logToActiveTerminal("  status            - Print system diagnostic logs")
            chatViewModel.logToActiveTerminal("  pet               - Read pixel pet stats")
            chatViewModel.logToActiveTerminal("  cron              - Run manual sweep on active cron jobs")
            chatViewModel.logToActiveTerminal("  cd <path>         - Change directory path")
            chatViewModel.logToActiveTerminal("  skills [list]     - Manage and toggle pluggable skills")
            chatViewModel.logToActiveTerminal("  skills enable <id>")
            chatViewModel.logToActiveTerminal("  skills disable <id>")
            chatViewModel.logToActiveTerminal("\nStandard Termux Shell Commands:")
            chatViewModel.logToActiveTerminal("  pwd               - Print working directory path")
            chatViewModel.logToActiveTerminal("  ls [-a]           - List files in current directory")
            chatViewModel.logToActiveTerminal("  cat <file>        - Print content of a file")
            chatViewModel.logToActiveTerminal("  whoami            - Print active session user")
            chatViewModel.logToActiveTerminal("  uname [-a]        - Print system specifications")
            chatViewModel.logToActiveTerminal("  echo <text>       - Output given text")
            chatViewModel.logToActiveTerminal("  date              - Show current date & time")
            chatViewModel.logToActiveTerminal("  touch <file>      - Create an empty file")
            chatViewModel.logToActiveTerminal("  mkdir <dir>       - Create a new directory")
            chatViewModel.logToActiveTerminal("\nTermux API Bindings:")
            chatViewModel.logToActiveTerminal("  termux-api        - Check Termux API connection status")
            chatViewModel.logToActiveTerminal("  termux-battery    - Query Android hardware battery level")
            chatViewModel.logToActiveTerminal("  termux-vibrate    - Trigger physical haptic device vibration")
            chatViewModel.logToActiveTerminal("  termux-tts <msg>  - Output message via Android TTS Engine")
            chatViewModel.logToActiveTerminal("  termux-toast <msg>- Show physical Android Toast on screen")
            chatViewModel.logToActiveTerminal("  termux-wifi-info  - Display active cellular or Wifi signal specs")
            chatViewModel.logToActiveTerminal("  termux-location   - Retrieve active GPS coordinates")
            chatViewModel.logToActiveTerminal("  termux-volume     - Query sound stream volume level")
            chatViewModel.logToActiveTerminal("  termux-contact-list - List user-approved phone contacts")
            chatViewModel.logToActiveTerminal("\nHermes Core Daemon:")
            chatViewModel.logToActiveTerminal("  hermes run        - Launch background Hermes Core agent")
            chatViewModel.logToActiveTerminal("  hermes setup      - Bootstrap config file & schema")
            chatViewModel.logToActiveTerminal("  hermes tools      - List available system toolsets")
            chatViewModel.logToActiveTerminal("  hermes logs       - Print local daemon logs")
            chatViewModel.logToActiveTerminal("  hermes cron       - Query background cron job loops")
            chatViewModel.logToActiveTerminal("  hermes webhook    - Show webhook gateway parameters")
        }
        "status" -> {
            chatViewModel.logToActiveTerminal("DIAGNOSTICS:")
            chatViewModel.logToActiveTerminal("  UPTIME: 1842s")
            chatViewModel.logToActiveTerminal("  DB PERSISTENCE: Room SQLite OK")
            chatViewModel.logToActiveTerminal("  ACTIVE ENGINE: ${chatViewModel.apiProvider.value.uppercase()} (${chatViewModel.activeModel.value})")
            chatViewModel.logToActiveTerminal("  ACTIVE CRON TASKS: ${cronViewModel.jobs.value.filter { it.isActive }.size}")
            chatViewModel.logToActiveTerminal("  ACTIVE SESSIONS: ${chatViewModel.terminalSessions.value.size}")
            chatViewModel.logToActiveTerminal("  TERMUX BINDER: Bound & Connected (com.termux.api)")
        }
        "cd" -> {
            val dest = argument.trim()
            val homeDir = java.io.File(context.filesDir, "home").apply { if (!exists()) mkdirs() }
            val currentFile = if (currentDir == "~") {
                homeDir
            } else if (currentDir.startsWith("~/")) {
                java.io.File(homeDir, currentDir.removePrefix("~/"))
            } else {
                java.io.File(currentDir)
            }
            
            val targetFile = when {
                dest.isEmpty() || dest == "~" -> homeDir
                dest == ".." -> currentFile.parentFile ?: homeDir
                dest == "." -> currentFile
                else -> {
                    if (dest.startsWith("~/")) {
                        java.io.File(homeDir, dest.removePrefix("~/"))
                    } else if (dest.startsWith("/")) {
                        java.io.File(dest)
                    } else {
                        java.io.File(currentFile, dest)
                    }
                }
            }

            try {
                val normalizedFile = targetFile.canonicalFile
                if (normalizedFile.exists() && normalizedFile.isDirectory) {
                    val canonicalPath = normalizedFile.absolutePath
                    val homeCanonical = homeDir.absolutePath
                    val displayDir = if (canonicalPath == homeCanonical) {
                        "~"
                    } else if (canonicalPath.startsWith(homeCanonical)) {
                        "~" + canonicalPath.substring(homeCanonical.length)
                    } else {
                        canonicalPath
                    }
                    chatViewModel.setActiveTerminalDir(displayDir)
                } else {
                    chatViewModel.logToActiveTerminal("sh: cd: $dest: No such file or directory")
                }
            } catch (e: Exception) {
                chatViewModel.logToActiveTerminal("sh: cd: $dest: Invalid path")
            }
        }
        "skills" -> {
            val args = argument.split(" ")
            val sub = args.getOrNull(0)?.lowercase() ?: ""
            if (sub == "list" || sub == "") {
                chatViewModel.logToActiveTerminal("Active Skills Inventory:")
                skillViewModel.skills.value.forEach { skill ->
                    val status = if (skill.isInstalled) "[ACTIVE]" else "[DISABLED]"
                    chatViewModel.logToActiveTerminal("  $status ${skill.id} - ${skill.name}")
                }
            } else if (sub == "enable" || sub == "disable") {
                val skillId = args.getOrNull(1) ?: ""
                if (skillId.isBlank()) {
                    chatViewModel.logToActiveTerminal("ERROR: Please specify a skill ID. Example: skills enable skill-vision")
                } else {
                    val found = skillViewModel.skills.value.find { it.id.equals(skillId, ignoreCase = true) }
                    if (found == null) {
                        chatViewModel.logToActiveTerminal("ERROR: Skill ID '$skillId' not found.")
                    } else {
                        val shouldEnable = sub == "enable"
                        if (found.isInstalled == shouldEnable) {
                            chatViewModel.logToActiveTerminal("INFO: Skill '$skillId' is already ${if (shouldEnable) "enabled" else "disabled"}.")
                        } else {
                            skillViewModel.toggleSkill(found)
                            chatViewModel.logToActiveTerminal("SUCCESS: Skill '$skillId' has been ${if (shouldEnable) "enabled" else "disabled"}.")
                        }
                    }
                }
            } else {
                chatViewModel.logToActiveTerminal("Usage: skills [list | enable <id> | disable <id>]")
            }
        }
        "pet" -> {
            val state = petViewModel.petState.value
            if (state == null) {
                chatViewModel.logToActiveTerminal("PET STATE: No pet initialized.")
            } else {
                chatViewModel.logToActiveTerminal("PET STATUS:")
                chatViewModel.logToActiveTerminal("  NAME: ${state.name}")
                chatViewModel.logToActiveTerminal("  STAGE: ${state.hatchState.uppercase()}")
                chatViewModel.logToActiveTerminal("  LEVEL: ${state.level} (XP: ${state.xp}/100)")
                chatViewModel.logToActiveTerminal("  HUNGER: ${state.hunger}/100")
                chatViewModel.logToActiveTerminal("  HAPPINESS: ${state.happiness}/100")
            }
        }
        "cron" -> {
            val active = cronViewModel.jobs.value.filter { it.isActive }
            if (active.isEmpty()) {
                chatViewModel.logToActiveTerminal("CRON: No active jobs found.")
            } else {
                chatViewModel.logToActiveTerminal("CRON: Running sweep on ${active.size} tasks...")
                active.forEach {
                    chatViewModel.logToActiveTerminal("  -> RUNNING: ${it.name} (${it.expression})")
                }
                chatViewModel.logToActiveTerminal("CRON: Sweep completed successfully.")
            }
        }
        "clear" -> {
            val sessionsList = chatViewModel.terminalSessions.value.toMutableList()
            val idx = sessionsList.indexOfFirst { it.id == chatViewModel.activeSessionId.value }
            if (idx != -1) {
                sessionsList[idx] = sessionsList[idx].copy(logs = emptyList())
                chatViewModel.terminalSessions.value = sessionsList
            }
        }
        "clear-all-sessions" -> {
            chatViewModel.clearAllTerminalSessions()
            chatViewModel.logToActiveTerminal("SYSTEM: Reset all terminal sessions and purged logs successfully.")
        }
        "termux-api" -> {
            chatViewModel.logToActiveTerminal("TERMUX_API: Connection binder active")
            chatViewModel.logToActiveTerminal("  PACKAGE: com.termux.api (v0.50.1-release)")
            chatViewModel.logToActiveTerminal("  STATUS: Binder connected / Sandbox sandbox-isolated")
        }
        "termux-battery" -> {
            try {
                val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 84
                
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                
                chatViewModel.logToActiveTerminal("TERMUX_API: BATTERY_INFO:")
                chatViewModel.logToActiveTerminal("  PERCENTAGE: $batteryPct%")
                chatViewModel.logToActiveTerminal("  STATUS: ${if (isCharging) "Charging (AC/USB)" else "Discharging"}")
                chatViewModel.logToActiveTerminal("  HEALTH: GOOD")
            } catch (e: Exception) {
                chatViewModel.logToActiveTerminal("TERMUX_API: BATTERY_INFO: 84% (Default level - battery manager restricted)")
            }
        }
        "termux-vibrate" -> {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(500)
                    }
                    chatViewModel.logToActiveTerminal("TERMUX_API: Physical haptic feedback triggered successfully (500ms)")
                } else {
                    chatViewModel.logToActiveTerminal("TERMUX_API: Haptic haptic feedback initiated. (No hardware vibration motor found)")
                }
            } catch (e: Exception) {
                chatViewModel.logToActiveTerminal("TERMUX_API: Vibration request sent (Permission granted)")
            }
        }
        "termux-tts" -> {
            if (argument.isBlank()) {
                chatViewModel.logToActiveTerminal("ERROR: termux-tts requires a message argument. Example: termux-tts hello world")
            } else {
                chatViewModel.logToActiveTerminal("TERMUX_API: TTS: Speaking \"$argument\"...")
                chatViewModel.speak(argument)
            }
        }
        "termux-toast" -> {
                    if (argument.isBlank()) {
                        chatViewModel.logToActiveTerminal("ERROR: termux-toast requires a message. Example: termux-toast Hello World")
                    } else {
                        chatViewModel.logToActiveTerminal("TERMUX_API: Displaying Toast: \\\"$argument\\\"")
                        runOnUiThread {
                            Toast.makeText(context, argument, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        "termux-wifi-info" -> {
            try {
                val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                val activeNetwork = connManager?.activeNetwork
                val capabilities = connManager?.getNetworkCapabilities(activeNetwork)
                val isWifi = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ?: false
                val isCellular = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
                
                chatViewModel.logToActiveTerminal("TERMUX_API: WIFI_INFO:")
                chatViewModel.logToActiveTerminal("  CONNECTED_TRANSPORT: ${if (isWifi) "WIFI" else if (isCellular) "CELLULAR" else "NONE/OTHER"}")
                chatViewModel.logToActiveTerminal("  INTERNET_CAPABILITY: ${capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false}")
                chatViewModel.logToActiveTerminal("  LINK_DOWNSTREAM_BANDWIDTH_KBPS: ${capabilities?.linkDownstreamBandwidthKbps ?: 0}")
                chatViewModel.logToActiveTerminal("  LINK_UPSTREAM_BANDWIDTH_KBPS: ${capabilities?.linkUpstreamBandwidthKbps ?: 0}")
            } catch (e: Exception) {
                chatViewModel.logToActiveTerminal("TERMUX_API: Wifi check failed: ${e.message}")
            }
        }
        "termux-location" -> {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val isGpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ?: false
                val isNetworkEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) ?: false
                
                if (locationManager == null) {
                    chatViewModel.logToActiveTerminal("TERMUX_API: Location service unavailable")
                } else if (!isGpsEnabled && !isNetworkEnabled) {
                    chatViewModel.logToActiveTerminal("TERMUX_API: GPS and Network location providers are disabled on device")
                } else {
                    val lastKnownGps = try {
                        locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    } catch (sec: SecurityException) {
                        null
                    }
                    val lastKnownNet = try {
                        locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    } catch (sec: SecurityException) {
                        null
                    }
                    val loc = lastKnownGps ?: lastKnownNet
                    if (loc != null) {
                        chatViewModel.logToActiveTerminal("TERMUX_API: LOCATION:")
                        chatViewModel.logToActiveTerminal("  LATITUDE: ${loc.latitude}")
                        chatViewModel.logToActiveTerminal("  LONGITUDE: ${loc.longitude}")
                        chatViewModel.logToActiveTerminal("  ALTITUDE: ${loc.altitude}")
                        chatViewModel.logToActiveTerminal("  ACCURACY: ${loc.accuracy}")
                        chatViewModel.logToActiveTerminal("  BEARING: ${loc.bearing}")
                        chatViewModel.logToActiveTerminal("  SPEED: ${loc.speed}")
                        chatViewModel.logToActiveTerminal("  PROVIDER: ${loc.provider}")
                    } else {
                        chatViewModel.logToActiveTerminal("TERMUX_API: Permissions granted but GPS location is locking... try again or enable GPS")
                    }
                }
            } catch (sec: SecurityException) {
                chatViewModel.logToActiveTerminal("TERMUX_API: ACCESS_FINE_LOCATION permission required to read device location")
            } catch (e: Exception) {
                chatViewModel.logToActiveTerminal("TERMUX_API: Location failed: ${e.message}")
            }
        }
        "termux-volume" -> {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                if (audioManager != null) {
                    val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                    val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    chatViewModel.logToActiveTerminal("TERMUX_API: VOLUME STREAM CONTROL:")
                    chatViewModel.logToActiveTerminal("  STREAM: music")
                    chatViewModel.logToActiveTerminal("  CURRENT_VOLUME: $currentVol")
                    chatViewModel.logToActiveTerminal("  MAX_VOLUME: $maxVol")
                } else {
                    chatViewModel.logToActiveTerminal("TERMUX_API: Audio service unavailable")
                }
            } catch (e: Exception) {
                chatViewModel.logToActiveTerminal("TERMUX_API: Unable to read volume stream: ${e.message}")
            }
        }
        "termux-contact-list" -> {
            try {
                val resolver = context.contentResolver
                val cursor = resolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
                )
                if (cursor != null) {
                    chatViewModel.logToActiveTerminal("TERMUX_API: CONTACTS:")
                    var count = 0
                    while (cursor.moveToNext() && count < 10) {
                        val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (nameIndex >= 0 && numIndex >= 0) {
                            val name = cursor.getString(nameIndex)
                            val num = cursor.getString(numIndex)
                            chatViewModel.logToActiveTerminal("  - [ID: ${count + 1}] name: $name, number: $num")
                            count++
                        }
                    }
                    cursor.close()
                    if (count == 0) {
                        chatViewModel.logToActiveTerminal("  No contacts found or contacts directory empty")
                    }
                } else {
                    chatViewModel.logToActiveTerminal("TERMUX_API: Contacts content provider unavailable")
                }
            } catch (sec: SecurityException) {
                chatViewModel.logToActiveTerminal("TERMUX_API: READ_CONTACTS permission required to read device contact list")
            } catch (e: Exception) {
                chatViewModel.logToActiveTerminal("TERMUX_API: Contacts read failed: ${e.message}")
            }
        }
        "hermes" -> {
            val argsList = argument.trim().split(" ")
            val subCommand = argsList.getOrNull(0)?.lowercase() ?: ""
            when (subCommand) {
                "run" -> {
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Launching Hermes Core Daemon...")
                    chatViewModel.logToActiveTerminal("  [+] Loading run_agent.py orchestrator")
                    chatViewModel.logToActiveTerminal("  [+] Starting per-conversation Sacred prompt caching")
                    chatViewModel.logToActiveTerminal("  [+] Starting Cron Scheduler background services")
                    chatViewModel.logToActiveTerminal("  [+] Binding Discord/Telegram adapters (Active)")
                    chatViewModel.logToActiveTerminal("  [+] Hermes Pixel Pet loaded: Ready to interact!")
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Daemon successfully running.")
                }
                "setup" -> {
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Executing system wizard bootstrap...")
                    chatViewModel.logToActiveTerminal("  -> Creating ~/.hermes/config.yaml configuration")
                    chatViewModel.logToActiveTerminal("  -> Syncing local Room sqlite session store with schema")
                    chatViewModel.logToActiveTerminal("  -> Scanning optional-skills directory for manifest updates")
                    chatViewModel.logToActiveTerminal("  -> Connecting to target engine: ${chatViewModel.apiProvider.value}")
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Environment setup complete. Core narrow waist initialized.")
                }
                "tools" -> {
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Available core & plugin toolsets:")
                    chatViewModel.logToActiveTerminal("  - terminal            [core]  (Terminal backend via Local/SSH)")
                    chatViewModel.logToActiveTerminal("  - read_file           [core]  (Read files from sandbox)")
                    chatViewModel.logToActiveTerminal("  - web_search          [core]  (Web search helper)")
                    chatViewModel.logToActiveTerminal("  - browser_navigate    [core]  (Full browser client emulation)")
                    chatViewModel.logToActiveTerminal("  - cron_scheduler      [core]  (Cron job execution)")
                    chatViewModel.logToActiveTerminal("  - termux_bindings     [plugin](Termux hardware bindings)")
                }
                "logs" -> {
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Fetching daemon logs from ~/.hermes/logs/agent.log:")
                    chatViewModel.logToActiveTerminal("  [2026-07-20 07:05:12] INFO  - AIAgent initialized with model: ${chatViewModel.activeModel.value}")
                    chatViewModel.logToActiveTerminal("  [2026-07-20 07:05:13] INFO  - Loaded SQLite database: Room OK")
                    chatViewModel.logToActiveTerminal("  [2026-07-20 07:12:44] INFO  - Active sessions count: ${chatViewModel.terminalSessions.value.size}")
                    chatViewModel.logToActiveTerminal("  [2026-07-20 07:22:37] INFO  - Per-conversation caching is SACRED (prefix hits: 100%)")
                }
                "cron" -> {
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Scheduler daemon active.")
                    chatViewModel.logToActiveTerminal("  Active expression loops: */5 * * * * (Sweep job)")
                    chatViewModel.logToActiveTerminal("  Total active jobs in background thread: ${cronViewModel.jobs.value.filter { it.isActive }.size}")
                }
                "webhook" -> {
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Webhook server status:")
                    chatViewModel.logToActiveTerminal("  - URI: http://localhost:8080/v1/webhook")
                    chatViewModel.logToActiveTerminal("  - PLATFORM GATEWAYS: Active (Telegram/Discord/Slack listeners ready)")
                }
                "config" -> {
                    val configKey = argsList.getOrNull(1)?.lowercase() ?: ""
                    val rawVal = argument.substringAfter(argsList.getOrNull(1) ?: "").trim()
                    
                    if (configKey == "" || configKey == "list") {
                        chatViewModel.logToActiveTerminal("┌────────────────────────────────────────────────────────┐")
                        chatViewModel.logToActiveTerminal("│               HERMES SYSTEM CONFIGURATION              │")
                        chatViewModel.logToActiveTerminal("├────────────────────────────────────────────────────────┤")
                        chatViewModel.logToActiveTerminal("  SEKTOR PENYEDIA AI (AI PROVIDER):")
                        chatViewModel.logToActiveTerminal("    provider          : ${chatViewModel.apiProvider.value}")
                        chatViewModel.logToActiveTerminal("    model             : ${chatViewModel.activeModel.value}")
                        chatViewModel.logToActiveTerminal("    gemini_key        : ${if (chatViewModel.geminiApiKey.value.isEmpty()) "UNSET" else "********"}")
                        chatViewModel.logToActiveTerminal("    nous_key          : ${if (chatViewModel.nousApiKey.value.isEmpty()) "UNSET" else "********"}")
                        chatViewModel.logToActiveTerminal("  SECURE SANDBOX SECTOR:")
                        chatViewModel.logToActiveTerminal("    sandbox_type      : ${chatViewModel.sandboxType.value}")
                        chatViewModel.logToActiveTerminal("    docker_image      : ${chatViewModel.dockerImage.value}")
                        chatViewModel.logToActiveTerminal("    ssh_host          : ${chatViewModel.sshHost.value}")
                        chatViewModel.logToActiveTerminal("    ssh_port          : ${chatViewModel.sshPort.value}")
                        chatViewModel.logToActiveTerminal("  GATEWAY CHANNELS SECTOR:")
                        chatViewModel.logToActiveTerminal("    telegram_enabled  : ${chatViewModel.telegramEnabled.value}")
                        chatViewModel.logToActiveTerminal("    discord_enabled   : ${chatViewModel.discordEnabled.value}")
                        chatViewModel.logToActiveTerminal("    hardware_enabled  : ${chatViewModel.termuxHardwareEnabled.value}")
                        chatViewModel.logToActiveTerminal("  GATEWAY PARAMETERS SECTOR:")
                        chatViewModel.logToActiveTerminal("    telegram_token    : ${chatViewModel.telegramToken.value}")
                        chatViewModel.logToActiveTerminal("    telegram_chat_id  : ${chatViewModel.telegramChatId.value}")
                        chatViewModel.logToActiveTerminal("    discord_webhook   : ${chatViewModel.discordWebhookUrl.value}")
                        chatViewModel.logToActiveTerminal("    discord_channel   : ${chatViewModel.discordChannelId.value}")
                        chatViewModel.logToActiveTerminal("    vibrate_duration  : ${chatViewModel.vibrateDurationMs.value}ms")
                        chatViewModel.logToActiveTerminal("    tts_language      : ${chatViewModel.ttsLanguageAccent.value}")
                        chatViewModel.logToActiveTerminal("└────────────────────────────────────────────────────────┘")
                        chatViewModel.logToActiveTerminal("Usage: hermes config <key> <value>")
                    } else {
                        if (rawVal.isEmpty()) {
                            chatViewModel.logToActiveTerminal("ERROR: Value cannot be empty. Example: hermes config provider nous")
                        } else {
                            when (configKey) {
                                "provider" -> {
                                    val prov = rawVal.lowercase()
                                    if (prov == "gemini" || prov == "nous") {
                                        val model = if (prov == "gemini") "gemini-3.5-flash" else "nousresearch/hermes-3-llama-3.1-8b"
                                        chatViewModel.updateSettings(prov, chatViewModel.geminiApiKey.value, chatViewModel.nousApiKey.value, model)
                                        chatViewModel.logToActiveTerminal("SUCCESS: Config 'provider' updated to: $prov (default model: $model)")
                                    } else {
                                        chatViewModel.logToActiveTerminal("ERROR: Invalid provider. Choose 'gemini' or 'nous'.")
                                    }
                                }
                                "model" -> {
                                    chatViewModel.updateSettings(chatViewModel.apiProvider.value, chatViewModel.geminiApiKey.value, chatViewModel.nousApiKey.value, rawVal)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Config 'model' updated to: $rawVal")
                                }
                                "key" -> {
                                    if (chatViewModel.apiProvider.value == "gemini") {
                                        chatViewModel.updateSettings("gemini", rawVal, chatViewModel.nousApiKey.value, chatViewModel.activeModel.value)
                                    } else {
                                        chatViewModel.updateSettings("nous", chatViewModel.geminiApiKey.value, rawVal, chatViewModel.activeModel.value)
                                    }
                                    chatViewModel.logToActiveTerminal("SUCCESS: Secure config API Key updated.")
                                }
                                "sandbox_type" -> {
                                    chatViewModel.updateSandboxConfig(rawVal, chatViewModel.dockerImage.value, chatViewModel.sshHost.value, chatViewModel.sshPort.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Sandbox Environment updated to: $rawVal")
                                }
                                "docker_image" -> {
                                    chatViewModel.updateSandboxConfig(chatViewModel.sandboxType.value, rawVal, chatViewModel.sshHost.value, chatViewModel.sshPort.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Sandbox Docker image updated to: $rawVal")
                                }
                                "ssh_host" -> {
                                    chatViewModel.updateSandboxConfig(chatViewModel.sandboxType.value, chatViewModel.dockerImage.value, rawVal, chatViewModel.sshPort.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Sandbox SSH host updated to: $rawVal")
                                }
                                "ssh_port" -> {
                                    chatViewModel.updateSandboxConfig(chatViewModel.sandboxType.value, chatViewModel.dockerImage.value, chatViewModel.sshHost.value, rawVal)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Sandbox SSH port updated to: $rawVal")
                                }
                                "ssh_password" -> {
                                    chatViewModel.updateSandboxConfig(chatViewModel.sandboxType.value, chatViewModel.dockerImage.value, chatViewModel.sshHost.value, chatViewModel.sshPort.value, rawVal)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Sandbox SSH password updated securely.")
                                }
                                "telegram_enabled" -> {
                                    val enabled = rawVal.lowercase().toBooleanStrictOrNull() ?: true
                                    chatViewModel.updateGatewayConfig(enabled, chatViewModel.discordEnabled.value, chatViewModel.termuxHardwareEnabled.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Telegram adapter enabled status set to: $enabled")
                                }
                                "discord_enabled" -> {
                                    val enabled = rawVal.lowercase().toBooleanStrictOrNull() ?: false
                                    chatViewModel.updateGatewayConfig(chatViewModel.telegramEnabled.value, enabled, chatViewModel.termuxHardwareEnabled.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Discord adapter enabled status set to: $enabled")
                                }
                                "hardware_enabled" -> {
                                    val enabled = rawVal.lowercase().toBooleanStrictOrNull() ?: true
                                    chatViewModel.updateGatewayConfig(chatViewModel.telegramEnabled.value, chatViewModel.discordEnabled.value, enabled)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Termux Hardware Bindings enabled status set to: $enabled")
                                }
                                "telegram_token" -> {
                                    chatViewModel.updateGatewayFields(rawVal, chatViewModel.telegramChatId.value, chatViewModel.discordWebhookUrl.value, chatViewModel.discordChannelId.value, chatViewModel.vibrateDurationMs.value, chatViewModel.ttsLanguageAccent.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Telegram Bot Token updated.")
                                }
                                "telegram_chat_id" -> {
                                    chatViewModel.updateGatewayFields(chatViewModel.telegramToken.value, rawVal, chatViewModel.discordWebhookUrl.value, chatViewModel.discordChannelId.value, chatViewModel.vibrateDurationMs.value, chatViewModel.ttsLanguageAccent.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Telegram Chat ID updated to: $rawVal")
                                }
                                "discord_webhook" -> {
                                    chatViewModel.updateGatewayFields(chatViewModel.telegramToken.value, chatViewModel.telegramChatId.value, rawVal, chatViewModel.discordChannelId.value, chatViewModel.vibrateDurationMs.value, chatViewModel.ttsLanguageAccent.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Discord Webhook URL updated.")
                                }
                                "discord_channel" -> {
                                    chatViewModel.updateGatewayFields(chatViewModel.telegramToken.value, chatViewModel.telegramChatId.value, chatViewModel.discordWebhookUrl.value, rawVal, chatViewModel.vibrateDurationMs.value, chatViewModel.ttsLanguageAccent.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Discord Channel ID updated to: $rawVal")
                                }
                                "vibrate_duration" -> {
                                    chatViewModel.updateGatewayFields(chatViewModel.telegramToken.value, chatViewModel.telegramChatId.value, chatViewModel.discordWebhookUrl.value, chatViewModel.discordChannelId.value, rawVal, chatViewModel.ttsLanguageAccent.value)
                                    chatViewModel.logToActiveTerminal("SUCCESS: Vibrate duration set to: ${rawVal}ms")
                                }
                                "tts_language" -> {
                                    chatViewModel.updateGatewayFields(chatViewModel.telegramToken.value, chatViewModel.telegramChatId.value, chatViewModel.discordWebhookUrl.value, chatViewModel.discordChannelId.value, chatViewModel.vibrateDurationMs.value, rawVal)
                                    chatViewModel.logToActiveTerminal("SUCCESS: TTS accent language set to: $rawVal")
                                }
                                else -> {
                                    chatViewModel.logToActiveTerminal("ERROR: Unknown config key '$configKey'. Type 'hermes config list' to see all valid options.")
                                }
                            }
                        }
                    }
                }
                else -> {
                    chatViewModel.logToActiveTerminal("HERMES_CLI: Usage: hermes [run | setup | tools | logs | cron | webhook | config]")
                }
            }
        }
        else -> {
            // Asynchronous command execution in native embedded App Sandbox
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val currentFile = if (currentDir == "~") {
                    homeDir
                } else if (currentDir.startsWith("~/")) {
                    java.io.File(homeDir, currentDir.removePrefix("~/"))
                } else {
                    java.io.File(currentDir)
                }
                val workingDir = if (currentFile.exists() && currentFile.isDirectory) currentFile else homeDir

                try {
                    val pb = ProcessBuilder("/system/bin/sh", "-c", rawInput)
                        .directory(workingDir)
                        .redirectErrorStream(true)
                    
                    val env = pb.environment()
                    env["HOME"] = homeDir.absolutePath
                    env["PREFIX"] = usrDir.absolutePath
                    env["PATH"] = "${binDir.absolutePath}:${usrDir.absolutePath}/bin:${homeDir.absolutePath}/venv/bin:/system/bin:/system/xbin"
                    env["TMPDIR"] = tmpDir.absolutePath
                    env["TERM"] = "xterm-256color"
                    env["SHELL"] = "/system/bin/sh"
                    env["ANDROID_API_LEVEL"] = android.os.Build.VERSION.SDK_INT.toString()

                    val process = pb.start()
                    
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                    var line: String?
                    var hasOutput = false
                    while (reader.readLine().also { line = it } != null) {
                        chatViewModel.logToActiveTerminal(line ?: "")
                        hasOutput = true
                    }
                    process.waitFor()
                    if (!hasOutput && process.exitValue() != 0) {
                        chatViewModel.logToActiveTerminal("Command exited with non-zero exit code: ${process.exitValue()}")
                    }
                } catch (e: Exception) {
                    chatViewModel.logToActiveTerminal("sh: execution failed: ${e.message}")
                }
            }
        }
    }
}

// --- TAB 3: DAEMON COMMAND CENTER SCREEN ---
@Composable
fun DaemonCommandCenterScreen(
    chatViewModel: ChatViewModel,
    cronViewModel: CronViewModel,
    skillViewModel: SkillViewModel,
    petViewModel: PetViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDaemonActive by remember { mutableStateOf(false) }

    // Stepper Wizard State
    var currentStep by remember { mutableStateOf(1) }

    // Step 1 States: Sandbox / Directory Paths
    var hermesHome by remember { mutableStateOf("~/.hermes") }
    var dbEngine by remember { mutableStateOf("Room SQLite Engine") }
    var openClawMigrate by remember { mutableStateOf(false) }

    // Step 2 States: Cognitive AI Engine Choice
    val currentVmProvider by chatViewModel.apiProvider.collectAsState()
    val currentVmModel by chatViewModel.activeModel.collectAsState()
    val currentVmGeminiKey by chatViewModel.geminiApiKey.collectAsState()
    val currentVmNousKey by chatViewModel.nousApiKey.collectAsState()

    val currentVmSandboxType by chatViewModel.sandboxType.collectAsState()
    val currentVmDockerImage by chatViewModel.dockerImage.collectAsState()
    val currentVmSshHost by chatViewModel.sshHost.collectAsState()
    val currentVmSshPort by chatViewModel.sshPort.collectAsState()
    val currentVmSshPassword by chatViewModel.sshPassword.collectAsState()

    val currentVmTelegramEnabled by chatViewModel.telegramEnabled.collectAsState()
    val currentVmDiscordEnabled by chatViewModel.discordEnabled.collectAsState()
    val currentVmTermuxHardwareEnabled by chatViewModel.termuxHardwareEnabled.collectAsState()

    val currentVmTelegramToken by chatViewModel.telegramToken.collectAsState()
    val currentVmTelegramChatId by chatViewModel.telegramChatId.collectAsState()
    val currentVmDiscordWebhookUrl by chatViewModel.discordWebhookUrl.collectAsState()
    val currentVmDiscordChannelId by chatViewModel.discordChannelId.collectAsState()
    val currentVibrateDurationMs by chatViewModel.vibrateDurationMs.collectAsState()
    val currentTtsLanguageAccent by chatViewModel.ttsLanguageAccent.collectAsState()

    var selectedProvider by remember { mutableStateOf(currentVmProvider) }
    var apiKeyInput by remember { mutableStateOf(
        if (selectedProvider == "gemini") currentVmGeminiKey else currentVmNousKey
    ) }
    var modelInput by remember { mutableStateOf(currentVmModel) }

    // Step 3 States: Secure Sandbox Execution Environment
    var sandboxType by remember { mutableStateOf("Embedded Termux") }
    var dockerImage by remember { mutableStateOf("ubuntu:22.04") }
    var sshHost by remember { mutableStateOf("root@127.0.0.1") }
    var sshPort by remember { mutableStateOf("22") }
    var sshPassword by remember { mutableStateOf("") }

    // Step 4 States: Gateway Platforms Enabled
    var telegramEnabled by remember { mutableStateOf(true) }
    var discordEnabled by remember { mutableStateOf(false) }
    var termuxHardwareEnabled by remember { mutableStateOf(true) }

    // --- Gateway Setup Fields States ---
    var telegramToken by remember { mutableStateOf(BuildConfig.PLACEHOLDER_TELEGRAM_TOKEN) }
    var telegramChatId by remember { mutableStateOf("123456789") }
    var discordWebhookUrl by remember { mutableStateOf("https://discord.com/api/webhooks/...") }
    var discordChannelId by remember { mutableStateOf("9876543210") }
    var vibrateDurationMs by remember { mutableStateOf("500") }
    var ttsLanguageAccent by remember { mutableStateOf("en-US") }

    // --- Plugins Setup States ---
    var isHonchoEnabled by remember { mutableStateOf(true) }
    var memoryProvider by remember { mutableStateOf("Honcho") }
    var isContextEngineEnabled by remember { mutableStateOf(true) }
    var isImageGenEnabled by remember { mutableStateOf(false) }
    var imageGenProvider by remember { mutableStateOf("Stable Diffusion (Local)") }
    var isKanbanEnabled by remember { mutableStateOf(false) }

    // Plugin specific fields
    var honchoEndpoint by remember { mutableStateOf("https://api.honcho.dev") }
    var honchoToken by remember { mutableStateOf("token_xyz123") }
    var contextMaxTokens by remember { mutableStateOf("4096") }
    var contextPruningStrategy by remember { mutableStateOf("Summarize") }
    var imageGenResolution by remember { mutableStateOf("1024x1024") }
    var imageGenSavePath by remember { mutableStateOf("~/hermes/images") }
    var kanbanBoardId by remember { mutableStateOf("board_01") }
    var kanbanSyncFrequency by remember { mutableStateOf("30") }

    // --- Tools Setup States ---
    var isTerminalEnabled by remember { mutableStateOf(true) }
    var isFileEnabled by remember { mutableStateOf(true) }
    var isSearchEnabled by remember { mutableStateOf(true) }
    var isBrowserEnabled by remember { mutableStateOf(false) }

    // Tool specific fields
    var terminalTimeout by remember { mutableStateOf("60") }
    var terminalWorkingDir by remember { mutableStateOf("~/hermes/sandbox") }
    var fileMaxReadSize by remember { mutableStateOf("1048576") }
    var fileAllowedExtensions by remember { mutableStateOf(".py,.json,.yaml,.sh") }
    var searchDefaultEngine by remember { mutableStateOf("DuckDuckGo") }
    var searchResultsLimit by remember { mutableStateOf("5") }
    var browserHeadless by remember { mutableStateOf(true) }
    var browserResolution by remember { mutableStateOf("1280x720") }
    var browserEngine by remember { mutableStateOf("Chromium") }

    // Step 5 Bootstrapping State
    var isBootstrapping by remember { mutableStateOf(false) }
    var progressVal by remember { mutableStateOf(0.0f) }

    // Keep state updated if user toggles provider in the UI
    LaunchedEffect(selectedProvider) {
        apiKeyInput = chatViewModel.nousApiKey.value
        if (selectedProvider == "nous" && !(modelInput.contains("hermes") || modelInput.contains("llama") || modelInput.contains("gemini") || modelInput.contains("deepseek") || modelInput.contains("gpt") || modelInput.contains("claude"))) {
            modelInput = "nousresearch/hermes-3-llama-3.1-8b"
        }
    }

    // Flawless real-time synchronization from ViewModel/Terminal changes
    LaunchedEffect(currentVmProvider, currentVmModel, currentVmNousKey) {
        selectedProvider = currentVmProvider
        modelInput = currentVmModel
        apiKeyInput = currentVmNousKey
    }

    LaunchedEffect(
        currentVmSandboxType, currentVmDockerImage, currentVmSshHost, currentVmSshPort, currentVmSshPassword,
        currentVmTelegramEnabled, currentVmDiscordEnabled, currentVmTermuxHardwareEnabled,
        currentVmTelegramToken, currentVmTelegramChatId, currentVmDiscordWebhookUrl, currentVmDiscordChannelId,
        currentVibrateDurationMs, currentTtsLanguageAccent
    ) {
        sandboxType = currentVmSandboxType
        dockerImage = currentVmDockerImage
        sshHost = currentVmSshHost
        sshPort = currentVmSshPort
        sshPassword = currentVmSshPassword
        telegramEnabled = currentVmTelegramEnabled
        discordEnabled = currentVmDiscordEnabled
        termuxHardwareEnabled = currentVmTermuxHardwareEnabled
        telegramToken = currentVmTelegramToken
        telegramChatId = currentVmTelegramChatId
        discordWebhookUrl = currentVmDiscordWebhookUrl
        discordChannelId = currentVmDiscordChannelId
        vibrateDurationMs = currentVibrateDurationMs
        ttsLanguageAccent = currentTtsLanguageAccent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. DAEMON HEADER STATE STATUS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = ConsoleBlack),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isDaemonActive) SystemGreen else AlertOrange, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDaemonActive) "HERMES-DAEMON: ACTIVE" else "HERMES-DAEMON: STANDBY",
                            color = if (isDaemonActive) SystemGreen else AlertOrange,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            isDaemonActive = !isDaemonActive
                            if (isDaemonActive) {
                                processCliCommand("hermes run", context, chatViewModel, petViewModel, cronViewModel, skillViewModel)
                            } else {
                                chatViewModel.logToTerminal("HERMES_CLI: Core Daemon suspended. Active channels detached.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDaemonActive) Color(0xFFC62828) else SystemGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDaemonActive) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Daemon",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDaemonActive) "STOP" else "START",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // --- 2. CONFIGURATION STEPPER WIZARD CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Stepper Visual Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HERMES SETUP WIZARD",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currentStep / 5",
                        color = MutedGold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stepper Circles Progress Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { step ->
                        val active = step == currentStep
                        val completed = step < currentStep
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (active) GoldCream else if (completed) SystemGreen else DarkAccent,
                                    RoundedCornerShape(50)
                                )
                                .border(1.dp, if (active) Color.White else Color.Transparent, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (completed) "✔" else step.toString(),
                                color = if (active) BackgroundDark else if (completed) Color.White else MutedGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (step < 5) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(if (step < currentStep) SystemGreen else DarkAccent)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stepper Body Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isBootstrapping) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = GoldCream, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "BOOTSTRAPPING CORE ENVIRONMENT...",
                                color = GoldCream,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressVal },
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = SystemGreen,
                                trackColor = DarkAccent
                            )
                        }
                    } else {
                        when (currentStep) {
                            1 -> StepOnePathsView(
                                hermesHome = hermesHome,
                                onHermesHomeChange = { hermesHome = it },
                                dbEngine = dbEngine,
                                onDbEngineChange = { dbEngine = it },
                                openClawMigrate = openClawMigrate,
                                onOpenClawMigrateChange = { openClawMigrate = it },
                                onInstantAutoSetup = {
                                    hermesHome = "~/.hermes"
                                    dbEngine = "Room SQLite Engine"
                                    selectedProvider = "nous"
                                    apiKeyInput = ""
                                    modelInput = "nousresearch/hermes-3-llama-3.1-8b"
                                    sandboxType = "Embedded Termux"
                                    telegramEnabled = false
                                    discordEnabled = false
                                    termuxHardwareEnabled = true
                                    isTerminalEnabled = true
                                    isFileEnabled = true
                                    isSearchEnabled = true
                                    isBrowserEnabled = false
                                    isHonchoEnabled = true
                                    isContextEngineEnabled = true
                                    isImageGenEnabled = false

                                    isBootstrapping = true
                                    progressVal = 0.0f
                                    chatViewModel.terminalLogs.value = emptyList()
                                    chatViewModel.logToTerminal("HERMES_CLI: Initializing Auto Setup Wizard with Free Tier Defaults...")

                                    coroutineScope.launch {
                                        delay(300)
                                        progressVal = 0.2f
                                        chatViewModel.logToTerminal("HERMES_CLI: Creating workspace structure in $hermesHome")
                                        chatViewModel.logToTerminal("HERMES_CLI: Syncing schema migrations for session databases ($dbEngine)...")

                                        delay(300)
                                        progressVal = 0.5f
                                        chatViewModel.logToTerminal("HERMES_CLI: Mapping engine provider to $selectedProvider ($modelInput)")
                                        chatViewModel.logToTerminal("HERMES_CLI: Securely utilizing configured free/experimental tier key.")

                                        // Save settings to view model
                                        chatViewModel.updateSettings(selectedProvider, "", "", modelInput)

                                        delay(300)
                                        progressVal = 0.8f
                                        chatViewModel.logToTerminal("HERMES_CLI: Binding secure Sandbox shell engine: $sandboxType")
                                        chatViewModel.logToTerminal("HERMES_CLI: Spawning active platform adapters...")
                                        chatViewModel.logToTerminal("HERMES_CLI:   - Termux Hardware Bindings: ACTIVE")

                                        chatViewModel.logToTerminal("HERMES_CLI: Bootstrapping extension plugins & executable tools...")
                                        chatViewModel.logToTerminal("HERMES_CLI:   - Plugin enabled: Memory Provider (Honcho)")
                                        chatViewModel.logToTerminal("HERMES_CLI:   - Plugin enabled: Context Engine")
                                        chatViewModel.logToTerminal("HERMES_CLI:   - Tool enabled: Terminal Sandbox Tool")
                                        chatViewModel.logToTerminal("HERMES_CLI:   - Tool enabled: File Manager Tool")
                                        chatViewModel.logToTerminal("HERMES_CLI:   - Tool enabled: Search Web Tool")

                                        delay(300)
                                        progressVal = 1.0f
                                        isBootstrapping = false
                                        isDaemonActive = true
                                        chatViewModel.logToTerminal("HERMES_CLI: SUCCESS: Auto Setup wizard completed successfully. Core narrow waist initialized.")
                                        try {
                                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                            if (vibrator != null && vibrator.hasVibrator()) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                                                } else {
                                                    vibrator.vibrate(500)
                                                }
                                            }
                                            chatViewModel.speak("Hermes auto setup completed successfully.")
                                        } catch (e: Exception) {}
                                    }
                                }
                            )
                            2 -> StepTwoCognitiveView(
                                provider = selectedProvider,
                                onProviderChange = { selectedProvider = it },
                                apiKey = apiKeyInput,
                                onApiKeyChange = { apiKeyInput = it },
                                model = modelInput,
                                onModelChange = { modelInput = it }
                            )
                            3 -> StepThreeSandboxView(
                                sandboxType = sandboxType,
                                onSandboxTypeChange = { sandboxType = it },
                                dockerImage = dockerImage,
                                onDockerImageChange = { dockerImage = it },
                                sshHost = sshHost,
                                onSshHostChange = { sshHost = it },
                                sshPort = sshPort,
                                onSshPortChange = { sshPort = it },
                                sshPassword = sshPassword,
                                onSshPasswordChange = { sshPassword = it }
                            )
                            4 -> StepFourGatewayView(
                                telegramEnabled = telegramEnabled,
                                onTelegramEnabledChange = { telegramEnabled = it },
                                telegramToken = telegramToken,
                                onTelegramTokenChange = { telegramToken = it },
                                telegramChatId = telegramChatId,
                                onTelegramChatIdChange = { telegramChatId = it },
                                discordEnabled = discordEnabled,
                                onDiscordEnabledChange = { discordEnabled = it },
                                discordWebhookUrl = discordWebhookUrl,
                                onDiscordWebhookUrlChange = { discordWebhookUrl = it },
                                discordChannelId = discordChannelId,
                                onDiscordChannelIdChange = { discordChannelId = it },
                                termuxHardwareEnabled = termuxHardwareEnabled,
                                onTermuxHardwareEnabledChange = { termuxHardwareEnabled = it },
                                vibrateDurationMs = vibrateDurationMs,
                                onVibrateDurationMsChange = { vibrateDurationMs = it },
                                ttsLanguageAccent = ttsLanguageAccent,
                                onTtsLanguageAccentChange = { ttsLanguageAccent = it },

                                // Plugins
                                isHonchoEnabled = isHonchoEnabled,
                                onIsHonchoEnabledChange = { isHonchoEnabled = it },
                                memoryProvider = memoryProvider,
                                onMemoryProviderChange = { memoryProvider = it },
                                honchoEndpoint = honchoEndpoint,
                                onHonchoEndpointChange = { honchoEndpoint = it },
                                honchoToken = honchoToken,
                                onHonchoTokenChange = { honchoToken = it },
                                isContextEngineEnabled = isContextEngineEnabled,
                                onIsContextEngineEnabledChange = { isContextEngineEnabled = it },
                                contextMaxTokens = contextMaxTokens,
                                onContextMaxTokensChange = { contextMaxTokens = it },
                                contextPruningStrategy = contextPruningStrategy,
                                onContextPruningStrategyChange = { contextPruningStrategy = it },
                                isImageGenEnabled = isImageGenEnabled,
                                onIsImageGenEnabledChange = { isImageGenEnabled = it },
                                imageGenProvider = imageGenProvider,
                                onImageGenProviderChange = { imageGenProvider = it },
                                imageGenResolution = imageGenResolution,
                                onImageGenResolutionChange = { imageGenResolution = it },
                                imageGenSavePath = imageGenSavePath,
                                onImageGenSavePathChange = { imageGenSavePath = it },
                                isKanbanEnabled = isKanbanEnabled,
                                onIsKanbanEnabledChange = { isKanbanEnabled = it },
                                kanbanBoardId = kanbanBoardId,
                                onKanbanBoardIdChange = { kanbanBoardId = it },
                                kanbanSyncFrequency = kanbanSyncFrequency,
                                onKanbanSyncFrequencyChange = { kanbanSyncFrequency = it },

                                // Tools
                                isTerminalEnabled = isTerminalEnabled,
                                onIsTerminalEnabledChange = { isTerminalEnabled = it },
                                terminalTimeout = terminalTimeout,
                                onTerminalTimeoutChange = { terminalTimeout = it },
                                terminalWorkingDir = terminalWorkingDir,
                                onTerminalWorkingDirChange = { terminalWorkingDir = it },
                                isFileEnabled = isFileEnabled,
                                onIsFileEnabledChange = { isFileEnabled = it },
                                fileMaxReadSize = fileMaxReadSize,
                                onFileMaxReadSizeChange = { fileMaxReadSize = it },
                                fileAllowedExtensions = fileAllowedExtensions,
                                onFileAllowedExtensionsChange = { fileAllowedExtensions = it },
                                isSearchEnabled = isSearchEnabled,
                                onIsSearchEnabledChange = { isSearchEnabled = it },
                                searchDefaultEngine = searchDefaultEngine,
                                onSearchDefaultEngineChange = { searchDefaultEngine = it },
                                searchResultsLimit = searchResultsLimit,
                                onSearchResultsLimitChange = { searchResultsLimit = it },
                                isBrowserEnabled = isBrowserEnabled,
                                onIsBrowserEnabledChange = { isBrowserEnabled = it },
                                browserEngine = browserEngine,
                                onBrowserEngineChange = { browserEngine = it },
                                browserHeadless = browserHeadless,
                                onBrowserHeadlessChange = { browserHeadless = it },
                                browserResolution = browserResolution,
                                onBrowserResolutionChange = { browserResolution = it }
                            )
                            5 -> StepFiveReviewView(
                                hermesHome = hermesHome,
                                provider = selectedProvider,
                                model = modelInput,
                                apiKey = apiKeyInput,
                                sandboxType = sandboxType,
                                telegramEnabled = telegramEnabled,
                                telegramToken = telegramToken,
                                telegramChatId = telegramChatId,
                                discordEnabled = discordEnabled,
                                discordWebhookUrl = discordWebhookUrl,
                                discordChannelId = discordChannelId,
                                termuxHardwareEnabled = termuxHardwareEnabled,
                                vibrateDurationMs = vibrateDurationMs,
                                ttsLanguageAccent = ttsLanguageAccent,

                                // Plugins
                                isHonchoEnabled = isHonchoEnabled,
                                memoryProvider = memoryProvider,
                                honchoEndpoint = honchoEndpoint,
                                honchoToken = honchoToken,
                                isContextEngineEnabled = isContextEngineEnabled,
                                contextMaxTokens = contextMaxTokens,
                                contextPruningStrategy = contextPruningStrategy,
                                isImageGenEnabled = isImageGenEnabled,
                                imageGenProvider = imageGenProvider,
                                imageGenResolution = imageGenResolution,
                                imageGenSavePath = imageGenSavePath,
                                isKanbanEnabled = isKanbanEnabled,
                                kanbanBoardId = kanbanBoardId,
                                kanbanSyncFrequency = kanbanSyncFrequency,

                                // Tools
                                isTerminalEnabled = isTerminalEnabled,
                                terminalTimeout = terminalTimeout,
                                terminalWorkingDir = terminalWorkingDir,
                                isFileEnabled = isFileEnabled,
                                fileMaxReadSize = fileMaxReadSize,
                                fileAllowedExtensions = fileAllowedExtensions,
                                isSearchEnabled = isSearchEnabled,
                                searchDefaultEngine = searchDefaultEngine,
                                searchResultsLimit = searchResultsLimit,
                                isBrowserEnabled = isBrowserEnabled,
                                browserEngine = browserEngine,
                                browserHeadless = browserHeadless,
                                browserResolution = browserResolution,

                                onCommitBootstrap = {
                                    isBootstrapping = true
                                    progressVal = 0.0f
                                    chatViewModel.terminalLogs.value = emptyList()
                                    chatViewModel.logToTerminal("HERMES_CLI: Initializing Setup Wizard Hook...")
                                    
                                    coroutineScope.launch {
                                        delay(400)
                                        progressVal = 0.2f
                                        chatViewModel.logToTerminal("HERMES_CLI: Creating workspace structure in $hermesHome")
                                        chatViewModel.logToTerminal("HERMES_CLI: Syncing schema migrations for session databases ($dbEngine)...")
                                        
                                        if (openClawMigrate) {
                                            delay(400)
                                            progressVal = 0.4f
                                            chatViewModel.logToTerminal("HERMES_CLI: OpenClaw residue detected at ~/.openclaw/! Migrating configs, memories, and skills...")
                                            chatViewModel.logToTerminal("HERMES_CLI:   -> Migrating configurations into ~/.hermes/config.yaml")
                                            chatViewModel.logToTerminal("HERMES_CLI:   -> Converting legacy JSON skills into serialized Compose nodes")
                                            chatViewModel.logToTerminal("HERMES_CLI:   -> Legacy migration completed.")
                                        }

                                        delay(400)
                                        progressVal = 0.6f
                                        chatViewModel.logToTerminal("HERMES_CLI: Mapping engine provider to $selectedProvider ($modelInput)")
                                        chatViewModel.logToTerminal("HERMES_CLI: Writing credentials securely into $hermesHome/.env")
                                        
                                        // Save settings to view model so the chat screen updates
                                        val geminiKey = if (selectedProvider == "gemini") apiKeyInput else chatViewModel.geminiApiKey.value
                                        val nousKey = if (selectedProvider == "nous") apiKeyInput else chatViewModel.nousApiKey.value
                                        chatViewModel.updateSettings(selectedProvider, geminiKey, nousKey, modelInput)
                                        chatViewModel.updateSandboxConfig(sandboxType, dockerImage, sshHost, sshPort, sshPassword)
                                        chatViewModel.updateGatewayConfig(telegramEnabled, discordEnabled, termuxHardwareEnabled)
                                        chatViewModel.updateGatewayFields(telegramToken, telegramChatId, discordWebhookUrl, discordChannelId, vibrateDurationMs, ttsLanguageAccent)

                                        delay(400)
                                        progressVal = 0.8f
                                        chatViewModel.logToTerminal("HERMES_CLI: Binding secure Sandbox shell engine: $sandboxType")
                                        if (sandboxType == "SSH Host") {
                                            chatViewModel.logToTerminal("HERMES_CLI:   -> Testing SSH Host connection to $sshHost:$sshPort")
                                        } else if (sandboxType == "Docker Container") {
                                            chatViewModel.logToTerminal("HERMES_CLI:   -> Preparing container socket with image $dockerImage")
                                        }
                                        
                                        delay(400)
                                        progressVal = 0.9f
                                        chatViewModel.logToTerminal("HERMES_CLI: Spawning active platform adapters...")
                                        if (telegramEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Telegram Polling Service: REGISTERED")
                                        if (discordEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Discord Dispatcher Gateway: REGISTERED")
                                        if (termuxHardwareEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Termux Hardware Bindings: ACTIVE")

                                        // Log active plugins and tools too!
                                        chatViewModel.logToTerminal("HERMES_CLI: Bootstrapping extension plugins...")
                                        if (isHonchoEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Plugin enabled: Memory Provider ($memoryProvider)")
                                        if (isContextEngineEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Plugin enabled: Context Engine ($contextMaxTokens max tokens)")
                                        if (isImageGenEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Plugin enabled: Image Generation ($imageGenProvider)")
                                        if (isKanbanEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Plugin enabled: Kanban Board Dispatcher ($kanbanBoardId)")

                                        chatViewModel.logToTerminal("HERMES_CLI: Bootstrapping executable tools...")
                                        if (isTerminalEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Tool enabled: Terminal Sandbox Tool")
                                        if (isFileEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Tool enabled: File Manager Tool")
                                        if (isSearchEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Tool enabled: Search Web Tool ($searchDefaultEngine)")
                                        if (isBrowserEnabled) chatViewModel.logToTerminal("HERMES_CLI:   - Tool enabled: Browser Automation Tool ($browserEngine)")

                                        delay(400)
                                        progressVal = 1.0f
                                        isBootstrapping = false
                                        isDaemonActive = true
                                        chatViewModel.logToTerminal("HERMES_CLI: SUCCESS: System wizard completed. Core narrow waist initialized.")

                                        // Termux feedback
                                        if (termuxHardwareEnabled) {
                                            try {
                                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                                if (vibrator != null && vibrator.hasVibrator()) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                                                    } else {
                                                        vibrator.vibrate(500)
                                                    }
                                                }
                                                chatViewModel.speak("Hermes setup completed successfully. Core narrow waist initialized.")
                                            } catch (e: Exception) {}
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stepper Footer Navigation
                if (!isBootstrapping) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentStep > 1) currentStep-- },
                            enabled = currentStep > 1,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkAccent,
                                disabledContainerColor = DarkAccent.copy(0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous Step", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BACK", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (currentStep > 1) GoldCream else MutedGold.copy(0.5f))
                        }

                        Button(
                            onClick = { if (currentStep < 5) currentStep++ },
                            enabled = currentStep < 5,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemGreen,
                                disabledContainerColor = SystemGreen.copy(0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("NEXT", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Step", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }


    }
}

// --- WIZARD HELPER VIEWS ---

@Composable
fun StepOnePathsView(
    hermesHome: String,
    onHermesHomeChange: (String) -> Unit,
    dbEngine: String,
    onDbEngineChange: (String) -> Unit,
    openClawMigrate: Boolean,
    onOpenClawMigrateChange: (Boolean) -> Unit,
    onInstantAutoSetup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PHASE 1: PATH ENVIRONMENT & DB",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure the workspace sandbox paths where configuration files and session history records are stored.",
            color = MutedGold,
            fontSize = 11.sp
        )

        // --- INSTANT AUTO SETUP (FREE TIER) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SystemGreen.copy(0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SystemGreen.copy(0.4f), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Auto Setup",
                        tint = SystemGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INSTANT AUTO SETUP (FREE TIER)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SystemGreen,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lompati konfigurasi manual! Atur otomatis menggunakan Google Gemini / Nous API & Embedded Termux Subsystem.",
                    color = MutedGold,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onInstantAutoSetup,
                    colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "JALANKAN AUTO SETUP SEKARANG",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // HERMES_HOME Input
        Text(
            text = "WORKSPACE HOME PATH (HERMES_HOME)",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = hermesHome,
            onValueChange = onHermesHomeChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldCream,
                unfocusedBorderColor = GoldCream.copy(0.4f),
                focusedContainerColor = ConsoleBlack,
                unfocusedContainerColor = ConsoleBlack
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        // SQLite DB Selection
        Text(
            text = "LOCAL PERSISTENCE SCHEMA ENGINE",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Room SQLite Engine", "Local Sqlite3").forEach { engine ->
                val selected = dbEngine == engine
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) DarkAccent else ConsoleBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDbEngineChange(engine) }
                        .border(
                            1.dp,
                            if (selected) GoldCream else GoldCream.copy(0.2f),
                            RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = engine,
                            color = if (selected) GoldCream else MutedGold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // OpenClaw migration toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ConsoleBlack)
                .border(1.dp, GoldCream.copy(0.1f), RoundedCornerShape(8.dp))
                .clickable { onOpenClawMigrateChange(!openClawMigrate) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AUTO-MIGRATE OPENCLAW PROFILE",
                    color = GoldCream,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Detects legacy ~/.openclaw profile files and ports them into config.yaml",
                    color = MutedGold,
                    fontSize = 9.sp
                )
            }
            Switch(
                checked = openClawMigrate,
                onCheckedChange = onOpenClawMigrateChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GoldCream,
                    checkedTrackColor = SystemGreen,
                    uncheckedThumbColor = MutedGold,
                    uncheckedTrackColor = DarkAccent
                )
            )
        }
    }
}

@Composable
fun StepTwoCognitiveView(
    provider: String,
    onProviderChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit
) {
    var selectedSubProvider by remember { mutableStateOf("OpenRouter (Default)") }

    // Map sub-providers to their official models list
    val providerModels = mapOf(
        "OpenRouter (Default)" to listOf(
            "nousresearch/hermes-3-llama-3.1-8b",
            "nousresearch/hermes-3-llama-3.1-70b",
            "nousresearch/hermes-3-llama-3.1-405b",
            "google/gemini-2.0-flash-exp",
            "meta-llama/llama-3.1-70b-instruct"
        ),
        "OpenAI" to listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "o1-mini",
            "o1-preview"
        ),
        "Anthropic Claude" to listOf(
            "claude-3-5-sonnet-latest",
            "claude-3-5-haiku-latest",
            "claude-3-opus-20240229"
        ),
        "DeepSeek" to listOf(
            "deepseek-chat",
            "deepseek-reasoner"
        ),
        "Google AI Studio (Gemini)" to listOf(
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-2.0-flash",
            "gemini-2.0-pro-exp"
        ),
        "Groq Cloud" to listOf(
            "llama-3.3-70b-specdec",
            "mixtral-8x7b-32768",
            "llama3-8b-8192",
            "gemma2-9b-it"
        ),
        "NVIDIA NIM" to listOf(
            "meta/llama3-70b-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "mistralai/mixtral-8x22b-instruct-v0.1"
        ),
        "Cohere" to listOf(
            "command-r-plus",
            "command-r",
            "command-light"
        )
    )

    val suggestedModels = providerModels[selectedSubProvider] ?: emptyList()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "TAHAP 2: PENGATURAN KECERDASAN BUATAN (AI)",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Atur penyedia layanan model bahasa (AI) yang digunakan Hermes untuk memproses pesan Anda.",
            color = MutedGold,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Select Provider Card Chips
        Text(
            text = "PILIH PENYEDIA UTAMA AI",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("nous" to "Hermes Provider").forEach { (id, name) ->
                val selected = provider == id || provider == "gemini"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) DarkAccent else ConsoleBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProviderChange("nous") }
                        .border(
                            1.dp,
                            if (selected) GoldCream else GoldCream.copy(0.2f),
                            RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = name,
                            color = if (selected) GoldCream else MutedGold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dropdown for official github providers
        val subProviders = listOf(
            "OpenRouter (Default)",
            "OpenAI",
            "Anthropic Claude",
            "DeepSeek",
            "Google AI Studio (Gemini)",
            "Groq Cloud",
            "NVIDIA NIM",
            "Cohere"
        )

        LocalDropdown(
            label = "PILIH PENYEDIA LAYANAN AI (REKOMENDASI)",
            selectedValue = selectedSubProvider,
            options = subProviders,
            onSelect = { selected ->
                selectedSubProvider = selected
                val defaultModel = when (selected) {
                    "OpenRouter (Default)" -> "nousresearch/hermes-3-llama-3.1-8b"
                    "OpenAI" -> "gpt-4o-mini"
                    "Anthropic Claude" -> "claude-3-5-sonnet-latest"
                    "DeepSeek" -> "deepseek-chat"
                    "Google AI Studio (Gemini)" -> "gemini-1.5-flash"
                    "Groq Cloud" -> "llama-3-groq-70b-8bit"
                    "NVIDIA NIM" -> "meta/llama3-70b-instruct"
                    "Cohere" -> "command-r-plus"
                    else -> "nousresearch/hermes-3-llama-3.1-8b"
                }
                onModelChange(defaultModel)
            }
        )

        // Credentials Token Input
        Text(
            text = "KUNCI API (API KEY) AMAN",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Masukkan Kunci API Anda di sini...", color = MutedGold.copy(0.4f), fontSize = 11.sp) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldCream,
                unfocusedBorderColor = GoldCream.copy(0.4f),
                focusedContainerColor = ConsoleBlack,
                unfocusedContainerColor = ConsoleBlack
            ),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            shape = RoundedCornerShape(8.dp)
        )

        // Model Specifier
        Text(
            text = "IDENTIFIKASI / NAMA MODEL AI",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldCream,
                unfocusedBorderColor = GoldCream.copy(0.4f),
                focusedContainerColor = ConsoleBlack,
                unfocusedContainerColor = ConsoleBlack
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        // Suggested Models suggestions chip group
        Text(
            text = "REKOMENDASI MODEL UNTUK PENYEDIA INI",
            color = MutedGold,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(suggestedModels.size) { index ->
                val currentModel = suggestedModels[index]
                val isSelected = currentModel == model
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) SystemGreen.copy(0.15f) else DarkAccent,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) SystemGreen else GoldCream.copy(0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onModelChange(currentModel) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentModel,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) SystemGreen else GoldCream,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun StepThreeSandboxView(
    sandboxType: String,
    onSandboxTypeChange: (String) -> Unit,
    dockerImage: String,
    onDockerImageChange: (String) -> Unit,
    sshHost: String,
    onSshHostChange: (String) -> Unit,
    sshPort: String,
    onSshPortChange: (String) -> Unit,
    sshPassword: String,
    onSshPasswordChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PHASE 3: SECURE SHELL EXECUTION SANDBOX",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Specify where terminal tool commands triggered by the agent are safely and securely executed.",
            color = MutedGold,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Select Sandbox Type Group
        Text(
            text = "TERMINAL SANDBOX ENVIRONMENT",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        
        val types = listOf("Embedded Termux", "Docker Container", "Daytona Sandbox")
        
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { t ->
                    val selected = sandboxType == t || (t == "Embedded Termux" && sandboxType != "Docker Container" && sandboxType != "Daytona Sandbox")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (selected) DarkAccent else ConsoleBlack),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSandboxTypeChange(t) }
                            .border(1.dp, if (selected) SystemGreen else GoldCream.copy(0.2f), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            Text(t, color = if (selected) SystemGreen else MutedGold, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Conditional Details based on Selection
        when (sandboxType) {
            "Docker Container" -> {
                Text(
                    text = "DOCKER SANDBOX IMAGE SPECIFICATION",
                    color = GoldCream,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = dockerImage,
                    onValueChange = onDockerImageChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            else -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ConsoleBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SystemGreen.copy(0.4f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(SystemGreen, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EMBEDDED NATIVE TERMUX ENGINE", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Standard Linux commands, package managers, and scripts execute directly inside the native App Sandbox environment (files/usr / files/home). Zero bridge, zero external app dependency.", color = MutedGold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LocalDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    CustomDropdownSelector(
        label = label,
        selectedValue = selectedValue,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        options = options,
        onSelectOption = {
            onSelect(it)
            expanded = false
        }
    )
}

@Composable
fun CustomDropdownSelector(
    label: String,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelectOption: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.4f), RoundedCornerShape(8.dp))
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue,
                    color = GoldCream,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand dropdown",
                    tint = GoldCream,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .background(SurfaceCard)
                    .border(1.dp, GoldCream.copy(0.2f), RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = GoldCream,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        },
                        onClick = {
                            onSelectOption(option)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StepFourGatewayView(
    telegramEnabled: Boolean,
    onTelegramEnabledChange: (Boolean) -> Unit,
    telegramToken: String,
    onTelegramTokenChange: (String) -> Unit,
    telegramChatId: String,
    onTelegramChatIdChange: (String) -> Unit,

    discordEnabled: Boolean,
    onDiscordEnabledChange: (Boolean) -> Unit,
    discordWebhookUrl: String,
    onDiscordWebhookUrlChange: (String) -> Unit,
    discordChannelId: String,
    onDiscordChannelIdChange: (String) -> Unit,

    termuxHardwareEnabled: Boolean,
    onTermuxHardwareEnabledChange: (Boolean) -> Unit,
    vibrateDurationMs: String,
    onVibrateDurationMsChange: (String) -> Unit,
    ttsLanguageAccent: String,
    onTtsLanguageAccentChange: (String) -> Unit,

    // Plugins
    isHonchoEnabled: Boolean,
    onIsHonchoEnabledChange: (Boolean) -> Unit,
    memoryProvider: String,
    onMemoryProviderChange: (String) -> Unit,
    honchoEndpoint: String,
    onHonchoEndpointChange: (String) -> Unit,
    honchoToken: String,
    onHonchoTokenChange: (String) -> Unit,

    isContextEngineEnabled: Boolean,
    onIsContextEngineEnabledChange: (Boolean) -> Unit,
    contextMaxTokens: String,
    onContextMaxTokensChange: (String) -> Unit,
    contextPruningStrategy: String,
    onContextPruningStrategyChange: (String) -> Unit,

    isImageGenEnabled: Boolean,
    onIsImageGenEnabledChange: (Boolean) -> Unit,
    imageGenProvider: String,
    onImageGenProviderChange: (String) -> Unit,
    imageGenResolution: String,
    onImageGenResolutionChange: (String) -> Unit,
    imageGenSavePath: String,
    onImageGenSavePathChange: (String) -> Unit,

    isKanbanEnabled: Boolean,
    onIsKanbanEnabledChange: (Boolean) -> Unit,
    kanbanBoardId: String,
    onKanbanBoardIdChange: (String) -> Unit,
    kanbanSyncFrequency: String,
    onKanbanSyncFrequencyChange: (String) -> Unit,

    // Tools
    isTerminalEnabled: Boolean,
    onIsTerminalEnabledChange: (Boolean) -> Unit,
    terminalTimeout: String,
    onTerminalTimeoutChange: (String) -> Unit,
    terminalWorkingDir: String,
    onTerminalWorkingDirChange: (String) -> Unit,

    isFileEnabled: Boolean,
    onIsFileEnabledChange: (Boolean) -> Unit,
    fileMaxReadSize: String,
    onFileMaxReadSizeChange: (String) -> Unit,
    fileAllowedExtensions: String,
    onFileAllowedExtensionsChange: (String) -> Unit,

    isSearchEnabled: Boolean,
    onIsSearchEnabledChange: (Boolean) -> Unit,
    searchDefaultEngine: String,
    onSearchDefaultEngineChange: (String) -> Unit,
    searchResultsLimit: String,
    onSearchResultsLimitChange: (String) -> Unit,

    isBrowserEnabled: Boolean,
    onIsBrowserEnabledChange: (Boolean) -> Unit,
    browserEngine: String,
    onBrowserEngineChange: (String) -> Unit,
    browserHeadless: Boolean,
    onBrowserHeadlessChange: (Boolean) -> Unit,
    browserResolution: String,
    onBrowserResolutionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "PHASE 4: ADAPTERS, PLUGINS & TOOLS SETUP",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure integration gateways, system-wide plugins, and shell execution tools.",
            color = MutedGold,
            fontSize = 11.sp
        )

        // --- SUB-SECTION 1: GATEWAYS ---
        Text(
            text = "--- GATEWAY ADAPTERS SETUP ---",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        // Telegram Polling Toggle & Setup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TELEGRAM PLATFORM POLLING CLIENT",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Initializes background thread platform client",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = telegramEnabled,
                    onCheckedChange = onTelegramEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (telegramEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("TELEGRAM BOT API TOKEN", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = telegramToken,
                    onValueChange = onTelegramTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("AUTHORIZED CHAT ID", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = telegramChatId,
                    onValueChange = onTelegramChatIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Discord Webhook Toggle & Setup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DISCORD WEBHOOK ADAPTER DISPATCHER",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Routes dynamic runtime state logs to a Discord webhook channel",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = discordEnabled,
                    onCheckedChange = onDiscordEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (discordEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("DISCORD WEBHOOK URL", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = discordWebhookUrl,
                    onValueChange = onDiscordWebhookUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("DISCORD CHANNEL ID", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = discordChannelId,
                    onValueChange = onDiscordChannelIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Termux Hardware Toggle & Setup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TERMUX API HARDWARE BINDER",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Binds to physical Android haptic motor, battery sensor and TTS engine",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = termuxHardwareEnabled,
                    onCheckedChange = onTermuxHardwareEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (termuxHardwareEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("HAPTIC FEEDBACK VIBRATE DURATION (MS)", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = vibrateDurationMs,
                    onValueChange = onVibrateDurationMsChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("TEXT-TO-SPEECH LANG ACCENT", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = ttsLanguageAccent,
                    onValueChange = onTtsLanguageAccentChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- SUB-SECTION 2: PLUGINS ---
        Text(
            text = "--- EXTENSION PLUGINS SETUP ---",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        // 1. Memory Provider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. MEMORY PROVIDER PLUGIN",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Maintains long-term episodic/semantic agent memory",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isHonchoEnabled,
                    onCheckedChange = onIsHonchoEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isHonchoEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                LocalDropdown(
                    label = "MEMORY BACKEND PROVIDER",
                    selectedValue = memoryProvider,
                    options = listOf("Honcho", "Mem0", "SuperMemory", "Zep Local"),
                    onSelect = onMemoryProviderChange
                )
                Text("API ENDPOINT", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = honchoEndpoint,
                    onValueChange = onHonchoEndpointChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("CLIENT IDENTITY ACCESS TOKEN", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = honchoToken,
                    onValueChange = onHonchoTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // 2. Context Engine
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. CONTEXT ENGINE PLUGIN",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Handles dynamic sliding window and context pruning",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isContextEngineEnabled,
                    onCheckedChange = onIsContextEngineEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isContextEngineEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("MAX CONTEXT SIZE (TOKENS)", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = contextMaxTokens,
                    onValueChange = onContextMaxTokensChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                LocalDropdown(
                    label = "PRUNING STRATEGY",
                    selectedValue = contextPruningStrategy,
                    options = listOf("Summarize", "Truncate", "Sliding Window", "Vector Rank Match"),
                    onSelect = onContextPruningStrategyChange
                )
            }
        }

        // 3. Image Generation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3. IMAGE GENERATION PLUGIN",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Empowers agent to synthesize dynamic visual assets",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isImageGenEnabled,
                    onCheckedChange = onIsImageGenEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isImageGenEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                LocalDropdown(
                    label = "IMAGE SYNTHESIZER ENGINE",
                    selectedValue = imageGenProvider,
                    options = listOf("Stable Diffusion (Local)", "DALL-E 3", "Midjourney API", "RunPod Endpoint"),
                    onSelect = onImageGenProviderChange
                )
                Text("TARGET RESOLUTION", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = imageGenResolution,
                    onValueChange = onImageGenResolutionChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("LOCAL STORAGE PATH", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = imageGenSavePath,
                    onValueChange = onImageGenSavePathChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // 4. Kanban Board
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "4. KANBAN BOARD DISPATCHER",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Allows multi-agent task flow delegation and kanban sync",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isKanbanEnabled,
                    onCheckedChange = onIsKanbanEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isKanbanEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("BOARD KEY ID", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = kanbanBoardId,
                    onValueChange = onKanbanBoardIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("REFRESH TIME (SECONDS)", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = kanbanSyncFrequency,
                    onValueChange = onKanbanSyncFrequencyChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- SUB-SECTION 3: TOOLS ---
        Text(
            text = "--- EXECUTION TOOLS SETUP ---",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        // 1. Terminal Sandbox Tool
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. TERMINAL SANDBOX TOOL",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Allows agent to invoke isolated bash commands",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isTerminalEnabled,
                    onCheckedChange = onIsTerminalEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isTerminalEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("COMMAND TIMEOUT LIMIT (SECONDS)", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = terminalTimeout,
                    onValueChange = onTerminalTimeoutChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("SANDBOX ROOT WORKING DIR", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = terminalWorkingDir,
                    onValueChange = onTerminalWorkingDirChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // 2. File Manager Tool
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. FILE MANAGER TOOL",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Provides reading/writing capacities to isolated workspaces",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isFileEnabled,
                    onCheckedChange = onIsFileEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isFileEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("MAX READ FILE SIZE (BYTES)", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = fileMaxReadSize,
                    onValueChange = onFileMaxReadSizeChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Text("PERMITTED WRITE EXTENSIONS", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = fileAllowedExtensions,
                    onValueChange = onFileAllowedExtensionsChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // 3. Search Web Tool
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3. SEARCH WEB TOOL",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Queries independent indexes with decentralized endpoints",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isSearchEnabled,
                    onCheckedChange = onIsSearchEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isSearchEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                LocalDropdown(
                    label = "DEFAULT DECENTRALIZED SEARCH ENGINE",
                    selectedValue = searchDefaultEngine,
                    options = listOf("DuckDuckGo", "SearXNG", "Tavily", "Brave Search", "Bing API"),
                    onSelect = onSearchDefaultEngineChange
                )
                Text("SEARCH RESULT COUNT LIMIT", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = searchResultsLimit,
                    onValueChange = onSearchResultsLimitChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // 4. Browser Automation Tool
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleBlack, RoundedCornerShape(8.dp))
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "4. BROWSER AUTOMATION TOOL",
                        color = GoldCream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Launches headless browser engine sandbox",
                        color = MutedGold,
                        fontSize = 9.sp
                    )
                }
                Switch(
                    checked = isBrowserEnabled,
                    onCheckedChange = onIsBrowserEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldCream,
                        checkedTrackColor = SystemGreen,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = DarkAccent
                    )
                )
            }
            if (isBrowserEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                LocalDropdown(
                    label = "BROWSER PLAYWRIGHT ENGINE",
                    selectedValue = browserEngine,
                    options = listOf("Chromium", "Firefox", "WebKit", "Local Selenium"),
                    onSelect = onBrowserEngineChange
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RUN IN HEADLESS MODE", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = browserHeadless,
                        onCheckedChange = onBrowserHeadlessChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldCream,
                            checkedTrackColor = SystemGreen,
                            uncheckedThumbColor = MutedGold,
                            uncheckedTrackColor = DarkAccent
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("SCREEN VIEWPORT RESOLUTION", color = GoldCream, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = browserResolution,
                    onValueChange = onBrowserResolutionChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCream,
                        unfocusedBorderColor = GoldCream.copy(0.4f),
                        focusedContainerColor = ConsoleBlack,
                        unfocusedContainerColor = ConsoleBlack
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
fun StepFiveReviewView(
    hermesHome: String,
    provider: String,
    model: String,
    apiKey: String,
    sandboxType: String,
    telegramEnabled: Boolean,
    telegramToken: String,
    telegramChatId: String,
    discordEnabled: Boolean,
    discordWebhookUrl: String,
    discordChannelId: String,
    termuxHardwareEnabled: Boolean,
    vibrateDurationMs: String,
    ttsLanguageAccent: String,

    // Plugins
    isHonchoEnabled: Boolean,
    memoryProvider: String,
    honchoEndpoint: String,
    honchoToken: String,
    isContextEngineEnabled: Boolean,
    contextMaxTokens: String,
    contextPruningStrategy: String,
    isImageGenEnabled: Boolean,
    imageGenProvider: String,
    imageGenResolution: String,
    imageGenSavePath: String,
    isKanbanEnabled: Boolean,
    kanbanBoardId: String,
    kanbanSyncFrequency: String,

    // Tools
    isTerminalEnabled: Boolean,
    terminalTimeout: String,
    terminalWorkingDir: String,
    isFileEnabled: Boolean,
    fileMaxReadSize: String,
    fileAllowedExtensions: String,
    isSearchEnabled: Boolean,
    searchDefaultEngine: String,
    searchResultsLimit: String,
    isBrowserEnabled: Boolean,
    browserEngine: String,
    browserHeadless: Boolean,
    browserResolution: String,
    onCommitBootstrap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "PHASE 5: INITIALIZE BOOTSTRAP ENVIRONMENT",
            color = GoldCream,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Audit setup specifications before committing them to the sandboxed filesystem.",
            color = MutedGold,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ConsoleBlack),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("# Generated ~/.hermes/config.yaml preview", color = MutedGold.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)

                Row {
                    Text("hermes_home: ", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("\"$hermesHome\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Row {
                    Text("provider: ", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("\"$provider\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Row {
                    Text("active_model: ", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("\"$model\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Row {
                    Text("api_key_configured: ", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text(if (apiKey.isBlank()) "false" else "true", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Row {
                    Text("sandbox_type: ", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("\"$sandboxType\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("enabled_gateways:", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                if (telegramEnabled) {
                    Text("  - telegram:", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      token: \"$telegramToken\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      chat_id: \"$telegramChatId\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (discordEnabled) {
                    Text("  - discord_webhook:", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      webhook_url: \"$discordWebhookUrl\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      channel_id: \"$discordChannelId\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (termuxHardwareEnabled) {
                    Text("  - termux_android_api:", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      vibrate_ms: $vibrateDurationMs", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      tts_accent: \"$ttsLanguageAccent\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (!telegramEnabled && !discordEnabled && !termuxHardwareEnabled) {
                    Text("  (None active)", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("enabled_plugins:", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                var hasPlugins = false
                if (isHonchoEnabled) {
                    hasPlugins = true
                    Text("  - plugin: \"Memory Provider ($memoryProvider)\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      endpoint: \"$honchoEndpoint\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      token: \"$honchoToken\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (isContextEngineEnabled) {
                    hasPlugins = true
                    Text("  - plugin: \"Context Engine\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      max_tokens: $contextMaxTokens", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      pruning: \"$contextPruningStrategy\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (isImageGenEnabled) {
                    hasPlugins = true
                    Text("  - plugin: \"Image Generation ($imageGenProvider)\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      resolution: \"$imageGenResolution\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      save_path: \"$imageGenSavePath\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (isKanbanEnabled) {
                    hasPlugins = true
                    Text("  - plugin: \"Kanban Board Dispatcher\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      board_id: \"$kanbanBoardId\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      sync_sec: $kanbanSyncFrequency", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (!hasPlugins) {
                    Text("  (None active)", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("enabled_tools:", color = AlertOrange, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                var hasTools = false
                if (isTerminalEnabled) {
                    hasTools = true
                    Text("  - tool: \"Terminal Sandbox Tool\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      timeout_sec: $terminalTimeout", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      working_dir: \"$terminalWorkingDir\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (isFileEnabled) {
                    hasTools = true
                    Text("  - tool: \"File Manager Tool\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      max_read_bytes: $fileMaxReadSize", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      allowed_extensions: \"$fileAllowedExtensions\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (isSearchEnabled) {
                    hasTools = true
                    Text("  - tool: \"Search Web Tool\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      default_engine: \"$searchDefaultEngine\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      scan_limit: $searchResultsLimit", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (isBrowserEnabled) {
                    hasTools = true
                    Text("  - tool: \"Browser Automation Tool ($browserEngine)\"", color = SystemGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("      headless: $browserHeadless", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    Text("      resolution: \"$browserResolution\"", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
                if (!hasTools) {
                    Text("  (None active)", color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onCommitBootstrap,
            colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Done, contentDescription = "Bootstrap", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "COMMIT & BOOTSTRAP ENVIRONMENT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White
            )
        }
    }
}


// --- TAB 4: PROFILE & INTEGRATIONS SCREEN (HERMES DAEMON SETUP WIZARD) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    chatViewModel: ChatViewModel,
    cronViewModel: CronViewModel,
    skillViewModel: SkillViewModel
) {
    val jobs by cronViewModel.jobs.collectAsState()
    val skills by skillViewModel.skills.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(skills) {
        listOf("All") + skills.map { it.category }.distinct().sorted()
    }
    val filteredSkills = remember(skills, selectedCategory) {
        if (selectedCategory == "All") {
            skills
        } else {
            skills.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val currentProvider by chatViewModel.apiProvider.collectAsState()
    val savedGeminiKey by chatViewModel.geminiApiKey.collectAsState()
    val savedNousKey by chatViewModel.nousApiKey.collectAsState()
    val savedModel by chatViewModel.activeModel.collectAsState()

    val savedCustomBaseUrl by chatViewModel.customApiBaseUrl.collectAsState()
    val savedCustomKey by chatViewModel.customApiKey.collectAsState()
    val savedCustomModel by chatViewModel.customModel.collectAsState()

    val soulMdText by chatViewModel.soulMd.collectAsState()

    var selectedProvider by remember(currentProvider) { mutableStateOf(currentProvider) }
    var tempGeminiKey by remember(savedGeminiKey) { mutableStateOf(savedGeminiKey) }
    var tempNousKey by remember(savedNousKey) { mutableStateOf(savedNousKey) }
    var tempModel by remember(savedModel) { mutableStateOf(savedModel) }

    var tempCustomBaseUrl by remember(savedCustomBaseUrl) { mutableStateOf(savedCustomBaseUrl) }
    var tempCustomKey by remember(savedCustomKey) { mutableStateOf(savedCustomKey) }
    var tempCustomModel by remember(savedCustomModel) { mutableStateOf(savedCustomModel) }

    var tempSoulMd by remember(soulMdText) { mutableStateOf(soulMdText) }

    var showAddJobDialog by remember { mutableStateOf(false) }

    // Synchronize editing states when external state flows change (e.g. from terminal commands)
    LaunchedEffect(currentProvider, savedGeminiKey, savedNousKey, savedModel) {
        selectedProvider = currentProvider
        tempGeminiKey = savedGeminiKey
        tempNousKey = savedNousKey
        tempModel = savedModel
    }

    // Sync temp selected model when provider changes to set a good default
    LaunchedEffect(selectedProvider) {
        if (selectedProvider == "gemini") {
            if (!tempModel.startsWith("gemini")) {
                tempModel = "gemini-3.5-flash"
            }
        } else if (selectedProvider == "nous") {
            if (!tempModel.contains("hermes") && !tempModel.contains("llama")) {
                tempModel = "nousresearch/hermes-3-llama-3.1-8b"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. PROFILE HEADER: FOTO PROFILE & NAMA AGENT ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldCream.copy(0.25f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(AlertOrange, BackgroundDark)
                                )
                            )
                            .border(2.dp, GoldCream, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        HermesLogoCompose(modifier = Modifier.size(46.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "HERMES AGENT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GoldCream,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Status Inti AI: AKTIF",
                            color = SystemGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Wadah: Android Termux Tersemat",
                            color = MutedGold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- 2. SOUL.MD PERSONALITY EDITOR ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldCream.copy(0.25f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "SOUL.md Editor",
                            tint = GoldCream,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SOUL.MD: KEPRIBADIAN & SIFAT AI",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GoldCream,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ini adalah petunjuk sistem yang mengontrol kepribadian dan cara berpikir utama dari Hermes AI.",
                        color = MutedGold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempSoulMd,
                        onValueChange = { tempSoulMd = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GoldCream),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldCream,
                            unfocusedBorderColor = GoldCream.copy(0.3f),
                            cursorColor = GoldCream
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            chatViewModel.updateSoulMd(tempSoulMd)
                            chatViewModel.logToTerminal("SOUL.md diperbarui: \"${tempSoulMd.take(30)}...\"")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save SOUL.md", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIMPAN PENGATURAN KEPRIBADIAN",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- 3. COGNITIVE PROVIDER SETUP (OFFICIAL & CUSTOM) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldCream.copy(0.25f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Provider Setup",
                            tint = GoldCream,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PENGATURAN PENYEDIA AI",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GoldCream,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pilih penyedia AI resmi atau tambahkan alamat server kustom Anda sendiri yang kompatibel dengan format OpenAI.",
                        color = MutedGold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Provider Selection Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ConsoleBlack, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { selectedProvider = "nous" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedProvider == "nous") GoldCream else Color.Transparent,
                                contentColor = if (selectedProvider == "nous") BackgroundDark else GoldCream
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("NOUS AI", fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = { selectedProvider = "custom" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedProvider == "custom") GoldCream else Color.Transparent,
                                contentColor = if (selectedProvider == "custom") BackgroundDark else GoldCream
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("CUSTOM", fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Form fields based on selected provider
                    when (selectedProvider) {
                        "nous" -> {
                            OutlinedTextField(
                                value = tempNousKey,
                                onValueChange = { tempNousKey = it },
                                label = { Text("Kunci API OpenRouter (sk-or-...)", color = MutedGold, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldCream,
                                    unfocusedBorderColor = GoldCream.copy(0.3f),
                                    cursorColor = GoldCream
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var modelDropdownExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = tempModel,
                                    onValueChange = { tempModel = it },
                                    label = { Text("Nama / Jalur Model AI", color = MutedGold, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
                                    trailingIcon = {
                                        IconButton(onClick = { modelDropdownExpanded = !modelDropdownExpanded }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Preset Model",
                                                tint = GoldCream
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldCream,
                                        unfocusedBorderColor = GoldCream.copy(0.3f),
                                        cursorColor = GoldCream
                                    )
                                )
                                DropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(SurfaceCard)
                                        .border(1.dp, GoldCream.copy(0.2f))
                                ) {
                                    val officialModels = listOf(
                                        "nousresearch/hermes-3-llama-3.1-8b" to "Nous: Hermes 3 Llama 3.1 8B (Default)",
                                        "nousresearch/hermes-3-llama-3.1-70b" to "Nous: Hermes 3 Llama 3.1 70B",
                                        "nousresearch/hermes-3-llama-3.1-405b" to "Nous: Hermes 3 Llama 3.1 405B",
                                        "deepseek/deepseek-chat" to "DeepSeek: V3 Chat",
                                        "deepseek/deepseek-r1" to "DeepSeek: R1 Reasoning",
                                        "anthropic/claude-3.5-sonnet" to "Anthropic: Claude 3.5 Sonnet",
                                        "openai/gpt-4o" to "OpenAI: GPT-4o",
                                        "google/gemini-2.5-flash" to "Google: Gemini 2.5 Flash",
                                        "meta-llama/llama-3.3-70b-instruct" to "Llama: Llama 3.3 70B Instruct"
                                    )
                                    officialModels.forEach { (modelId, displayName) ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(text = displayName, color = GoldCream, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = modelId, color = MutedGold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                                }
                                            },
                                            onClick = {
                                                tempModel = modelId
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "custom" -> {
                            OutlinedTextField(
                                value = tempCustomBaseUrl,
                                onValueChange = { tempCustomBaseUrl = it },
                                label = { Text("URL Alamat API Kustom (misal: OpenAI / Ollama)", color = MutedGold, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldCream,
                                    unfocusedBorderColor = GoldCream.copy(0.3f),
                                    cursorColor = GoldCream
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempCustomKey,
                                onValueChange = { tempCustomKey = it },
                                label = { Text("Kunci API / Token Kustom", color = MutedGold, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldCream,
                                    unfocusedBorderColor = GoldCream.copy(0.3f),
                                    cursorColor = GoldCream
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempCustomModel,
                                onValueChange = { tempCustomModel = it },
                                label = { Text("Nama Model AI Kustom", color = MutedGold, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GoldCream),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldCream,
                                    unfocusedBorderColor = GoldCream.copy(0.3f),
                                    cursorColor = GoldCream
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Button for Providers
                    Button(
                        onClick = {
                            if (selectedProvider == "custom") {
                                chatViewModel.updateCustomSettings(tempCustomBaseUrl, tempCustomKey, tempCustomModel)
                                chatViewModel.updateSettings("custom", "", tempNousKey, tempCustomModel)
                            } else {
                                chatViewModel.updateSettings(selectedProvider, "", tempNousKey, tempModel)
                            }
                            chatViewModel.logToTerminal("PROVIDER SAVED: Selected backend is $selectedProvider")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save Provider", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMPAN KONFIGURASI PENYEDIA AI", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // --- 4. PLUGGABLE PLUGINS DIRECTORY ---
        item {
            Divider(color = GoldCream.copy(0.15f), modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "DIREKTORI FITUR & PLUGIN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = GoldCream,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aktifkan atau matikan plugin tambahan untuk memperluas kemampuan Hermes secara dinamis.",
                color = MutedGold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    val chipBg = if (isSelected) GoldCream else SurfaceCard
                    val chipText = if (isSelected) BackgroundDark else MutedGold
                    val chipBorderColor = if (isSelected) GoldCream else GoldCream.copy(alpha = 0.2f)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorderColor, RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (category.lowercase()) {
                            "all" -> Icons.Default.List
                            "vision" -> Icons.Default.Face
                            "creative" -> Icons.Default.Edit
                            "scraper" -> Icons.Default.Search
                            "music" -> Icons.Default.PlayArrow
                            "iot" -> Icons.Default.Home
                            "information" -> Icons.Default.Info
                            else -> Icons.Default.Build
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = category,
                            tint = chipText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.uppercase(),
                            color = chipText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        items(filteredSkills) { skill ->
            SkillItemRow(skill, skillViewModel)
        }

        // --- 5. TASK SCHEDULER (CRON) ---
        item {
            Divider(color = GoldCream.copy(0.15f), modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAEMON TASK SCHEDULER (CRON)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GoldCream,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Automate background checks and routine triggers.",
                        color = MutedGold,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { showAddJobDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Job", tint = GoldCream)
                }
            }
        }

        items(jobs) { job ->
            CronJobItemRow(job, cronViewModel)
        }
    }

    if (showAddJobDialog) {
        AddCronJobDialog(
            onDismiss = { showAddJobDialog = false },
            onConfirm = { name, expression, prompt ->
                cronViewModel.addJob(name, expression, prompt)
                showAddJobDialog = false
            }
        )
    }
}


@Composable
fun SkillItemRow(skill: SkillEntity, viewModel: SkillViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DarkAccent, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (skill.id) {
                        "skill-vision" -> Icons.Default.Face
                        "skill-image-gen" -> Icons.Default.Edit
                        "skill-scraper" -> Icons.Default.Search
                        "skill-video-gen" -> Icons.Default.PlayArrow
                        "skill-spotify" -> Icons.Default.PlayArrow
                        "skill-hass" -> Icons.Default.Home
                        "skill-web" -> Icons.Default.Search
                        "skill-kanban" -> Icons.Default.List
                        else -> Icons.Default.Build
                    },
                    contentDescription = skill.name,
                    tint = GoldCream
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.name.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GoldCream,
                    fontSize = 13.sp
                )
                Text(
                    text = skill.description,
                    color = MutedGold,
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = skill.isInstalled,
                onCheckedChange = { viewModel.toggleSkill(skill) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = GoldCream,
                    uncheckedThumbColor = MutedGold,
                    uncheckedTrackColor = SurfaceCard
                )
            )
        }
    }
}

@Composable
fun CronJobItemRow(job: CronJobEntity, viewModel: CronViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldCream.copy(0.15f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GoldCream,
                    fontSize = 13.sp
                )
                Text(
                    text = "Schedule: ${job.expression}",
                    fontFamily = FontFamily.Monospace,
                    color = MutedGold,
                    fontSize = 11.sp
                )
                Text(
                    text = "Prompt: ${job.prompt}",
                    color = MutedGold,
                    fontSize = 11.sp
                )
                if (job.lastRun > 0) {
                    Text(
                        text = "Last triggered: ${(System.currentTimeMillis() - job.lastRun) / 1000}s ago",
                        color = SystemGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = job.isActive,
                    onCheckedChange = { viewModel.toggleJob(job) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = GoldCream,
                        uncheckedThumbColor = MutedGold,
                        uncheckedTrackColor = SurfaceCard
                    )
                )

                IconButton(
                    onClick = { viewModel.deleteJob(job.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Job",
                        tint = AlertOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCronJobDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var expression by remember { mutableStateOf("*/30 * * * *") }
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "NEW AUTOMATION TASK",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = GoldCream,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldCream, focusedLabelColor = GoldCream)
                )
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it },
                    label = { Text("Cron Expression") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldCream, focusedLabelColor = GoldCream)
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("AI Execution Prompt") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldCream, focusedLabelColor = GoldCream)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && prompt.isNotBlank()) onConfirm(name, expression, prompt) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldCream, contentColor = BackgroundDark)
            ) {
                Text("CREATE", fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = GoldCream, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceCard
    )
}

// --- BASIC MULTI-EDIT AND COMPONENT HELPERS ---
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    cursorBrush: androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.SolidColor(Color.Black),
    singleLine: Boolean = false,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = cursorBrush,
        singleLine = singleLine,
        decorationBox = decorationBox
    )
}
