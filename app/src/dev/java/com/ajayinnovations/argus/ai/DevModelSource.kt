package com.ajayinnovations.argus.ai

import android.content.Context
import java.io.File

/**
 * dev flavor: no bundled model. Reads the file pushed with
 * adb push model.litertlm /sdcard/Android/data/com.ajayinnovations.argus/files/model.litertlm
 */
class DevModelSource(private val context: Context) : ModelSource {

    private fun file(): File? =
        context.getExternalFilesDir(null)?.let { File(it, BundledModelSource.MODEL_FILE) }

    override fun status(): ModelStatus =
        if (file()?.let { it.isFile && it.length() > 0 } == true) ModelStatus.READY else ModelStatus.MISSING

    override fun modelSizeBytes(): Long? = readyFile()?.length()

    override fun bytesNeeded(): Long = 0

    override fun readyFile(): File? = file()?.takeIf { status() == ModelStatus.READY }

    override suspend fun prepare(onProgress: (Float) -> Unit): File =
        readyFile() ?: throw ModelUnavailable.Missing()

    /** The adb-pushed file is ours to read, not to delete: push a good one again instead. */
    override fun discardCopy() = Unit
}
