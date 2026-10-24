package com.ajayinnovations.argus.core

/** What kind of secret a message asks for. */
enum class Secret { BVN, NIN, OTP, PIN, PASSWORD, CARD, CODE }

/** The account trouble a message claims. */
enum class AccountTrouble { BLOCKED, SUSPENDED, RESTRICTED, CLOSED }

/**
 * Concrete things found in a message. The app turns the most important two into a fixed
 * English or Hindi sentence ("It has a link and says your account is blocked"), so the
 * parent sees why without any model-written words.
 */
sealed interface Finding {
    data class AsksForSecret(val secret: Secret) : Finding
    data object Link : Finding
    data class AccountProblem(val trouble: AccountTrouble) : Finding
    data object VerifyAccount : Finding
    data object NewNumber : Finding
    data object Prize : Finding
    data object MoneyRequest : Finding
    data object OfficialClaim : Finding
    data object PhoneNumber : Finding
    data object Urgency : Finding
}

object Findings {

    private fun words(vararg w: String) = Regex("""\b(${w.joinToString("|")})\b""", RegexOption.IGNORE_CASE)

    private val trouble = listOf(
        AccountTrouble.BLOCKED to words("blocked", "block"),
        AccountTrouble.SUSPENDED to words("suspended", "suspend"),
        AccountTrouble.RESTRICTED to words("restricted", "restrict"),
        AccountTrouble.CLOSED to words("deactivated", "closed", "disabled"),
    )
    private val verify = words("verify", "verification", "update your account", "update your bvn", "update your details", "upgrade")
    private val prize = words("you have won", "you won", "winner", "prize", "promo", "congratulations", "job offer", "loan", "grant", "investment", "double your")
    private val secretWords = listOf(
        Secret.BVN to words("bvn"), Secret.NIN to words("nin"), Secret.OTP to words("otp", "token"),
        Secret.PIN to words("pin"), Secret.PASSWORD to words("password"),
        Secret.CARD to words("card number", "card details", "cvv"), Secret.CODE to words("code"),
    )

    /** In order of importance; the reason uses the first two. Empty when nothing was found. */
    fun find(text: String): List<Finding> {
        val signs = ConcreteSigns.find(text)
        return buildList {
            secret(text, Sign.CODE_REQUEST in signs)?.let { add(Finding.AsksForSecret(it)) }
            if (Sign.LINK in signs) add(Finding.Link)
            trouble.firstOrNull { it.second.containsMatchIn(text) }?.let { add(Finding.AccountProblem(it.first)) }
            if (verify.containsMatchIn(text)) add(Finding.VerifyAccount)
            if (Sign.NEW_NUMBER in signs) add(Finding.NewNumber)
            if (prize.containsMatchIn(text) || Sign.INVESTMENT_RETURN in signs) add(Finding.Prize)
            if (Sign.MONEY_REQUEST in signs) add(Finding.MoneyRequest)
            if (Sign.OFFICIAL_CLAIM in signs) add(Finding.OfficialClaim)
            if (Sign.PHONE in signs) add(Finding.PhoneNumber)
            if (Sign.URGENCY in signs) add(Finding.Urgency)
        }
    }

    /** The secret asked for: the hard rule's exact one, else any secret word in a looser request. */
    private fun secret(text: String, looseRequest: Boolean): Secret? {
        HardRules.requestedSecret(text)?.let { word -> return secretWords.first { it.second.matches(word) }.first }
        if (!looseRequest) return null
        return secretWords.firstOrNull { it.second.containsMatchIn(text) }?.first
    }
}
