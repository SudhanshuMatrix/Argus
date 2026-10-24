package com.ajayinnovations.argus.core

/** Which notification channel an alert uses. SCAM interrupts; CAREFUL is quiet. */
enum class AlertChannel { SCAM, CAREFUL }

/** How strongly one level alerts. The table lives in docs/PLAN.md, Step 5. */
data class AlertPlan(
    val channel: AlertChannel,
    /** Full-screen red warning, also over the lock screen. */
    val fullScreen: Boolean,
    /** Strong vibration even when the phone is on silent. */
    val alwaysVibrate: Boolean,
    /** Family SMS, when turned on in Settings. */
    val familySms: Boolean,
) {
    companion object {
        /** Null for NONE: no notification, no sound, nothing. */
        fun forLevel(level: Level): AlertPlan? = when (level) {
            Level.SCAM -> AlertPlan(AlertChannel.SCAM, fullScreen = true, alwaysVibrate = true, familySms = true)
            Level.CAREFUL, Level.OTP -> AlertPlan(AlertChannel.CAREFUL, fullScreen = false, alwaysVibrate = false, familySms = false)
            Level.NONE -> null
        }
    }
}
