package com.ajayinnovations.argus.core

/** Answers "is this obviously harmless?", never "does this look like a scam?". */
object SkipFilter {

    /** True only for messages that clearly need no check. Anything unsure goes to the model. */
    fun isObviouslyHarmless(text: String): Boolean {
        val t = text.trim()
        if (t.length < MIN_LENGTH) return true                    // "ok", "Amen", "Thank you"
        if (t.none { it.isLetterOrDigit() }) return true          // only emojis or symbols
        return t.length < SHORT_LENGTH && t.none { it.isDigit() } && RedFlags.find(t).isEmpty()
    }

    const val MIN_LENGTH = 8
    private const val SHORT_LENGTH = 40
}
