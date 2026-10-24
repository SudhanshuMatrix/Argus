package com.ajayinnovations.argus.core

/**
 * WhatsApp re-posts the same notification as messages stack up. Remembers recent message
 * hashes in memory only (never the text), oldest dropped after [capacity].
 */
class Deduper(private val capacity: Int = 200) {

    private val seen = object : LinkedHashMap<Int, Unit>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Unit>) = size > capacity
    }

    @Synchronized
    fun isNew(text: String): Boolean {
        val key = text.trim().lowercase().hashCode()
        if (seen.containsKey(key)) {
            seen[key] = Unit   // refresh: a repeat keeps it remembered
            return false
        }
        seen[key] = Unit
        return true
    }
}
