package com.ajayinnovations.argus.core

/**
 * Patterns that are SCAM whatever the model says. Checked before the model runs, so the alert
 * goes out at once. Kept narrow on purpose: every rule here can raise a loud alarm on its own.
 */
object HardRules {

    enum class Rule {
        /** A link plus BVN, NIN, blocked, restricted, suspended, verify or "update your account". */
        LINK_ACCOUNT,
        /** Someone asks you to send or share a code, OTP, PIN, BVN or password. */
        CODE_REQUEST,
        /** A money request from someone claiming a new number ("my friend line", "lost my phone"). */
        MONEY_NEW_NUMBER,
        /** A prize, promo, "you have won" or "congratulations" plus a fee, delivery charge or payment request. */
        PRIZE_FEE,
        /** A promised investment return ("invest N50k, get N150k", "double your money") plus a money request. */
        INVESTMENT_MONEY,
        /** "Call this number" about account or card trouble, from someone claiming to be a bank, customer care or official. */
        CALL_OFFICIAL,
    }

    private val accountWords = Regex(
        """\b(bvn|nin|blocked|restricted|suspended|verify|update your account)\b""",
        RegexOption.IGNORE_CASE,
    )

    // "enter"/"type" are not here: real OTP texts say "enter this code in the app" (ConcreteSigns counts them)
    private const val VERBS = "send|share|give|forward|tell|provide|drop|reply"
    private const val SECRETS = "code|otp|pin|bvn|password"

    /** A hand-over verb with the secret within a few words: "send me the OTP", "share your BVN". */
    private val codeRequest = Regex(
        """\b($VERBS)\b(\W+\w+){0,4}?\W+($SECRETS)s?\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Real banks say "do not share this code": that is a warning, not a request. */
    private val negated = Regex(
        """\b(do not|don't|dont|never|not to|no)\s+($VERBS|disclose)\b(\W+\w+){0,4}?\W+($SECRETS)s?\b""",
        RegexOption.IGNORE_CASE,
    )

    private val prize = Regex("""\b(prize|promo|you have won|you won|congratulations)\b""", RegexOption.IGNORE_CASE)
    private val fee = Regex("""\b(fees?|delivery charges?|charges?|processing|pay|payment|transfer)\b""", RegexOption.IGNORE_CASE)

    /** The first rule that fires, or null. */
    fun match(text: String): Rule? = when {
        codeRequest.containsMatchIn(text.replace(negated, " ")) -> Rule.CODE_REQUEST
        RedFlags.hasLink(text) && accountWords.containsMatchIn(text) -> Rule.LINK_ACCOUNT
        ConcreteSigns.find(text).containsAll(listOf(Sign.MONEY_REQUEST, Sign.NEW_NUMBER)) -> Rule.MONEY_NEW_NUMBER
        prize.containsMatchIn(text) && (fee.containsMatchIn(text) || Sign.MONEY_REQUEST in ConcreteSigns.find(text)) -> Rule.PRIZE_FEE
        else -> ConcreteSigns.find(text).let { signs ->
            when {
                Sign.INVESTMENT_RETURN in signs && Sign.MONEY_REQUEST in signs -> Rule.INVESTMENT_MONEY
                Sign.CALL_ABOUT_ACCOUNT in signs && Sign.OFFICIAL_CLAIM in signs -> Rule.CALL_OFFICIAL
                else -> null
            }
        }
    }

    /** Which secret is asked for, for the reason text ("It asks for your BVN"). */
    internal fun requestedSecret(text: String): String? =
        codeRequest.find(text.replace(negated, " "))?.groupValues?.get(3)?.lowercase()
}
