package com.ajayinnovations.argus.ai

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * standard and lite: the model ships inside the APK. LiteRT-LM needs a real file, so
 * setup copies it out once. The copy goes to a temp file first, so a half-finished copy
 * is never used.
 */
class BundledModelSource(private val context: Context) : ModelSource {

    private val target = File(context.filesDir, MODEL_FILE)
    private val temp = File(context.filesDir, "$MODEL_FILE.tmp")

    /** Null when this APK has no model. openFd works because the asset is stored uncompressed. */
    private fun bundledSize(): Long? =
        runCatching { context.assets.openFd(MODEL_FILE).use { it.length } }.getOrNull()

    override fun status(): ModelStatus {
        val size = bundledSize() ?: return ModelStatus.MISSING
        return if (target.isFile && target.length() == size) ModelStatus.READY else ModelStatus.NEEDS_COPY
    }

    override fun modelSizeBytes(): Long? = bundledSize()

    override fun bytesNeeded(): Long {
        val size = bundledSize() ?: return 0
        return if (status() == ModelStatus.READY) 0 else size + SPACE_MARGIN_BYTES
    }

    override fun readyFile(): File? = target.takeIf { status() == ModelStatus.READY }

    override suspend fun prepare(onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val total = bundledSize() ?: throw ModelUnavailable.Missing()
        if (target.isFile && target.length() == total) return@withContext target

        // An old or broken copy is about to be replaced: count its space as free
        temp.delete()
        target.delete()
        val free = context.filesDir.usableSpace
        if (free < total + SPACE_MARGIN_BYTES) throw ModelUnavailable.NotEnoughSpace(total + SPACE_MARGIN_BYTES, free)

        try {
            context.assets.open(MODEL_FILE).use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 20)
                    var copied = 0L
                    var lastPercent = -1
                    while (true) {
                        ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        copied += n
                        val percent = (copied * 100 / total).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(copied.toFloat() / total)
                        }
                    }
                    output.fd.sync()
                }
            }
            if (temp.length() != total) throw ModelUnavailable.CopyFailed(null)
            if (!temp.renameTo(target)) throw ModelUnavailable.CopyFailed(null)
            target
        } catch (e: ModelUnavailable) {
            temp.delete()
            throw e
        } catch (e: Exception) {
            // Includes cancellation: never leave a partial temp file behind
            temp.delete()
            if (e is CancellationException) throw e
            throw ModelUnavailable.CopyFailed(e)
        }
    }

    override fun discardCopy() {
        temp.delete()
        target.delete()
    }

    companion object {
        const val MODEL_FILE = "model.litertlm"
        /** Head room so the phone isn't left completely full after the copy. */
        const val SPACE_MARGIN_BYTES = 200L * 1024 * 1024
    }
}
