package com.ajayinnovations.argus.core

import java.util.Locale

/** One test phone's facts and timings. Has no field that could hold message text. */
data class DeviceReport(
    val appVersion: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val totalRamBytes: Long,
    val abis: List<String>,
    val support: Support,
    val issues: List<DeviceIssue>,
    /** "GPU", "CPU", or null if the model hasn't loaded yet. */
    val backend: String?,
    val useGpu: Boolean,
    val gpuDisabled: Boolean,
    val loadMillis: Long?,
    val checkCount: Int,
    val averageCheckMillis: Long?,
) {
    fun format(): String = buildString {
        appendLine("Argus device report")
        appendLine("App: $appVersion")
        appendLine("Phone: $manufacturer $model")
        appendLine("Android: $androidVersion (SDK $sdkInt)")
        appendLine("RAM: ${String.format(Locale.US, "%.1f", DeviceCheck.gib(totalRamBytes))} GB")
        appendLine("ABIs: ${abis.joinToString()}")
        appendLine("Device check: $support${if (issues.isEmpty()) "" else " (${issues.joinToString()})"}")
        appendLine("Backend: ${backend ?: "not loaded"}${if (useGpu) " (Use GPU on)" else ""}${if (gpuDisabled) " (GPU disabled after a failure)" else ""}")
        appendLine("Model load: ${loadMillis?.let(::seconds) ?: "not loaded"}")
        append("Checks: $checkCount, average ${averageCheckMillis?.let(::seconds) ?: "n/a"}")
    }

    private fun seconds(millis: Long) = String.format(Locale.US, "%.1f s", millis / 1000.0)
}
