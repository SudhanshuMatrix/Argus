package com.ajayinnovations.argus.ai

import com.ajayinnovations.argus.core.TextGenerator
import kotlinx.coroutines.flow.StateFlow

enum class ComputeBackend { GPU, CPU }

/** What the engine is doing. Timings and counts only, never text. */
data class EngineState(
    val loading: Boolean = false,
    val backend: ComputeBackend? = null,
    val loadMillis: Long? = null,
    val checkCount: Int = 0,
    val totalCheckMillis: Long = 0,
    /** The last load failed (a broken model copy or cache). The home screen offers to set it up again. */
    val loadFailed: Boolean = false,
) {
    val loaded: Boolean get() = backend != null
    val averageCheckMillis: Long? get() = if (checkCount == 0) null else totalCheckMillis / checkCount
}

/** The app talks to this, so the model is swappable and core never imports LiteRT-LM. */
interface LlmEngine : TextGenerator {
    val state: StateFlow<EngineState>

    /**
     * Runs one prompt in a fresh conversation and returns the raw reply.
     * Throws [ModelUnavailable] if the model isn't ready; never starts a model copy.
     */
    override suspend fun generate(system: String, prompt: String, maxOutputTokens: Int): String

    /** Frees the model's memory. The next [generate] loads it again. */
    suspend fun unload()

    /** Like [unload] but returns at once; the unload runs when the current check finishes. */
    fun unloadSoon()
}
