package com.ajayinnovations.argus.core

/** The start of a message for the warning screen: one line of spaces, at most [max] characters. */
object MessagePreview {
    const val DEFAULT_MAX = 120
    /** Never longer than this, even to keep a dangerous part. */
    const val HARD_MAX = 200

    /**
     * Like [of], but stretched (up to [HARD_MAX]) so a dangerous part that starts early, such
     * as the link at the end of a scam, isn't cut off.
     */
    fun keepingMarks(text: String, findings: List<Finding>): String {
        val flat = flatten(text)
        val keepUntil = Highlights.find(flat, findings).filter { it.start < HARD_MAX }.maxOfOrNull { it.end } ?: 0
        return of(flat, maxOf(DEFAULT_MAX, keepUntil.coerceAtMost(HARD_MAX)))
    }

    /** The whole message for the warning screen: trimmed, line breaks kept, never past [FULL_MAX]. */
    fun full(text: String): String {
        val t = text.trim()
        if (t.length <= FULL_MAX) return t
        var cut = FULL_MAX
        if (t[cut - 1].isHighSurrogate()) cut--
        return t.substring(0, cut).trimEnd() + "…"
    }

    const val FULL_MAX = 4000

    private fun flatten(text: String) = text.replace(Regex("""\s+"""), " ").trim()

    fun of(text: String, max: Int = DEFAULT_MAX): String {
        val flat = flatten(text)
        if (flat.length <= max) return flat
        // Cut at a word boundary when one is near, so the preview doesn't end mid-word
        var cut = flat.lastIndexOf(' ', max).takeIf { it >= max - 20 } ?: max
        // Never split an emoji (a surrogate pair) in half
        if (cut > 0 && flat[cut - 1].isHighSurrogate()) cut--
        return flat.substring(0, cut).trimEnd() + "…"
    }
}
