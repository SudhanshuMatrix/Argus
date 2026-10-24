package com.ajayinnovations.argus.core

/**
 * Mode F guard: concrete, checkable signs found by plain rules. The model's SCAM is only
 * accepted when at least one is present.
 */
enum class Sign {
    LINK, PHONE, CODE_REQUEST, MONEY_REQUEST, URGENCY, OFFICIAL_CLAIM, NEW_NUMBER,
    /** A scheme promising a return: "invest N50k, get N150k", "double your money", "guaranteed profit". */
    INVESTMENT_RETURN,
    /** "Call" (or WhatsApp) a phone number about account or card trouble: fraud, blocked, unusual activity. */
    CALL_ABOUT_ACCOUNT,
}

object ConcreteSigns {

    private fun words(vararg w: String) = Regex("""\b(${w.joinToString("|")})\b""", RegexOption.IGNORE_CASE)

    private val secret = words("otp", "pin", "bvn", "nin", "password", "code", "token", "cvv", "card number", "card details")
    private val handOver = words("send", "share", "give", "forward", "tell", "confirm", "provide", "enter", "type", "reply", "drop")
    private val negated = Regex("""\b(do not|don't|dont|never|no)\s+(send|share|give|forward|tell|disclose|provide|reply|enter|type)\b""", RegexOption.IGNORE_CASE)

    private val pay = words("send", "pay", "transfer", "deposit", "remit", "credit", "invest")
    private val amount = Regex("""(₦|\bN\s?\d|\bNGN\s?\d|\bnaira\b|\d+\s?k\b|\bmoney\b|\bfee\b|\bcharge\b|\bcash\b)""", RegexOption.IGNORE_CASE)

    private val urgency = words("urgent", "urgently", "now", "immediately", "today", "quick", "quickly", "fast", "within 24", "sharp sharp", "asap")
    private val official = words(
        "dear customer", "dear valued customer", "valued customer", "customer care", "customer service",
        "customer support", "cbn", "efcc", "central bank", "government", "ministry", "our agent",
        "account officer", "management", "hr", "fraud desk", "fraud department", "fraud unit", "security team",
    )

    /** Money words for the investment pattern: N50,000, ₦50k, 50k, $200, 100,000 naira. */
    private const val MONEY = """(?:₦\s?\d[\d,.]*k?|\bN\s?\d[\d,.]*k?|\bNGN\s?\d[\d,.]*|\$\s?\d[\d,.]*|\b\d[\d,.]*\s?(?:k|naira|usdt|dollars?)\b)"""
    private val investment = Regex(
        """\b(?:invest\w*|put|deposit\w*)\b[^.!?\n]{0,40}?$MONEY[^.!?\n]{0,60}?\b(?:get|got|receive|received|earn|earned|withdraw|withdrew|make|made|returns?)\b[^.!?\n]{0,30}?$MONEY""" +
            """|\b(?:double|triple)\s+(?:your|ur)\s+(?:money|investment|cash|capital)\b""" +
            """|\bguaranteed\s+(?:profit|returns?|income|payout|interest)\b""" +
            """|\b\d+\s?%\s+(?:daily|weekly|monthly)\s+(?:profit|returns?|roi|interest)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val callVerb = words("call", "phone", "contact", "reach", "dial", "whatsapp", "text")
    /** Account or card trouble, not just the word "account": a real credit alert stays quiet. */
    private val accountTrouble = words(
        "fraud", "fraudulent", "unusual", "suspicious", "unauthorised", "unauthorized", "compromised",
        "blocked", "block", "restricted", "suspended", "closure", "closed", "deactivated", "frozen", "hacked",
    )
    internal val newNumber = Regex(
        """\b(new number|new line|friend'?s? (line|phone|number)|phone (fell|spoil|spoilt|got lost|was stolen)|lost my phone|don'?t call (the|my) old)""",
        RegexOption.IGNORE_CASE,
    )

    fun find(text: String): Set<Sign> {
        val flags = RedFlags.find(text)
        val asksHandOver = handOver.containsMatchIn(text.replace(negated, " "))
        return buildSet {
            if ("link" in flags) add(Sign.LINK)
            if ("phone number" in flags) add(Sign.PHONE)
            if (secret.containsMatchIn(text) && asksHandOver) add(Sign.CODE_REQUEST)
            if (pay.containsMatchIn(text) && amount.containsMatchIn(text)) add(Sign.MONEY_REQUEST)
            if (urgency.containsMatchIn(text)) add(Sign.URGENCY)
            if (official.containsMatchIn(text)) add(Sign.OFFICIAL_CLAIM)
            if (newNumber.containsMatchIn(text)) add(Sign.NEW_NUMBER)
            if (investment.containsMatchIn(text)) add(Sign.INVESTMENT_RETURN)
            if ("phone number" in flags && callVerb.containsMatchIn(text) && accountTrouble.containsMatchIn(text)) add(Sign.CALL_ABOUT_ACCOUNT)
        }
    }

    /**
     * Mode F: what the model's SCAM becomes. Kept as SCAM only with a concrete sign;
     * a money request alone becomes CAREFUL; anything else NONE. Reasons are fixed texts.
     */
    fun guardScam(signs: Set<Sign>): QuestionRules.Decision {
        val money = Sign.MONEY_REQUEST in signs
        return when {
            Sign.CODE_REQUEST in signs -> QuestionRules.Decision(Level.SCAM, ReasonCode.ASKS_FOR_CODE)
            money && Sign.NEW_NUMBER in signs -> QuestionRules.Decision(Level.SCAM, ReasonCode.MONEY_UNDER_PRESSURE)
            money && Sign.OFFICIAL_CLAIM in signs -> QuestionRules.Decision(Level.SCAM, ReasonCode.FAKE_OFFICIAL)
            money && Sign.URGENCY in signs -> QuestionRules.Decision(Level.SCAM, ReasonCode.MONEY_UNDER_PRESSURE)
            Sign.INVESTMENT_RETURN in signs -> QuestionRules.Decision(Level.SCAM, ReasonCode.TOO_GOOD_OFFER)
            Sign.LINK in signs || Sign.PHONE in signs -> QuestionRules.Decision(Level.SCAM, ReasonCode.LINK_OR_NUMBER)
            money -> QuestionRules.Decision(Level.CAREFUL, ReasonCode.ASKS_FOR_MONEY_OR_LINK)
            else -> QuestionRules.Decision(Level.NONE, null)
        }
    }

    /**
     * Mode F: what the model's CAREFUL becomes. Kept only with something concrete to be careful
     * about (a link, a number, a code or money request, a "new number" claim); otherwise NONE,
     * so "please bring rice and drinks" in a busy group stays silent. The safety floor still applies.
     */
    fun guardCareful(signs: Set<Sign>): QuestionRules.Decision =
        if (signs.any { it in CAREFUL_SIGNS }) QuestionRules.Decision(Level.CAREFUL, carefulReason(signs))
        else QuestionRules.Decision(Level.NONE, null)

    private val CAREFUL_SIGNS = setOf(
        Sign.LINK, Sign.PHONE, Sign.CODE_REQUEST, Sign.MONEY_REQUEST, Sign.NEW_NUMBER, Sign.INVESTMENT_RETURN, Sign.CALL_ABOUT_ACCOUNT,
    )

    /**
     * Signs strong enough to make a message at least CAREFUL on their own, whatever the model
     * says or if it fails: a promised investment return, or "call this number" about account trouble.
     */
    fun floorReason(signs: Set<Sign>): ReasonCode? = when {
        Sign.CALL_ABOUT_ACCOUNT in signs -> ReasonCode.LINK_OR_NUMBER
        Sign.INVESTMENT_RETURN in signs -> ReasonCode.TOO_GOOD_OFFER
        else -> null
    }

    /** Mode F: a fixed reason for the model's CAREFUL. */
    fun carefulReason(signs: Set<Sign>): ReasonCode = when {
        Sign.MONEY_REQUEST in signs -> ReasonCode.ASKS_FOR_MONEY_OR_LINK
        Sign.LINK in signs || Sign.PHONE in signs -> ReasonCode.LINK_OR_NUMBER
        else -> ReasonCode.WARNING_SIGNS
    }
}
