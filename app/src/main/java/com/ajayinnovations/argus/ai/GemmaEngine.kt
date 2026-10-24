package com.ajayinnovations.argus.ai

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ajayinnovations.argus.core.GpuPolicy
import com.ajayinnovations.argus.core.ModelHeader
import com.ajayinnovations.argus.settings.Settings
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** A model failure the caller can catch as a normal Exception, whatever the native cause was. */
class LlmFailure(cause: Throwable) : Exception("Model failed: ${cause.javaClass.simpleName}", cause)

/**
 * Gemma on LiteRT-LM 0.17.1. One inference at a time, CPU by default (the GPU is opt-in, see
 * [GpuPolicy]). Once loaded it stays loaded, so a message never waits for a reload; it is
 * unloaded only when Android reports memory pressure (see ArgusApp.onTrimMemory).
 * Logs only backends, timings and counts.
 */
class GemmaEngine(
    private val context: Context,
    private val models: ModelSource,
    private val settings: Settings,
    private val cache: ModelCache,
) : LlmEngine {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    // Only touched while holding mutex
    private var engine: Engine? = null
    private var backend: ComputeBackend? = null

    override suspend fun generate(system: String, prompt: String, maxOutputTokens: Int): String = mutex.withLock {
        runWithFallback(system, prompt, maxOutputTokens)
    }

    /**
     * One conversation for the whole [block]: each ask is a new turn, so LiteRT-LM keeps the
     * earlier turns (the message) in its KV cache instead of reading them again. Holds the lock
     * throughout. No GPU retry here: the GPU is opt-in and a failure surfaces as [LlmFailure].
     */
    override suspend fun <T> chat(
        system: String,
        maxOutputTokens: Int,
        block: suspend (ask: suspend (String) -> String) -> T,
    ): T = mutex.withLock {
        try {
            val loaded = ensureLoaded()
            val onGpu = backend == ComputeBackend.GPU
            val conversation = nativeCall(onGpu) { loaded.createConversation(conversationConfig(system, maxOutputTokens)) }
            try {
                block { prompt ->
                    val start = SystemClock.elapsedRealtime()
                    val reply = nativeCall(onGpu) { conversation.sendMessage(prompt).text() }
                    record(SystemClock.elapsedRealtime() - start)
                    reply
                }
            } finally {
                runCatching { nativeCall(onGpu) { conversation.close() } }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ModelUnavailable) {
            throw e
        } catch (e: LlmFailure) {
            throw e
        } catch (t: Throwable) {
            throw LlmFailure(t)
        }
    }

    override suspend fun unload() = mutex.withLock {
        closeEngine()
    }

    override fun unloadSoon() {
        scope.launch { unload() }
    }

    private suspend fun runWithFallback(system: String, prompt: String, maxOutputTokens: Int): String {
        val loaded = ensureLoaded()
        return try {
            infer(loaded, system, prompt, maxOutputTokens)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // Class name only: native messages could echo the prompt
            Log.w(TAG, "Inference failed on $backend (${t.javaClass.simpleName})")
            if (backend != ComputeBackend.GPU) throw LlmFailure(t)
            // Some GPUs load fine but fail on the first run: switch to CPU for good and retry once
            Log.w(TAG, "GPU inference failed (${t.javaClass.simpleName}); using CPU from now on")
            disableGpu()
            closeEngine()
            try {
                infer(ensureLoaded(), system, prompt, maxOutputTokens)
            } catch (e: CancellationException) {
                throw e
            } catch (retry: Throwable) {
                throw LlmFailure(retry)
            }
        }
    }

    private suspend fun infer(loaded: Engine, system: String, prompt: String, maxOutputTokens: Int): String {
        val start = SystemClock.elapsedRealtime()
        val reply = nativeCall(onGpu = backend == ComputeBackend.GPU) {
            // A fresh conversation per message, so nothing from one message leaks into the next
            loaded.createConversation(conversationConfig(system, maxOutputTokens)).use { conversation ->
                conversation.sendMessage(prompt).text()
            }
        }
        record(SystemClock.elapsedRealtime() - start)
        return reply
    }

    private fun conversationConfig(system: String, maxOutputTokens: Int) = ConversationConfig(
        systemInstruction = Contents.of(system),
        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.1),
        maxOutputToken = maxOutputTokens,
    )

    /** 0.17.1 has no Message.text: join the text parts. */
    private fun Message.text(): String =
        contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

    private fun record(millis: Long) {
        _state.update { it.copy(checkCount = it.checkCount + 1, totalCheckMillis = it.totalCheckMillis + millis) }
        Log.i(TAG, "Inference on $backend took $millis ms")
    }

    private suspend fun ensureLoaded(): Engine {
        engine?.let { return it }
        // Never copies: background callers skip until setup has made the model ready
        val file = models.readyFile() ?: throw ModelUnavailable.Missing()

        if (settings.gpuCallInProgress) {
            val deaths = GpuPolicy.deathsAfterStart(callWasInProgress = true, deathsInARow = settings.gpuDeathsInARow)
            settings.gpuDeathsInARow = deaths
            settings.gpuCallInProgress = false
            Log.w(TAG, "App died during a GPU call ($deaths in a row)")
            if (GpuPolicy.shouldDisable(deaths)) {
                Log.w(TAG, "Using CPU from now on")
                disableGpu()
            }
        }

        _state.update { it.copy(loading = true) }
        try {
            // A broken file can abort the process inside LiteRT-LM: never hand it one
            if (!hasModelHeader(file)) throw IllegalStateException("Not a LiteRT-LM model file")
            val start = SystemClock.elapsedRealtime()
            val useGpu = GpuPolicy.shouldUseGpu(optedIn = settings.useGpu, disabled = settings.gpuDisabled)
            val (created, used) = if (useGpu) {
                try {
                    create(file, ComputeBackend.GPU) to ComputeBackend.GPU
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.w(TAG, "GPU load failed (${t.javaClass.simpleName}); using CPU from now on")
                    disableGpu()
                    createOnCpu(file) to ComputeBackend.CPU
                }
            } else {
                createOnCpu(file) to ComputeBackend.CPU
            }
            val millis = SystemClock.elapsedRealtime() - start
            engine = created
            backend = used
            _state.update { it.copy(backend = used, loadMillis = millis, loadFailed = false) }
            Log.i(TAG, "Model loaded on $used in $millis ms")
            return created
        } catch (e: CancellationException) {
            throw e
        } catch (e: ModelUnavailable) {
            throw e
        } catch (t: Throwable) {
            _state.update { it.copy(loadFailed = true) }
            throw LlmFailure(t)
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    private fun hasModelHeader(file: File): Boolean = try {
        val start = ByteArray(ModelHeader.SIZE)
        val read = file.inputStream().use { it.read(start) }
        read == start.size && ModelHeader.isValid(start)
    } catch (e: java.io.IOException) {
        false
    }

    /**
     * A broken CPU weight cache (say the app died while LiteRT-LM was writing it) would make
     * every load fail: delete it and try once more, which writes a fresh one.
     */
    private suspend fun createOnCpu(file: File): Engine = try {
        create(file, ComputeBackend.CPU)
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        if (!cache.deleteCpuCaches()) throw t
        Log.w(TAG, "CPU load failed (${t.javaClass.simpleName}); retrying without the weight cache")
        create(file, ComputeBackend.CPU)
    }

    private suspend fun create(file: File, use: ComputeBackend): Engine =
        nativeCall(onGpu = use == ComputeBackend.GPU) {
            val created = Engine(
                EngineConfig(
                    modelPath = file.absolutePath,
                    backend = if (use == ComputeBackend.GPU) Backend.GPU() else Backend.CPU(),
                    maxNumTokens = MAX_NUM_TOKENS,
                    cacheDir = context.cacheDir.path,
                ),
            )
            try {
                created.initialize()   // can take ~10 s
                created
            } catch (t: Throwable) {
                runCatching { created.close() }
                throw t
            }
        }

    /**
     * Runs a blocking native call off the main thread. On the GPU it brackets the call with a
     * flag written synchronously, so a native crash (which no catch can see) is noticed at the
     * next start.
     */
    private suspend fun <T> nativeCall(onGpu: Boolean, block: () -> T): T = withContext(Dispatchers.Default) {
        if (onGpu) settings.gpuCallInProgress = true
        try {
            block().also {
                // A GPU call that finishes breaks the run of deaths
                if (onGpu && settings.gpuDeathsInARow != 0) settings.gpuDeathsInARow = 0
            }
        } finally {
            if (onGpu) settings.gpuCallInProgress = false
        }
    }

    private suspend fun closeEngine() {
        val current = engine ?: return
        val wasGpu = backend == ComputeBackend.GPU
        engine = null
        backend = null
        runCatching { nativeCall(onGpu = wasGpu) { current.close() } }
            .onFailure { Log.w(TAG, "Engine close failed (${it.javaClass.simpleName})") }
        _state.update { it.copy(backend = null) }
        Log.i(TAG, "Model unloaded")
    }

    /** GPU off for good: its ~0.5 GB weight cache would never be used again. */
    private fun disableGpu() {
        settings.gpuDisabled = true
        cache.deleteGpuCaches()
    }

    private companion object {
        const val TAG = "GemmaEngine"
        /**
         * Context window: system prompt (~400) + message (capped at 1,500 chars, ~500) + reply.
         * Smaller than the model default, so the KV cache uses less RAM on budget phones.
         */
        const val MAX_NUM_TOKENS = 1536
    }
}
