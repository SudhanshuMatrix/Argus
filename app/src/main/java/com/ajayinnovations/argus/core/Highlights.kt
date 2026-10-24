package com.ajayinnovations.argus.core

/** What a marked stretch of the message is, for the screen reader ("Marked: BVN, link"). */
enum class MarkKind { LINK, SECRET, ACCOUNT, MONEY, NEW_NUMBER, PRIZE, PHONE }

data class Mark(val start: Int, val end: Int, val kind: MarkKind) {
    val range: IntRange get() = start until end
}

/**
 * The exact dangerous parts of a message, to highlight on the warning screen: links, the
 * secret words asked for, account threats, money amounts, "new number" phrases, prizes and
 * phone numbers. Same patterns as the rules. Ranges never overlap; pure Kotlin.
 */
object Highlights {

    private fun words(vararg w: String) = Regex("""\b(${w.joinToString("|")})\b""", RegexOption.IGNORE_CASE)

    private val link = Regex("""(https?://\S+|www\.\S+|\b(bit\.ly|wa\.me|tinyurl\.com|t\.me)/\S*)""", RegexOption.IGNORE_CASE)
    private val secret = words("bvn", "nin", "pin", "otp", "password", "cvv", "card number", "card details", "token", "code")
    private val account = words(
        "blocked", "block", "suspended", "restricted", "deactivated", "closed", "verify", "verification",
        "update your account", "update your bvn", "upgrade",
    )
    private val money = Regex(
        """(₦\s?\d[\d,]*(\.\d+)?|\bN\s?\d[\d,]*(\.\d+)?\b|\bNGN\s?\d[\d,]*\b|\b\d[\d,]*(\.\d+)?\s?(naira|k)\b)""",
        RegexOption.IGNORE_CASE,
    )
    private val prize = words("you have won", "you won", "winner", "prize", "promo", "congratulations")
    private val phone = Regex("""(\+?234|\b0)[789][01]\d[\s-]?\d{3}[\s-]?\d{4}\b""")

    /** Marks for [text]; secret words only when the rules found a request for one. */
    fun find(text: String, findings: List<Finding> = Findings.find(text)): List<Mark> {
        val candidates = buildList {
            link.findAll(text).forEach { add(Mark(it.range.first, trimEnd(text, it.range.last + 1), MarkKind.LINK)) }
            if (findings.any { it is Finding.AsksForSecret }) secret.findAll(text).forEach { add(it.mark(MarkKind.SECRET)) }
            account.findAll(text).forEach { add(it.mark(MarkKind.ACCOUNT)) }
            money.findAll(text).forEach { add(it.mark(MarkKind.MONEY)) }
            ConcreteSigns.newNumber.findAll(text).forEach { add(it.mark(MarkKind.NEW_NUMBER)) }
            prize.findAll(text).forEach { add(it.mark(MarkKind.PRIZE)) }
            phone.findAll(text).forEach { add(it.mark(MarkKind.PHONE)) }
        }
        // Earliest first, longest first; drop anything overlapping a mark already taken
        val taken = mutableListOf<Mark>()
        candidates.sortedWith(compareBy<Mark> { it.start }.thenByDescending { it.end - it.start }).forEach { m ->
            if (m.end > m.start && taken.none { it.start < m.end && m.start < it.end }) taken += m
        }
        return taken
    }

    private fun MatchResult.mark(kind: MarkKind) = Mark(range.first, range.last + 1, kind)

    /** A link at the end of a sentence shouldn't swallow the full stop. */
    private fun trimEnd(text: String, end: Int): Int {
        var e = end
        while (e > 0 && text[e - 1] in ".,;:!?)\"'…") e--
        return e
    }
}
