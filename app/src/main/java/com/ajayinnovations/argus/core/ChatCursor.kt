package com.ajayinnovations.argus.core

/**
 * Which messages in a chat notification are new. WhatsApp keeps one notification per chat
 * holding its recent messages and re-posts it for each new one. In a burst (a busy group)
 * Android drops some of those updates, because it rate-limits each app, so reading only the
 * newest message would skip the ones in between.
 *
 * Remembers, in memory only, the newest message time seen per chat, keyed by a hash of the
 * notification key. No text. Pure Kotlin.
 */
class ChatCursor(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val freshMillis: Long = FRESH_MILLIS,
) {
    private val newest = object : LinkedHashMap<Int, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Long>) = size > capacity
    }

    /**
     * Indexes into [timestamps] (oldest first, as MessagingStyle lists them) to check.
     * - A chat seen before: every message newer than the newest one seen.
     * - A chat not seen yet (its first message, or after a restart): the messages from the
     *   last [freshMillis] before [postedAt], so old ones already checked aren't alerted again,
     *   and always the newest one.
     * - Any message without a time: only the newest one, as before.
     */
    @Synchronized
    fun newMessages(chatKey: String, timestamps: List<Long>, postedAt: Long): List<Int> {
        if (timestamps.isEmpty()) return emptyList()
        val newestIndex = timestamps.lastIndex
        if (timestamps.any { it <= 0 }) return listOf(newestIndex)

        val key = chatKey.hashCode()
        val seen = newest[key]
        newest[key] = maxOf(seen ?: Long.MIN_VALUE, timestamps.max())
        return if (seen != null) {
            timestamps.indices.filter { timestamps[it] > seen }
        } else {
            val since = postedAt - freshMillis
            timestamps.indices.filter { timestamps[it] >= since }.ifEmpty { listOf(newestIndex) }
        }
    }

    companion object {
        const val DEFAULT_CAPACITY = 100
        const val FRESH_MILLIS = 5 * 60 * 1000L
    }
}
