package com.ajayinnovations.argus.core

/** What core needs from a language model. The Android engine implements it; tests fake it. */
interface TextGenerator {
    suspend fun generate(system: String, prompt: String, maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS): String

    /**
     * Runs [block] in one conversation: each `ask` adds a turn after the earlier ones, so an
     * engine can reuse what it already read (the message) instead of reading it again.
     * The default just makes independent calls.
     */
    suspend fun <T> chat(system: String, maxOutputTokens: Int, block: suspend (ask: suspend (String) -> String) -> T): T =
        block { prompt -> generate(system, prompt, maxOutputTokens) }

    companion object {
        /** A verdict JSON is about 60–80 tokens; this leaves room without letting it ramble. */
        const val DEFAULT_MAX_OUTPUT_TOKENS = 160
    }
}
