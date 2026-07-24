# Refactor Plan: ChatViewModel God Class Decomposition

**Analysis Source:** Graphify graph analysis (115 nodes, 217 edges, 12 communities)
**Target:** `ChatViewModel` — 30 edges, spans 6 communities, betweenness centrality 0.416

---

## Current State (Problem)

`ChatViewModel` (ViewModels.kt:L49-L579) — **530 lines**, handles:
- Chat messaging (Gemini, Nous, Custom providers)
- Terminal session management (multi-session, logs, font size)
- Settings persistence (SharedPreferences — 25+ keys)
- TTS (TextToSpeech)
- Slash command parsing (/help, /model, /provider, /key, /clear)
- Workspace file attachment injection
- Cron/Skill/Pet ViewModel references passed to Terminal

**Spans 6 communities** with 30 edges — highest betweenness (0.416).

---

## Target Architecture

### 1. `ChatViewModel` → **Chat only** (~150 lines)
- `sendMessage()` — Gemini/Nous/Custom provider calls
- `messages` StateFlow + `isSending`
- `clearHistory()`
- Provider/model/key settings (delegated to `SettingsViewModel`)

### 2. `TerminalViewModel` (NEW) — **Terminal session management** (~120 lines)
- `terminalSessions`, `activeSessionId`, `terminalLogs`
- `addNewTerminalSession()`, `removeTerminalSession()`, `clearAllTerminalSessions()`
- `logToTerminal()`, `logToActiveTerminal()`, `setActiveTerminalDir()`
- `terminalFontSize` + inc/dec/reset
- `processCliCommand()` — slash command parsing

### 3. `SettingsViewModel` (NEW) — **All SharedPreferences** (~100 lines)
- API provider/model/keys (Gemini, Nous, Custom)
- Sandbox config (type, docker image, SSH)
- Gateway platforms (Telegram, Discord, Termux)
- Gateway fields (tokens, chat IDs, webhooks)
- Soul.md personality
- Custom provider (base URL, key, model)
- Terminal font size

### 4. `PetViewModel` — **Unchanged** (already separate)

### 5. `CronViewModel` — **Unchanged** (already separate)

### 5. `SkillViewModel` — **Unchanged** (already separate)

---

## Migration Steps

### Phase 1: Extract `TerminalViewModel`
1. Create `TerminalViewModel.kt` with:
   - All `terminalSessions`, `activeSessionId`, `terminalLogs` state
   - `TerminalSession` data class
   - `logToTerminal`, `logToActiveTerminal`, `addNewTerminalSession`, `removeTerminalSession`, `clearAllTerminalSessions`
   - `terminalFontSize` + controls
   - `processCliCommand()` — move slash command logic here
2. Update `ChatViewModel`:
   - Remove terminal fields/methods
   - Inject `TerminalViewModel` via `viewModel()` in Composable
   - Delegate `logToTerminal()` → `terminalViewModel.logToTerminal()`

### Phase 2: Extract `SettingsViewModel`
1. Create `SettingsViewModel.kt` with:
   - All SharedPreferences keys as `MutableStateFlow`
   - `updateSettings()`, `updateCustomSettings()`, `updateSandboxConfig()`, `updateGatewayConfig()`, `updateGatewayFields()`, `updateSoulMd()`, `updateTerminalFontSize()`
2. Update `ChatViewModel`:
   - Remove all `sharedPrefs` + `MutableStateFlow` for settings
   - Inject `SettingsViewModel`
   - Delegate `updateSettings()` → `settingsViewModel.updateSettings()`
   - Read `apiProvider`, `activeModel`, `geminiApiKey`, etc. from `settingsViewModel`

### Phase 3: Update `MainScreen` & Composables
1. `MainScreen()` — obtain all 4 ViewModels:
```kotlin
val chatViewModel: ChatViewModel = viewModel()
val terminalViewModel: TerminalViewModel = viewModel()
val settingsViewModel: SettingsViewModel = viewModel()
val petViewModel: PetViewModel = viewModel()
val cronViewModel: CronViewModel = viewModel()
val skillViewModel: SkillViewModel = viewModel()
```
2. `TuiTerminalScreen(terminalViewModel, petViewModel, cronViewModel, skillViewModel)`
3. `AgentChatScreen(chatViewModel, settingsViewModel)` — pass settings for engine dialog
4. `IntegrationsScreen(settingsViewModel, cronViewModel, skillViewModel)`
5. `DaemonCommandCenterScreen(settingsViewModel, ...)`

### Phase 4: Update `MainActivity.kt` imports & references
- Replace `ChatViewModel.terminalSessions` → `TerminalViewModel.terminalSessions`
- Replace `ChatViewModel.logToTerminal()` → `TerminalViewModel.logToTerminal()`
- Replace `ChatViewModel.updateSettings()` → `SettingsViewModel.updateSettings()`
- Update all Composable previews/tests

### Phase 5: Clean up `ChatViewModel`
- Remove all terminal-related code (~150 lines)
- Remove all SharedPreferences code (~100 lines)
- Keep only: `sendMessage()`, `messages`, `isSending`, `clearHistory()`, provider/model delegation to SettingsViewModel

---

## Graphify Validation (Post-Refactor)

Run graphify again to verify:
```bash
cd /path/to/project
graphify . --update
# Check:
# - ChatViewModel edges reduced from 30 → ~10
# - TerminalViewModel edges ~15 (new)
# - SettingsViewModel edges ~12 (new)
# - Betweenness centrality of ChatViewModel < 0.1
# - No community spans > 2 for any ViewModel
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| State loss on ViewModel recreation | All state in `StateFlow`/`SharedPreferences` — survives recreation |
| Composable recompilation | Minimal — only ViewModel references change |
| Tests | Update test mocks for new ViewModels |
| BuildConfig.GEMINI_API_KEY | Migrate to SettingsViewModel with fallback |

---

## Files to Create/Modify

| File | Action |
|------|--------|
| `ViewModels.kt` | **Split** → `ChatViewModel`, `TerminalViewModel`, `SettingsViewModel` (keep Pet/Cron/Skill) |
| `TerminalViewModel.kt` | **Create** |
| `SettingsViewModel.kt` | **Create** |
| `MainActivity.kt` | Update ViewModel instantiation + composable params |
| `AgentChatScreen` | Add `settingsViewModel` param |
| `TuiTerminalScreen` | Use `terminalViewModel` instead of `chatViewModel` |
| `IntegrationsScreen` | Use `settingsViewModel` |
| `DaemonCommandCenterScreen` | Use `settingsViewModel` |

---

## Estimated Effort
- **Phase 1-2:** 2-3 hours (extract ViewModels)
- **Phase 3:** 1-2 hours (update composables)
- **Phase 4:** 30 min (imports/references)
- **Phase 5:** 30 min (cleanup + graphify validation)

**Total: ~4-6 hours**

---

## Success Criteria
- [ ] `ChatViewModel` < 200 lines, edges < 15
- [ ] `TerminalViewModel` handles all terminal state
- [ ] `SettingsViewModel` owns all SharedPreferences
- [ ] Graphify shows no single node spanning > 2 communities
- [ ] All existing functionality works (manual test)
- [ ] `graphify . --update` passes with improved metrics