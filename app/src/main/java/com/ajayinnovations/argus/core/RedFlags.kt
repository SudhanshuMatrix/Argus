package com.ajayinnovations.argus.core

/**
 * Warning signs. They never decide on their own: two or more only set the safety floor
 * in [ScamChecker]. Grow the keyword list from real scam samples.
 */
object RedFlags {

    private val link = Regex("""https?://|www\.|bit\.ly|wa\.me|tinyurl""", RegexOption.IGNORE_CASE)

    /** Nigerian mobile numbers: 080x, 070x, 090x, 081x, 091x... or +234 / 234 forms. */
    private val phone = Regex("""(\+?234|0)[789][01]\d{8}""")

    /** Spaces or dashes between digits ("0801 234 5678") are removed before the phone check. */
    private val digitSeparator = Regex("""(?<=\d)[\s-](?=\d)""")

    private val keywords = listOf(
        "bvn", "nin", "blocked", "suspended", "restricted", "deactivated", "upgrade",
        "reversal", "refund", "winner", "you have won", "prize", "promo", "congratulations",
        "verify", "update your", "click", "pin", "password", "urgent", "urgently", "within 24",
        "loan", "investment", "double your", "crypto", "job offer", "customer care",
        "cbn", "efcc", "grant", "dollar", "otp",
    ).map { it to Regex("""\b${Regex.escape(it)}\b""") }   // "pin" must not match "shopping"

    fun hasLink(text: String): Boolean = link.containsMatchIn(text)

    fun find(text: String): List<String> {
        val lower = text.lowercase()
        return buildList {
            if (link.containsMatchIn(text)) add("link")
            if (phone.containsMatchIn(text.replace(digitSeparator, ""))) add("phone number")
            keywords.filter { it.second.containsMatchIn(lower) }.forEach { add(it.first) }
        }
    }
}
