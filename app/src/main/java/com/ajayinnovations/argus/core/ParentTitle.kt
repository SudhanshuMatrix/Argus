package com.ajayinnovations.argus.core

/** How the app addresses the parent. Picks the voice clip set; NEUTRAL uses "Please stop". */
enum class ParentTitle(val key: String) {
    MUMMY("mummy"),
    DADDY("daddy"),
    GRANDMA("grandma"),
    NEUTRAL("neutral");

    companion object {
        /** Unknown or missing keys fall back to NEUTRAL. */
        fun fromKey(key: String?): ParentTitle =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: NEUTRAL
    }
}
