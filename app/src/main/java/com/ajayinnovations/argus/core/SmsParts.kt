package com.ajayinnovations.argus.core

/** A long SMS arrives as several parts in one broadcast; join them per sender, in order. */
object SmsParts {

    data class Part(val sender: String?, val body: String?)

    /** One (sender, full text) per sender, in arrival order. Blank results are dropped. */
    fun join(parts: List<Part>, unknownSender: String): List<Pair<String, String>> =
        parts.groupBy { it.sender?.takeIf(String::isNotBlank) ?: unknownSender }
            .map { (sender, ps) -> sender to ps.joinToString("") { it.body.orEmpty() } }
            .filter { it.second.isNotBlank() }
}
