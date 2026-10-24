package com.ajayinnovations.argus.core

/** What setup needs to know about the phone. Read on Android, judged here. */
data class DeviceSpecs(
    /** True when the phone can run 64-bit ARM code (arm64-v8a); LiteRT-LM ships only that. */
    val supportsArm64: Boolean,
    /** Total RAM as Android reports it (a "4 GB" phone reports about 3.6 GB). */
    val totalRamBytes: Long,
    /** Free space in app storage. */
    val freeStorageBytes: Long,
    /** Space the model still needs; 0 once it is ready. */
    val storageNeededBytes: Long,
    val sdkInt: Int,
)

enum class Support { SUPPORTED, SLOW, NOT_SUPPORTED }

/** Each problem maps to one plain-language sentence in string resources. */
enum class DeviceIssue(val blocking: Boolean) {
    NOT_64_BIT(blocking = true),
    RAM_TOO_LOW(blocking = true),
    STORAGE_TOO_LOW(blocking = true),
    RAM_LOW(blocking = false),
    OLD_ANDROID(blocking = false),
}

data class DeviceCheckResult(val support: Support, val issues: List<DeviceIssue>)

object DeviceCheck {

    private const val GIB = 1024L * 1024 * 1024

    /** Below this (2 GB and 3 GB phones report under 2.8 GB), Gemma 3 1B can't stay loaded. */
    const val MIN_RAM_BYTES = 2_800L * 1024 * 1024
    /** Below this ("4 GB" phones report about 3.6 GB), it works but is slow and gets unloaded often. */
    const val GOOD_RAM_BYTES = 3_400L * 1024 * 1024
    /** Android 10 (API 29): older phones usually have old GPUs and slow storage. */
    const val GOOD_SDK = 29

    fun evaluate(specs: DeviceSpecs): DeviceCheckResult {
        val issues = buildList {
            if (!specs.supportsArm64) add(DeviceIssue.NOT_64_BIT)
            when {
                specs.totalRamBytes < MIN_RAM_BYTES -> add(DeviceIssue.RAM_TOO_LOW)
                specs.totalRamBytes < GOOD_RAM_BYTES -> add(DeviceIssue.RAM_LOW)
            }
            if (specs.freeStorageBytes < specs.storageNeededBytes) add(DeviceIssue.STORAGE_TOO_LOW)
            if (specs.sdkInt < GOOD_SDK) add(DeviceIssue.OLD_ANDROID)
        }
        val support = when {
            issues.any { it.blocking } -> Support.NOT_SUPPORTED
            issues.isNotEmpty() -> Support.SLOW
            else -> Support.SUPPORTED
        }
        return DeviceCheckResult(support, issues)
    }

    /** Rounds bytes up to whole MB for "Free up about X MB" messages. */
    fun shortfallMb(specs: DeviceSpecs): Long {
        val missing = specs.storageNeededBytes - specs.freeStorageBytes
        return if (missing <= 0) 0 else (missing + (1L shl 20) - 1) shr 20
    }

    fun gib(bytes: Long): Double = bytes.toDouble() / GIB

    /** "Free up about X GB": the shortfall rounded up to the next 0.1 GB, at least 0.1. */
    fun freeUpGb(missingBytes: Long): Double {
        if (missingBytes <= 0) return 0.0
        val tenths = (missingBytes * 10 + GIB - 1) / GIB
        return maxOf(1L, tenths) / 10.0
    }

    fun shortfallGb(specs: DeviceSpecs): Double = freeUpGb(specs.storageNeededBytes - specs.freeStorageBytes)
}
