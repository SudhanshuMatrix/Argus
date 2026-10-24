package com.ajayinnovations.argus.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.ajayinnovations.argus.ai.ModelCache
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.core.DeviceSpecs

/** Reads the phone facts DeviceCheck and the device report need. Never touches messages. */
class DeviceInfo(
    private val context: Context,
    private val models: ModelSource,
    private val cache: ModelCache,
) {
    val manufacturer: String get() = Build.MANUFACTURER.orEmpty()
    val model: String get() = Build.MODEL.orEmpty()
    val androidVersion: String get() = Build.VERSION.RELEASE.orEmpty()
    val sdkInt: Int get() = Build.VERSION.SDK_INT
    val abis: List<String> get() = Build.SUPPORTED_ABIS.toList()

    fun totalRamBytes(): Long {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        return info.totalMem
    }

    fun freeStorageBytes(): Long = context.filesDir.usableSpace

    fun specs(): DeviceSpecs = DeviceSpecs(
        supportsArm64 = ARM64 in abis,
        totalRamBytes = totalRamBytes(),
        freeStorageBytes = freeStorageBytes(),
        // The copy (bundled flavors) plus the weight cache the first load writes
        storageNeededBytes = models.bytesNeeded() + cache.bytesStillNeeded(models.modelSizeBytes() ?: 0),
        sdkInt = sdkInt,
    )

    private companion object {
        const val ARM64 = "arm64-v8a"
    }
}
