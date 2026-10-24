package com.ajayinnovations.argus.core

import kotlinx.serialization.Serializable

/** SCAM: loud alert. CAREFUL: quiet alert. OTP: "never share this code". NONE: stay silent. */
enum class Level { SCAM, CAREFUL, OTP, NONE }

/**
 * Why a rule fired. The app turns each into a fixed English or Hindi sentence from string
 * resources, so the parent never sees model-written text in this mode.
 */
enum class ReasonCode {
    ASKS_FOR_CODE,
    ACCOUNT_PROBLEM,
    TOO_GOOD_OFFER,
    FAKE_OFFICIAL,
    ASKS_FOR_MONEY_OR_LINK,
    PRESSURE,
    /** Account problem or a promised prize/job/loan, plus a request to act (mode E merges them). */
    ACCOUNT_OR_OFFER,
    /** Money asked for in a hurry, or from a "new number". */
    MONEY_UNDER_PRESSURE,
    /** A link or a phone number the reader is meant to use. */
    LINK_OR_NUMBER,
    /** Safety floor: the model failed or said NONE, but two or more red flags were found. */
    WARNING_SIGNS,
}

/**
 * [reason] and [advice] are model-written (classify mode only). [reasonCode] is set by the
 * rules (question mode and the safety floor); when present, show its fixed text instead.
 */
@Serializable
data class Verdict(
    val level: Level,
    val reason: String = "",
    val advice: String = "",
    val reasonCode: ReasonCode? = null,
)
