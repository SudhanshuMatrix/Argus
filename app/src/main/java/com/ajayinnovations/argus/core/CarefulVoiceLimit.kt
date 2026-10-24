package com.ajayinnovations.argus.core

/**
 * At most one spoken "be careful" per chat every [windowMillis], so a busy group can't keep the
 * phone talking. The notification still posts each time; SCAM and OTP always speak. Remembers
 * a hash of the chat and a time, in memory only. Pure Kotlin.
 */
class CarefulVoiceLimit(
    private val windowMillis: Long = WINDOW_MILLIS,
    private val capacity: Int = 100,
) {
    private val lastSpoken = object : LinkedHashMap<Int, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Long>) = size > capacity
    }

    /** True when the voice may play for [level] now. A null [chatId] (manual checks, tests) always may. */
    @Synchronized
    fun allow(level: Level, chatId: Int?, now: Long): Boolean {
        if (level != Level.CAREFUL || chatId == null) return true
        val last = lastSpoken[chatId]
        if (last != null && now - last < windowMillis) return false
        lastSpoken[chatId] = now
        return true
    }

    companion object {
        const val WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
