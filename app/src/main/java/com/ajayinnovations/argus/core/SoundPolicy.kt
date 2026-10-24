package com.ajayinnovations.argus.core

/** The phone's ringer switch. */
enum class Ringer { NORMAL, VIBRATE, SILENT }

/**
 * Silent mode is respected by default: the voice stays quiet unless the ringer is on or the
 * "Speak warnings even on silent" setting is on. A SCAM always vibrates and shows the red screen.
 */
object SoundPolicy {

    /** [userAsked]: the parent tapped "Hear it again", so play whatever the ringer says. */
    fun shouldSpeak(ringer: Ringer, speakOnSilent: Boolean, userAsked: Boolean = false): Boolean =
        userAsked || ringer == Ringer.NORMAL || speakOnSilent

    /**
     * The scam channel vibrates on its own when the ringer is on or on vibrate. On silent, or
     * when the notification couldn't be posted at all, the app vibrates the phone itself.
     */
    fun vibrateDirectly(plan: AlertPlan, ringer: Ringer, notificationPosted: Boolean): Boolean =
        plan.alwaysVibrate && (ringer == Ringer.SILENT || !notificationPosted)
}
