package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceReportTest {

    private val report = DeviceReport(
        appVersion = "1.0-dev",
        manufacturer = "Google",
        model = "Pixel 8",
        androidVersion = "16",
        sdkInt = 36,
        totalRamBytes = 7_739_420L * 1024,
        abis = listOf("arm64-v8a"),
        support = Support.SUPPORTED,
        issues = emptyList(),
        backend = "CPU",
        useGpu = false,
        gpuDisabled = false,
        loadMillis = 4_250,
        checkCount = 3,
        averageCheckMillis = 1_840,
    )

    @Test
    fun `formats a loaded phone`() {
        assertEquals(
            """
            Argus device report
            App: 1.0-dev
            Phone: Google Pixel 8
            Android: 16 (SDK 36)
            RAM: 7.4 GB
            ABIs: arm64-v8a
            Device check: SUPPORTED
            Backend: CPU
            Model load: 4.3 s
            Checks: 3, average 1.8 s
            """.trimIndent(),
            report.format(),
        )
    }

    @Test
    fun `formats a phone before the model loaded, with issues and flags`() {
        val text = report.copy(
            support = Support.SLOW,
            issues = listOf(DeviceIssue.RAM_LOW),
            backend = null,
            useGpu = true,
            gpuDisabled = true,
            loadMillis = null,
            checkCount = 0,
            averageCheckMillis = null,
        ).format()
        val lines = text.lines()
        assertEquals("Device check: SLOW (RAM_LOW)", lines[6])
        assertEquals("Backend: not loaded (Use GPU on) (GPU disabled after a failure)", lines[7])
        assertEquals("Model load: not loaded", lines[8])
        assertEquals("Checks: 0, average n/a", lines[9])
    }
}
