package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCheckTest {

    private val mb = 1024L * 1024
    private val good = DeviceSpecs(
        supportsArm64 = true,
        totalRamBytes = 7_500 * mb,
        freeStorageBytes = 3_000 * mb,
        storageNeededBytes = 800 * mb,
        sdkInt = 36,
    )

    @Test
    fun `a modern phone is supported with no issues`() {
        val result = DeviceCheck.evaluate(good)
        assertEquals(Support.SUPPORTED, result.support)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `32-bit only phone is not supported`() {
        val result = DeviceCheck.evaluate(good.copy(supportsArm64 = false))
        assertEquals(Support.NOT_SUPPORTED, result.support)
        assertEquals(listOf(DeviceIssue.NOT_64_BIT), result.issues)
    }

    @Test
    fun `2 GB phone is not supported`() {
        val result = DeviceCheck.evaluate(good.copy(totalRamBytes = 1_850 * mb))
        assertEquals(Support.NOT_SUPPORTED, result.support)
        assertEquals(listOf(DeviceIssue.RAM_TOO_LOW), result.issues)
    }

    @Test
    fun `3 GB phone is not supported`() {
        // A "3 GB" phone reports about 2.7 GB
        val result = DeviceCheck.evaluate(good.copy(totalRamBytes = 2_750 * mb))
        assertEquals(Support.NOT_SUPPORTED, result.support)
    }

    @Test
    fun `4 GB phone is supported but slow`() {
        // A "4 GB" phone reports about 3.6 GB; some report a little less
        val result = DeviceCheck.evaluate(good.copy(totalRamBytes = 3_300 * mb))
        assertEquals(Support.SLOW, result.support)
        assertEquals(listOf(DeviceIssue.RAM_LOW), result.issues)
    }

    @Test
    fun `typical 4 GB report is supported`() {
        val result = DeviceCheck.evaluate(good.copy(totalRamBytes = 3_650 * mb))
        assertEquals(Support.SUPPORTED, result.support)
    }

    @Test
    fun `not enough storage is not supported`() {
        val result = DeviceCheck.evaluate(good.copy(freeStorageBytes = 500 * mb))
        assertEquals(Support.NOT_SUPPORTED, result.support)
        assertEquals(listOf(DeviceIssue.STORAGE_TOO_LOW), result.issues)
    }

    @Test
    fun `storage is fine once the model is ready`() {
        val result = DeviceCheck.evaluate(good.copy(freeStorageBytes = 10 * mb, storageNeededBytes = 0))
        assertEquals(Support.SUPPORTED, result.support)
    }

    @Test
    fun `Android 9 is supported but slow`() {
        val result = DeviceCheck.evaluate(good.copy(sdkInt = 28))
        assertEquals(Support.SLOW, result.support)
        assertEquals(listOf(DeviceIssue.OLD_ANDROID), result.issues)
    }

    @Test
    fun `all issues are reported, blocking wins`() {
        val result = DeviceCheck.evaluate(
            good.copy(supportsArm64 = false, totalRamBytes = 3_000 * mb, freeStorageBytes = 0, sdkInt = 26),
        )
        assertEquals(Support.NOT_SUPPORTED, result.support)
        assertEquals(
            listOf(DeviceIssue.NOT_64_BIT, DeviceIssue.RAM_LOW, DeviceIssue.STORAGE_TOO_LOW, DeviceIssue.OLD_ANDROID),
            result.issues,
        )
    }

    @Test
    fun `shortfall rounds up to whole MB`() {
        assertEquals(0, DeviceCheck.shortfallMb(good))
        assertEquals(301, DeviceCheck.shortfallMb(good.copy(freeStorageBytes = 500 * mb - 1)))
    }

    @Test
    fun `free up GB rounds up to a tenth`() {
        val gib = 1024L * 1024 * 1024
        assertEquals(0.0, DeviceCheck.freeUpGb(0), 0.0)
        assertEquals(0.1, DeviceCheck.freeUpGb(1), 0.0)
        assertEquals(1.0, DeviceCheck.freeUpGb(gib), 0.0)
        assertEquals(1.1, DeviceCheck.freeUpGb(gib + 1), 0.0)
    }
}
