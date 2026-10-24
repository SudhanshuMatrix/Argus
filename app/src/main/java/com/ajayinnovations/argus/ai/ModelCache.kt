package com.ajayinnovations.argus.ai

import android.content.Context
import java.io.File

/**
 * LiteRT-LM writes a repacked copy of the weights to cacheDir on first load, about the size
 * of the model file per backend (measured on Pixel 8 with 0.17.1: GPU 505 MB weights + 22 MB
 * programs in "*_mldrift_*" files, CPU 509 MB in "*.xnnpack_cache"). It makes later loads
 * faster; Android may delete it when storage runs low, and the next load rebuilds it.
 */
class ModelCache(context: Context) {

    private val dir = context.cacheDir

    private fun files(): List<File> =
        dir.listFiles()?.filter { it.isFile && it.name.startsWith(BundledModelSource.MODEL_FILE) }.orEmpty()

    fun totalBytes(): Long = files().sumOf { it.length() }

    /** Space the first load still needs for its cache: about one model size, minus what exists. */
    fun bytesStillNeeded(modelBytes: Long): Long = (modelBytes - totalBytes()).coerceAtLeast(0)

    /** Deletes the CPU weight cache; true if there was one. The next load writes it again. */
    fun deleteCpuCaches(): Boolean =
        files().filter { GPU_MARKER !in it.name }.map { it.delete() }.any { it }

    /** Before copying the model again: every cache made from the old copy. */
    fun deleteAll() {
        files().forEach { it.delete() }
    }

    /** Called once the GPU is disabled for good: its cache would never be used again. */
    fun deleteGpuCaches() {
        files().filter { GPU_MARKER in it.name }.forEach { it.delete() }
    }

    private companion object {
        const val GPU_MARKER = "_mldrift_"
    }
}
