package com.ajayinnovations.argus.alert

import android.content.Context
import androidx.annotation.StringRes
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.ReasonCode
import com.ajayinnovations.argus.core.Verdict

/** Fixed English/Hindi sentences for each rule reason. */
object ReasonText {

    @StringRes
    fun reason(code: ReasonCode): Int = when (code) {
        ReasonCode.ASKS_FOR_CODE -> R.string.reason_asks_for_code
        ReasonCode.ACCOUNT_PROBLEM -> R.string.reason_account_problem
        ReasonCode.TOO_GOOD_OFFER -> R.string.reason_too_good_offer
        ReasonCode.FAKE_OFFICIAL -> R.string.reason_fake_official
        ReasonCode.ASKS_FOR_MONEY_OR_LINK -> R.string.reason_asks_for_money_or_link
        ReasonCode.PRESSURE -> R.string.reason_pressure
        ReasonCode.ACCOUNT_OR_OFFER -> R.string.reason_account_or_offer
        ReasonCode.MONEY_UNDER_PRESSURE -> R.string.reason_money_under_pressure
        ReasonCode.LINK_OR_NUMBER -> R.string.reason_link_or_number
        ReasonCode.WARNING_SIGNS -> R.string.reason_warning_signs
    }

    @StringRes
    fun advice(code: ReasonCode): Int = when (code) {
        ReasonCode.ASKS_FOR_CODE -> R.string.advice_asks_for_code
        ReasonCode.ACCOUNT_PROBLEM -> R.string.advice_account_problem
        ReasonCode.TOO_GOOD_OFFER -> R.string.advice_too_good_offer
        ReasonCode.FAKE_OFFICIAL -> R.string.advice_fake_official
        ReasonCode.ASKS_FOR_MONEY_OR_LINK -> R.string.advice_asks_for_money_or_link
        ReasonCode.PRESSURE -> R.string.advice_pressure
        ReasonCode.ACCOUNT_OR_OFFER -> R.string.advice_account_or_offer
        ReasonCode.MONEY_UNDER_PRESSURE -> R.string.advice_money_under_pressure
        ReasonCode.LINK_OR_NUMBER -> R.string.advice_link_or_number
        ReasonCode.WARNING_SIGNS -> R.string.advice_warning_signs
    }

    /** The fixed reason when the rules decided, otherwise the model-written one (classify mode). */
    fun reasonOf(context: Context, verdict: Verdict): String =
        verdict.reasonCode?.let { context.getString(reason(it)) } ?: verdict.reason
}
