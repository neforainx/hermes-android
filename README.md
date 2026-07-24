# Hermes Android - Full Wrapped Implementation

Complete self-contained Android APK with embedded Termux environment, Hermes Daemon, and all AI capabilities running locally.

## Architecture

```
Single APK
├── Hermes UI (Jetpack Compose, 4 tabs)
├── Hermes Daemon Service (Foreground Service, separate process)
│   ├── Python Hermes Agent (in proot/Termux)
│   ├── Termux RootFS (embedded in assets)
│   └── Local Socket/TCP IPC (port 5175)
├── Room Database (Messages, Cron, Skills, Pet)
├── Certificate Pinning (Google APIs, OpenRouter)
└── Settings (SharedPreferences)
```

## Components Created

### Kotlin Files (app/src/main/kotlin/com/example/)

| File | Purpose |
|------|---------|
| `MainActivity.kt` | 4-tab UI (Agent, Terminal, Setup, Profile) with Compose |
| `HermesApplication.kt` | Application class, Room init |
| `service/HermesDaemonService.kt` | Foreground Service, Termux bootstrap, Daemon manager |
| `ipc/HermesSocketClient.kt` | Unix/TCP socket client for UI↔Daemon communication |
| `database/Database.kt` | Room entities + DAOs + DatabaseProvider |
| `api/NetworkClient.kt` | Shared OkHttpClient with Certificate Pinning |
| `api/GeminiApi.kt` | Gemini API service with cert pinning |
| `api/OpenRouterClient.kt` | OpenRouter API service with cert pinning |
| `viewmodel/SettingsViewModel.kt` | All SharedPreferences settings (25+ keys) |
| `viewmodel/ChatViewModel.kt` | Chat logic, AI providers, slash commands |
| `viewmodel/TerminalViewModel.kt` | Terminal sessions, logs, commands |
| `viewmodel/CronViewModel.kt` | Cron jobs scheduling & monitoring |
| `viewmodel/SkillViewModel.kt` | 14 skills management |
| `viewmodel/PetViewModel.kt` | Virtual pet (hatch, feed, play, XP) |

### Python Daemon (app/src/main/assets/)

| File | Purpose |
|------|---------|
| `hermes-daemon.py` | Async daemon with Unix/TCP servers, handles chat/terminal/cron/skill/settings |

### Configuration

| File | Purpose |
|------|---------|
| `app/build.gradle.kts` | Dependencies, buildConfig fields |
| `settings.gradle.kts` | Project setup |
| `gradle/libs.versions.toml` | Version catalog |
| `AndroidManifest.xml` | Permissions, service, app class |
| `network_security_config.xml` | Certificate pinning config |
| `proguard-rules.pro` | Release obfuscation rules |

## Build Instructions

### Prerequisites
- Android Studio Koala+ / JDK 17
- Android SDK 36 (compileSdk)
- Min SDK 26 (Android 8.0)

### Build Termux RootFS (One-time)

```bash
# Option 1: Use pre-built (download from termux.dev)
# Option 2: Build yourself
docker run --rm -v $(pwd)/app/src/main/assets:/out termux/termux:latest \
  tar -czf /out/termux-rootfs.tar.gz -C /data/data/com.termux/files .
```

Place `termux-rootfs.tar.gz` in `app/src/main/assets/`

### Build APK

```bash
# Debug
./gradlew assembleDebug

# Release (with minifyEnabled=true after testing)
./gradlew assembleRelease
```

### Install & Run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.aistudio.hermes.pkwsva/.MainActivity
```

## Features

### Chat (Tab 1)
- **3 AI Providers**: Gemini, Nous/OpenRouter, Custom OpenAI-compatible
- **Slash Commands**: `/help`, `/model`, `/provider`, `/key`, `/clear`, `/clear-sessions`, `/file`
- **Workspace File Injection**: Attach local files as context
- **Quick Engine Switcher**: Dropdown in header bar
- **TTS**: Text-to-speech for responses

### Terminal (Tab 2)
- Multi-session support
- Full command execution in proot/Termux
- Log sanitization (redacts API keys, tokens, credit cards)
- Slash command integration

### Daemon Command Center (Tab 3)
- Wizard-style setup (5 steps)
- Provider configuration
- Sandbox selection (Embedded Termux, Docker, SSH)
- Gateway platforms (Telegram, Discord)
- SOUL.md personality editor
- Live terminal logs viewer

### Profile/Integrations (Tab 4)
- All 25+ settings keys
- Cron job manager
- Skills installer (14 skills)
- Virtual pet (Hermie)
- Battery monitor

### Security
- **Certificate Pinning**: Google APIs + OpenRouter SHA-256 pins
- **Network Security Config**: Enforces HTTPS, pins
- **Log Sanitization**: Auto-redacts sensitive data
- **No Cleartext**: `usesCleartextTraffic=false`
- **Shared OkHttpClient**: Single pinned client for all external calls

## IPC Protocol

UI ↔ Daemon communication over TCP `127.0.0.1:5175` (Unix socket also supported):

```json
// Request
{"cmd": "chat", "data": {"message": "hello", "provider": "nous"}}

// Response
{"status": "queued", "message": "Chat request forwarded to UI"}
```

Commands: `chat`, `terminal`, `cron`, `skill`, `settings`, `session`, `status`

## Daemon Commands (Python)

```python
# In proot environment
python3 hermes-daemon.py
```

Daemon exposes:
- Unix socket: `/data/data/com.example/files/hermes.sock`
- TCP: `127.0.0.1:5175`
- Handles: chat, terminal exec, cron, skills, settings, sessions

## Customization

### API Keys
Set via Settings tab or `local.properties`:
```
GEMINI_API_KEY=your_key_here
```

### Certificate Pins (Update when certs rotate)
Edit `NetworkClient.kt` and `network_security_config.xml`

### Theme Colors
Edit `MainActivity.kt` color constants:
```kotlin
val BackgroundDark = Color(0xFF070B19)
val GoldCream = Color(0xFFFFE6CB)
// etc.
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Daemon not starting | Check logcat `HERMES` tag, ensure `termux-rootfs.tar.gz` in assets |
| Socket connection failed | Verify foreground service running, check port 5175 |
| Cert pinning errors | Update pins in `NetworkClient.kt` + `network_security_config.xml` |
| Proot binary missing | Auto-downloads on first run (GitHub releases) |

## License

Proprietary - Hermes AI Agent