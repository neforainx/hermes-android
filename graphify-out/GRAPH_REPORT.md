# Graph Report - .  (2026-07-24)

## Corpus Check
- Corpus is ~24,545 words - fits in a single context window. You may not need a graph.

## Summary
- 157 nodes · 265 edges · 12 communities (10 shown, 2 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 5 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8

## God Nodes (most connected - your core abstractions)
1. `ChatViewModel` - 27 edges
2. `TerminalViewModel` - 14 edges
3. `DaemonCommandCenterScreen()` - 13 edges
4. `SettingsViewModel` - 13 edges
5. `TuiTerminalScreen()` - 9 edges
6. `IntegrationsScreen()` - 9 edges
7. `processCliCommand()` - 8 edges
8. `AppDatabase` - 8 edges
9. `MainScreen()` - 7 edges
10. `MessageEntity` - 7 edges

## Surprising Connections (you probably didn't know these)
- `AgentChatScreen()` --references--> `ChatViewModel`  [EXTRACTED]
  MainActivity.kt → app/src/main/kotlin/com/example/viewmodel/ChatViewModel.kt
- `MessageRow()` --references--> `MessageEntity`  [EXTRACTED]
  MainActivity.kt → Room.kt
- `TuiTerminalScreen()` --references--> `ChatViewModel`  [EXTRACTED]
  MainActivity.kt → app/src/main/kotlin/com/example/viewmodel/ChatViewModel.kt
- `processCliCommand()` --references--> `ChatViewModel`  [EXTRACTED]
  MainActivity.kt → app/src/main/kotlin/com/example/viewmodel/ChatViewModel.kt
- `DaemonCommandCenterScreen()` --references--> `ChatViewModel`  [EXTRACTED]
  MainActivity.kt → app/src/main/kotlin/com/example/viewmodel/ChatViewModel.kt

## Import Cycles
- None detected.

## Communities (12 total, 2 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.15
Nodes (29): androidx, Color, Context, AddCronJobDialog(), AgentChatScreen(), AgentThinkingPlaceholder(), BasicTextField(), CronJobItemRow() (+21 more)

### Community 1 - "Community 1"
Cohesion: 0.11
Nodes (12): Flow, SkillItemRow(), AppDatabase, CronJobDao, CronJobEntity, MessageDao, MessageEntity, PetStateDao (+4 more)

### Community 2 - "Community 2"
Cohesion: 0.11
Nodes (18): NetworkClient, OpenRouterApiService, OpenRouterChoice, OpenRouterClient, OpenRouterError, OpenRouterMessage, OpenRouterRequest, OpenRouterResponse (+10 more)

### Community 3 - "Community 3"
Cohesion: 0.12
Nodes (6): android, ChatViewModel, DatabaseProvider, AndroidViewModel, StateFlow, TextToSpeech

### Community 4 - "Community 4"
Cohesion: 0.20
Nodes (16): AgentChatScreen(), AgentThinkingPlaceholder(), DaemonCommandCenterScreen(), HermesAppTheme(), HermesLogoCompose(), IntegrationsScreen(), Bundle, ComponentActivity (+8 more)

### Community 5 - "Community 5"
Cohesion: 0.19
Nodes (3): AndroidViewModel, TerminalSession, TerminalViewModel

### Community 7 - "Community 7"
Cohesion: 0.40
Nodes (4): HermesAppTheme(), Bundle, ComponentActivity, MainActivity

## Knowledge Gaps
- **6 isolated node(s):** `GenerationConfig`, `Candidate`, `NetworkClient`, `NetworkClient`, `OpenRouterChoice` (+1 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ChatViewModel` connect `Community 3` to `Community 0`, `Community 1`, `Community 2`, `Community 4`?**
  _High betweenness centrality (0.579) - this node is a cross-community bridge._
- **Why does `MessageEntity` connect `Community 1` to `Community 0`, `Community 2`, `Community 3`, `Community 4`?**
  _High betweenness centrality (0.224) - this node is a cross-community bridge._
- **Why does `TuiTerminalScreen()` connect `Community 4` to `Community 3`, `Community 5`?**
  _High betweenness centrality (0.202) - this node is a cross-community bridge._
- **What connects `GenerationConfig`, `Candidate`, `NetworkClient` to the rest of the system?**
  _6 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.10574712643678161 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.11333333333333333 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.11764705882352941 - nodes in this community are weakly interconnected._