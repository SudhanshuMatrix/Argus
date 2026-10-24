package com.ajayinnovations.argus.device

import android.content.Context
import androidx.annotation.StringRes
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.DeviceCheck
import com.ajayinnovations.argus.core.DeviceIssue
import com.ajayinnovations.argus.core.DeviceSpecs
import com.ajayinnovations.argus.core.Support

/** Plain-language sentences for a DeviceCheck result, in the app's language. */
object DeviceCheckText {

    @StringRes
    fun headline(support: Support): Int = when (support) {
        Support.SUPPORTED -> R.string.device_supported
        Support.SLOW -> R.string.device_slow
        Support.NOT_SUPPORTED -> R.string.device_not_supported
    }

    fun reason(context: Context, issue: DeviceIssue, specs: DeviceSpecs): String = when (issue) {
        DeviceIssue.NOT_64_BIT -> context.getString(R.string.device_issue_not_64_bit)
        DeviceIssue.RAM_TOO_LOW -> context.getString(R.string.device_issue_ram_too_low)
        DeviceIssue.STORAGE_TOO_LOW ->
            context.getString(R.string.device_issue_storage_too_low, gb(DeviceCheck.shortfallGb(specs)))
        DeviceIssue.RAM_LOW -> context.getString(R.string.device_issue_ram_low)
        DeviceIssue.OLD_ANDROID -> context.getString(R.string.device_issue_old_android)
    }

    /** "1.2": one decimal, Western digits whatever the locale. */
    fun gb(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)
}
