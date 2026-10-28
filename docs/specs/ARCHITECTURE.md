# ARCHITECTURE.md

## 1. Gradle modules

| Module | Type | Contains | Depends on |
|---|---|---|---|
| `:app` | android app | `MainActivity`, navigation, Hilt setup, screens wiring, assistant service | all |
| `:core-ui` | android lib | design system (tokens, components, motion), Pim composables; wraps Compose Unstyled, Haze, Capsule, shimmer | – |
| `:core-common` | kotlin/jvm lib | `Result`, dispatchers, time provider, `Locale` helpers, logging facade | – |
| `:core-data` | android lib | Room DB, DAOs, SQLCipher, DataStore settings | `:core-common` |
| `:core-llm` | android lib + CMake | JNI bridge, `LlmEngine`, `PromptBuilder`, `ToolCallParser`, sampler config, GBNF | `:core-common` |
| `:core-tools` | android lib | `ToolRouter`, `NetworkGate`, all tools (search, weather, wiki, news, calc, intents) | `:core-common`, `:core-data` |
| `:core-memory` | android lib | memory layers, embedder, retrieval | `:core-data`, `:core-llm` |
| `:core-docs` | android lib | PDF text, OCR, correction, doc chunks | `:core-common`, `:core-data` |
| `:core-voice` | android lib | STT, TTS (sherpa-onnx) | `:core-common` |
| `:core-cards` | android lib | OpenUI subset parser + Compose renderer | `:core-ui` |
| `:core-models` | android lib | model catalogue, download manager, device check | `:core-data` |
| `:core-testing` | kotlin/jvm lib | fakes for every interface, test fixtures | api packages |
| `:feature-chat` | android lib | chat screen, chat list, composer, ViewModels | core-* |
| `:feature-settings` | android lib | settings screens | core-* |
| `:feature-memory` | android lib | memory screen | core-memory |
| `:feature-news` | android lib | news screen + feed settings | core-tools |
| `:feature-image` | android lib | image editor, optional generator | core-ui |
| `:feature-onboarding` | android lib | first start, pack download | core-models |

Rule: `feature-*` never depend on each other. Shared things go to a `core-*` module.

## 2. Package layout (per module)

```
app.hain.<module>/
  api/        ← public interfaces + data classes (stable, small)
  internal/   ← implementations (Hilt-bound)
  di/         ← Hilt module
```

## 3. Core interfaces (write these first, T-010…T-016)

```kotlin
// :core-llm  app.hain.llm.api
interface LlmEngine {
    val state: StateFlow<EngineState>                 // Unloaded, Loading(progress), Ready(modelId), Error(msg)
    suspend fun load(model: ModelSpec, params: LoadParams)
    suspend fun unload()
    fun generate(prompt: String, params: SamplingParams, grammar: String? = null): Flow<GenEvent>
    fun cancel()
    suspend fun tokenCount(text: String): Int
}
sealed interface GenEvent { data class Token(val text: String): GenEvent
    data class Done(val stats: GenStats): GenEvent; data class Failed(val error: String): GenEvent }

data class ChatMessage(val role: Role, val content: String,
    val toolCalls: List<ToolCall> = emptyList(), val reasoning: String? = null, val toolCallId: String? = null)
enum class Role { SYSTEM, USER, ASSISTANT, TOOL }

interface PromptBuilder {               // pure Kotlin, re-implements chat_template.jinja
    fun build(system: String?, tools: List<ToolSchema>, history: List<ChatMessage>,
              addGenerationPrompt: Boolean = true, enableThinking: Boolean = true): String
}
interface ToolCallParser { fun parse(output: String): ParsedOutput }   // reasoning, text, toolCalls

// :core-tools  app.hain.tools.api
interface Tool { val schema: ToolSchema; suspend fun run(args: Map<String, Any?>, ctx: ToolContext): ToolResult }
data class ToolResult(val forModel: String, val card: CardData?, val sources: List<Source>, val error: String? = null)
interface ToolRouter { fun tools(enabled: Set<String>): List<Tool>; suspend fun call(call: ToolCall, ctx: ToolContext): ToolResult }
interface NetworkGate { suspend fun get(req: GateRequest): GateResponse; val log: Flow<List<NetworkLogEntry>> }

// :core-memory  app.hain.memory.api
interface MemoryStore { suspend fun context(query: String, budgetTokens: Int): MemoryContext
    suspend fun remember(text: String, origin: Origin); suspend fun forget(id: Long); fun all(): Flow<List<MemoryItem>> }

// :core-docs  app.hain.docs.api
interface DocumentReader { suspend fun read(uri: Uri, progress: (Float) -> Unit): ReadDocument }
interface OcrEngine { suspend fun recognize(bitmap: Bitmap, lang: OcrLang): OcrResult }

// :core-voice  app.hain.voice.api
interface SpeechToText { fun listen(lang: AppLang): Flow<SttEvent>; fun stop() }
interface TextToSpeech { suspend fun speak(text: String, lang: AppLang); fun stop(); val speaking: StateFlow<Boolean> }

// :core-models  app.hain.models.api
interface ModelCatalog { fun all(): List<ModelSpec>; fun installed(): Flow<List<InstalledModel>> }
interface PackDownloader { fun download(id: String): Flow<DownloadState>; suspend fun delete(id: String) }
interface DeviceCheck { suspend fun profile(): DeviceProfile }  // ramGb, socName, freeStorage, gpu
```

## 4. Chat turn data flow

```
User sends text
 → ChatViewModel.send()
 → ChatEngine.turn(chatId, userMsg)
     1. MemoryStore.context(userMsg, 600 tokens)
     2. PromptBuilder.build(system = SystemPrompt.fill(...), tools = ToolRouter.tools(enabled), history)
     3. LlmEngine.generate(...) → stream tokens → StreamSplitter (reasoning | text | tool_call)
     4. if tool_call: ToolRouter.call() → append TOOL message → goto 2 (max 3 tool rounds)
     5. text → MarkdownCardSplitter → UI (markdown blocks + cards)
     6. save messages (Room), schedule MemoryExtractionWorker
```

`ChatEngine` lives in `:feature-chat/internal/engine` and is pure Kotlin except DI.

## 5. Threading

- Model calls on a single-thread dispatcher `LlmDispatcher` (llama.cpp context not thread-safe).
- Tokens are emitted via `callbackFlow` from JNI callback → buffered (64) → UI collects on Main.
- DB on `Dispatchers.IO`. Never block Main.

## 6. RAM policy (ADR-19)

`ModelSlot` holds at most one big model. Switching: `unload()` → `System.gc()` hint → `load()`.
Packs that need a different model (Omni for images, SD for generation) request the slot and give it back.
