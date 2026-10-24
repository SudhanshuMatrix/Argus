package com.ajayinnovations.argus.alert

import android.content.Context
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.AccountTrouble
import com.ajayinnovations.argus.core.Finding
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.ReasonCode
import com.ajayinnovations.argus.core.Secret
import com.ajayinnovations.argus.core.SourceType

/**
 * The fixed words for each level, from string resources in the context's language. Shared by
 * the notification, the warning screen and the check result. Never the message or its sender.
 */
object AlertText {

    /** "Mummy", "Daddy", "Grandma", or their own name; null for the neutral title without a name. */
    fun titleName(context: Context, title: ParentTitle, customName: String = ""): String? = when (title) {
        ParentTitle.MUMMY -> context.getString(R.string.title_mummy)
        ParentTitle.DADDY -> context.getString(R.string.title_daddy)
        ParentTitle.GRANDMA -> context.getString(R.string.title_grandma)
        ParentTitle.NEUTRAL -> customName.trim().ifEmpty { null }
    }

    /** The family member's name, or "your family" when setup left it empty. */
    fun familyName(context: Context, name: String): String =
        name.ifBlank { context.getString(R.string.your_family) }

    fun headline(context: Context, level: Level, title: ParentTitle, customName: String = ""): String {
        val name = titleName(context, title, customName)
        return when (level) {
            Level.SCAM -> name?.let { context.getString(R.string.alert_scam_headline_named, it) }
                ?: context.getString(R.string.alert_scam_headline)
            Level.CAREFUL -> name?.let { context.getString(R.string.alert_careful_headline_named, it) }
                ?: context.getString(R.string.alert_careful_headline)
            Level.OTP -> context.getString(R.string.alert_otp_headline)
            Level.NONE -> context.getString(R.string.alert_none_headline)
        }
    }

    /** What to do, the same every time for a level. */
    fun body(context: Context, level: Level, familyName: String): String {
        val family = familyName(context, familyName)
        return when (level) {
            Level.SCAM -> context.getString(R.string.alert_scam_body)
            Level.CAREFUL -> context.getString(R.string.alert_careful_body, family)
            Level.OTP -> context.getString(R.string.alert_otp_body)
            Level.NONE -> context.getString(R.string.alert_none_body, family)
        }
    }

    /**
     * Built from what the rules found ("It has a link and says your account is blocked."), else
     * the fixed reason for the rule that decided, else the model's (classify mode B only), else null.
     */
    fun reason(context: Context, findings: List<Finding>, code: ReasonCode?, modelReason: String): String? =
        fromFindings(context, findings)
            ?: code?.let { context.getString(ReasonText.reason(it)) }
            ?: modelReason.takeIf { it.isNotBlank() }

    /** "From: Tunde (WhatsApp)"; null when the sender isn't known. */
    fun from(context: Context, source: SourceType?, sender: String?): String? {
        if (sender.isNullOrBlank() || source == null) return null
        return when (source) {
            SourceType.SMS -> context.getString(R.string.from_sms, sender)
            SourceType.WHATSAPP -> context.getString(R.string.from_whatsapp, sender)
        }
    }

    /** The two most important findings, joined into one fixed sentence. */
    fun fromFindings(context: Context, findings: List<Finding>): String? {
        val phrases = findings.take(2).map { phrase(context, it) }
        return when (phrases.size) {
            0 -> null
            1 -> context.getString(R.string.reason_one, phrases[0])
            else -> context.getString(R.string.reason_two, phrases[0], phrases[1])
        }
    }

    private fun phrase(context: Context, finding: Finding): String = when (finding) {
        is Finding.AsksForSecret -> context.getString(R.string.finding_asks_for, secret(context, finding.secret))
        is Finding.AccountProblem -> context.getString(R.string.finding_account, context.getString(trouble(finding.trouble)))
        Finding.Link -> context.getString(R.string.finding_link)
        Finding.VerifyAccount -> context.getString(R.string.finding_verify)
        Finding.NewNumber -> context.getString(R.string.finding_new_number)
        Finding.Prize -> context.getString(R.string.finding_prize)
        Finding.MoneyRequest -> context.getString(R.string.finding_money)
        Finding.OfficialClaim -> context.getString(R.string.finding_official)
        Finding.PhoneNumber -> context.getString(R.string.finding_phone)
        Finding.Urgency -> context.getString(R.string.finding_urgency)
    }

    // BVN, NIN, OTP and PIN stay as these Latin acronyms in English and Hindi
    private fun secret(context: Context, secret: Secret): String = when (secret) {
        Secret.BVN -> "BVN"
        Secret.NIN -> "NIN"
        Secret.OTP -> "OTP"
        Secret.PIN -> "PIN"
        Secret.PASSWORD -> context.getString(R.string.secret_password)
        Secret.CARD -> context.getString(R.string.secret_card)
        Secret.CODE -> context.getString(R.string.secret_code)
    }

    private fun trouble(trouble: AccountTrouble): Int = when (trouble) {
        AccountTrouble.BLOCKED -> R.string.trouble_blocked
        AccountTrouble.SUSPENDED -> R.string.trouble_suspended
        AccountTrouble.RESTRICTED -> R.string.trouble_restricted
        AccountTrouble.CLOSED -> R.string.trouble_closed
    }

    fun advice(context: Context, code: ReasonCode?, modelAdvice: String): String? =
        code?.let { context.getString(ReasonText.advice(it)) } ?: modelAdvice.takeIf { it.isNotBlank() }
}
