package com.ajayinnovations.argus.core

/** The wizard's screens, in order. Not every phone gets every step. */
enum class SetupStep {
    PHONE_CHECK,
    GETTING_READY,
    WHO,
    SMS,
    NOTIFICATIONS,
    NOTIFICATION_ACCESS,
    RESTRICTED_SETTINGS,
    FULL_SCREEN,
    /** Recommended, skippable: lets the SCAM warning cover the screen while the phone is in use. */
    OVERLAY,
    BATTERY,
    MAKER_EXTRA,
    TEST,
}

/** Phone makers whose battery savers need extra steps. */
enum class Maker {
    /** Tecno, Infinix, itel: Auto-start. */
    TRANSSION,
    /** Xiaomi, Redmi, POCO: Auto-start, show on lock screen, pop-up windows. */
    XIAOMI,
    OTHER;

    companion object {
        fun from(manufacturer: String, brand: String): Maker {
            val names = "$manufacturer $brand".lowercase()
            return when {
                listOf("tecno", "infinix", "itel", "transsion").any { it in names } -> TRANSSION
                listOf("xiaomi", "redmi", "poco").any { it in names } -> XIAOMI
                else -> OTHER
            }
        }
    }
}

/**
 * Small facts the wizard remembers across process deaths (the parent's helper may spend a
 * while in Android's settings). No message content.
 */
enum class SetupMark {
    PHONE_CHECKED,
    MODEL_WORKS,
    WHO_DONE,
    ACCESS_ATTEMPTED,
    /** Shown once, so the step count doesn't change when access is later granted. */
    RESTRICTED_SHOWN,
    /** canUseFullScreenIntent() was false once: keep the step in the list. */
    FULL_SCREEN_NEEDED,
    MAKER_DONE,
    TEST_DONE,
    /** "Not now" on the over-other-apps step: setup goes on without it. */
    OVERLAY_SKIPPED,
}

/** Everything the wizard needs to know, read fresh on every resume. */
data class SetupFacts(
    val sdkInt: Int,
    val support: Support,
    val modelReady: Boolean,
    val familyAlerts: Boolean,
    val receiveSmsGranted: Boolean,
    val sendSmsGranted: Boolean,
    val notificationsGranted: Boolean,
    val accessGranted: Boolean,
    /** Not installed from the Play Store: Android 13+ may block notification access. */
    val sideloaded: Boolean,
    val fullScreenAllowed: Boolean,
    val batteryIgnored: Boolean,
    /** "Display over other apps" is allowed. */
    val overlayAllowed: Boolean = false,
    val maker: Maker,
    val marks: Set<SetupMark>,
)

object SetupFlow {

    const val SDK_TIRAMISU = 33
    const val SDK_UPSIDE_DOWN_CAKE = 34

    /** The steps this phone gets, in order. */
    fun steps(f: SetupFacts): List<SetupStep> = SetupStep.entries.filter { applies(it, f) }

    fun applies(step: SetupStep, f: SetupFacts): Boolean = when (step) {
        SetupStep.NOTIFICATIONS -> f.sdkInt >= SDK_TIRAMISU
        SetupStep.RESTRICTED_SETTINGS -> restrictedNeeded(f)
        SetupStep.FULL_SCREEN -> f.sdkInt >= SDK_UPSIDE_DOWN_CAKE &&
            (!f.fullScreenAllowed || SetupMark.FULL_SCREEN_NEEDED in f.marks)
        SetupStep.MAKER_EXTRA -> f.maker != Maker.OTHER
        else -> true
    }

    fun isDone(step: SetupStep, f: SetupFacts): Boolean = when (step) {
        SetupStep.PHONE_CHECK -> f.support != Support.NOT_SUPPORTED && SetupMark.PHONE_CHECKED in f.marks
        SetupStep.GETTING_READY -> f.modelReady && SetupMark.MODEL_WORKS in f.marks
        SetupStep.WHO -> SetupMark.WHO_DONE in f.marks
        SetupStep.SMS -> f.receiveSmsGranted && (!f.familyAlerts || f.sendSmsGranted)
        SetupStep.NOTIFICATIONS -> f.notificationsGranted
        // Passed on to the next screen when Android seems to have blocked it
        SetupStep.NOTIFICATION_ACCESS -> f.accessGranted || restrictedNeeded(f)
        SetupStep.RESTRICTED_SETTINGS -> f.accessGranted
        SetupStep.FULL_SCREEN -> f.fullScreenAllowed
        SetupStep.OVERLAY -> f.overlayAllowed || SetupMark.OVERLAY_SKIPPED in f.marks
        SetupStep.BATTERY -> f.batteryIgnored
        SetupStep.MAKER_EXTRA -> SetupMark.MAKER_DONE in f.marks
        SetupStep.TEST -> SetupMark.TEST_DONE in f.marks
    }

    /** The first step not done yet; null when setup is finished. */
    fun current(f: SetupFacts): SetupStep? = steps(f).firstOrNull { !isDone(it, f) }

    /** Android 13+ blocks notification access for some sideloaded apps until "Allow restricted settings". */
    private fun restrictedNeeded(f: SetupFacts): Boolean =
        SetupMark.RESTRICTED_SHOWN in f.marks ||
            (f.sdkInt >= SDK_TIRAMISU && f.sideloaded && SetupMark.ACCESS_ATTEMPTED in f.marks && !f.accessGranted)
}
