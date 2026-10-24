package com.ajayinnovations.argus.ai

import java.io.File

enum class ModelStatus {
    /** The model file is in place and complete. */
    READY,
    /** The APK carries the model but it hasn't been copied out yet (setup does this). */
    NEEDS_COPY,
    /** No model anywhere: dev build without the adb-pushed file, or an APK built without it. */
    MISSING,
}

/** Where the ready model file is. */
interface ModelSource {

    fun status(): ModelStatus

    /** Size of the model file, or null if there is none. */
    fun modelSizeBytes(): Long?

    /** Bytes of free space [prepare] needs; 0 once the model is ready. */
    fun bytesNeeded(): Long

    /**
     * The model file if it is ready, otherwise null. Never copies anything, so background
     * callers use this and skip until setup has finished.
     */
    fun readyFile(): File?

    /** Setup only: makes the model ready, copying it if needed. Throws [ModelUnavailable]. */
    suspend fun prepare(onProgress: (Float) -> Unit = {}): File

    /** Deletes our copy of the model so [prepare] copies it again (a copy that won't load). */
    fun discardCopy()
}

sealed class ModelUnavailable(message: String) : Exception(message) {
    class Missing : ModelUnavailable("No model file")
    class NotEnoughSpace(val neededBytes: Long, val freeBytes: Long) : ModelUnavailable("Not enough space")
    class CopyFailed(cause: Throwable?) : ModelUnavailable("Model copy failed") {
        init { cause?.let(::initCause) }
    }
}
