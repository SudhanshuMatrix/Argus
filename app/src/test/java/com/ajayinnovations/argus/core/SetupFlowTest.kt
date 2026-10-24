package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupFlowTest {

    private val fresh = SetupFacts(
        sdkInt = 36, support = Support.SUPPORTED, modelReady = false, familyAlerts = false,
        receiveSmsGranted = false, sendSmsGranted = false, notificationsGranted = false,
        accessGranted = false, sideloaded = true, fullScreenAllowed = true, batteryIgnored = false,
        maker = Maker.OTHER, marks = emptySet(),
    )

    private fun SetupFacts.mark(vararg m: SetupMark) = copy(marks = marks + m)

    private val allDone = fresh.copy(
        modelReady = true, receiveSmsGranted = true, notificationsGranted = true, accessGranted = true, batteryIgnored = true,
        overlayAllowed = true,
    ).mark(SetupMark.PHONE_CHECKED, SetupMark.MODEL_WORKS, SetupMark.WHO_DONE, SetupMark.TEST_DONE)

    @Test
    fun `starts at the phone check`() = assertEquals(SetupStep.PHONE_CHECK, SetupFlow.current(fresh))

    @Test
    fun `an unsupported phone stays on the phone check`() =
        assertEquals(SetupStep.PHONE_CHECK, SetupFlow.current(fresh.copy(support = Support.NOT_SUPPORTED).mark(SetupMark.PHONE_CHECKED)))

    @Test
    fun `a slow phone continues once seen`() =
        assertEquals(SetupStep.GETTING_READY, SetupFlow.current(fresh.copy(support = Support.SLOW).mark(SetupMark.PHONE_CHECKED)))

    @Test
    fun `getting ready needs the file and a working model`() {
        val f = fresh.mark(SetupMark.PHONE_CHECKED).copy(modelReady = true)
        assertEquals(SetupStep.GETTING_READY, SetupFlow.current(f))
        assertEquals(SetupStep.WHO, SetupFlow.current(f.mark(SetupMark.MODEL_WORKS)))
    }

    @Test
    fun `Pixel on Android 16 with no special maker`() =
        assertEquals(
            listOf(
                SetupStep.PHONE_CHECK, SetupStep.GETTING_READY, SetupStep.WHO, SetupStep.SMS, SetupStep.NOTIFICATIONS,
                SetupStep.NOTIFICATION_ACCESS, SetupStep.OVERLAY, SetupStep.BATTERY, SetupStep.TEST,
            ),
            SetupFlow.steps(fresh),
        )

    @Test
    fun `Android 12 has no notifications or full-screen step`() {
        val steps = SetupFlow.steps(fresh.copy(sdkInt = 31, fullScreenAllowed = false))
        assertFalse(SetupStep.NOTIFICATIONS in steps)
        assertFalse(SetupStep.FULL_SCREEN in steps)
    }

    @Test
    fun `full-screen step only when not allowed, then it stays`() {
        assertFalse(SetupStep.FULL_SCREEN in SetupFlow.steps(fresh))
        assertTrue(SetupStep.FULL_SCREEN in SetupFlow.steps(fresh.copy(fullScreenAllowed = false)))
        assertTrue(SetupStep.FULL_SCREEN in SetupFlow.steps(fresh.mark(SetupMark.FULL_SCREEN_NEEDED)))
    }

    @Test
    fun `family alerts also need SEND_SMS`() {
        val f = allDone.copy(familyAlerts = true)
        assertEquals(SetupStep.SMS, SetupFlow.current(f))
        assertNull(SetupFlow.current(f.copy(sendSmsGranted = true)))
    }

    @Test
    fun `blocked notification access leads to the restricted-settings screen`() {
        val beforeAccess = allDone.copy(accessGranted = false)
        assertEquals(SetupStep.NOTIFICATION_ACCESS, SetupFlow.current(beforeAccess))
        val blocked = beforeAccess.mark(SetupMark.ACCESS_ATTEMPTED)
        assertEquals(SetupStep.RESTRICTED_SETTINGS, SetupFlow.current(blocked))
        // Once granted, the screen is done but stays in the count if it was shown
        val granted = blocked.mark(SetupMark.RESTRICTED_SHOWN).copy(accessGranted = true)
        assertTrue(SetupStep.RESTRICTED_SETTINGS in SetupFlow.steps(granted))
        assertNull(SetupFlow.current(granted))
    }

    @Test
    fun `Play Store installs and Android 12 never see the restricted screen`() {
        val attempted = allDone.copy(accessGranted = false).mark(SetupMark.ACCESS_ATTEMPTED)
        assertEquals(SetupStep.NOTIFICATION_ACCESS, SetupFlow.current(attempted.copy(sideloaded = false)))
        assertEquals(SetupStep.NOTIFICATION_ACCESS, SetupFlow.current(attempted.copy(sdkInt = 32)))
    }

    @Test
    fun `maker screen for Tecno and Xiaomi only`() {
        assertTrue(SetupStep.MAKER_EXTRA in SetupFlow.steps(fresh.copy(maker = Maker.TRANSSION)))
        assertEquals(SetupStep.MAKER_EXTRA, SetupFlow.current(allDone.copy(maker = Maker.XIAOMI).copy(marks = allDone.marks - SetupMark.TEST_DONE)))
    }

    @Test
    fun `test is last, then finished`() {
        assertEquals(SetupStep.TEST, SetupFlow.current(allDone.copy(marks = allDone.marks - SetupMark.TEST_DONE)))
        assertNull(SetupFlow.current(allDone))
    }

    @Test
    fun `makers from Build values`() {
        assertEquals(Maker.TRANSSION, Maker.from("TECNO MOBILE LIMITED", "TECNO"))
        assertEquals(Maker.TRANSSION, Maker.from("INFINIX MOBILITY LIMITED", "Infinix"))
        assertEquals(Maker.TRANSSION, Maker.from("itel", "itel"))
        assertEquals(Maker.XIAOMI, Maker.from("Xiaomi", "Redmi"))
        assertEquals(Maker.XIAOMI, Maker.from("Xiaomi", "POCO"))
        assertEquals(Maker.OTHER, Maker.from("Google", "google"))
        assertEquals(Maker.OTHER, Maker.from("samsung", "samsung"))
    }

    @Test
    fun `over other apps is recommended but skippable`() {
        val atOverlay = allDone.copy(overlayAllowed = false).let { it.copy(marks = it.marks - SetupMark.TEST_DONE) }
        assertEquals(SetupStep.OVERLAY, SetupFlow.current(atOverlay))
        assertEquals(SetupStep.TEST, SetupFlow.current(atOverlay.mark(SetupMark.OVERLAY_SKIPPED)))
        assertEquals(SetupStep.TEST, SetupFlow.current(atOverlay.copy(overlayAllowed = true)))
    }

    @Test
    fun `skipping over other apps still finishes setup`() {
        assertEquals(null, SetupFlow.current(allDone.mark(SetupMark.OVERLAY_SKIPPED)))
    }
}
