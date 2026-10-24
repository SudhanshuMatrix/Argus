package com.ajayinnovations.argus.core

/** Spots a genuine one-time code so the parent gets "never share this code", not a scam alert. */
object OtpCheck {

    private val code = Regex("""\b\d{4,8}\b""")
    private val otpWords = Regex("""\b(otp|one[- ]time|verification code|token)\b""", RegexOption.IGNORE_CASE)
    private val askWords = Regex("""\b(send|share|give|forward|tell)\b""", RegexOption.IGNORE_CASE)

    /** Real banks say "Do not share this code" / "Never give it to anyone": that is not a request. */
    private val negatedAsk = Regex(
        """\b(do not|don't|dont|never|not to|no)\s+(send|share|give|forward|tell|disclose)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** A genuine OTP: a code and OTP words, but no link and nobody asking for it. */
    fun isRealOtp(text: String): Boolean {
        if (!otpWords.containsMatchIn(text) || !code.containsMatchIn(text)) return false
        if (text.contains("http", ignoreCase = true) || text.contains("www.", ignoreCase = true)) return false
        return !askWords.containsMatchIn(text.replace(negatedAsk, " "))
    }
}
