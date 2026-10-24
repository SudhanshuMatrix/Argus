package com.ajayinnovations.argus.core

/**
 * A .litertlm file starts with the ASCII magic "LITERTLM". LiteRT-LM 0.17.1 aborts the whole
 * process (not an exception) on some broken files: its error message quotes the file's bytes,
 * which aren't valid UTF-8 for JNI. So the file's start is checked before LiteRT-LM sees it.
 * Pure Kotlin.
 */
object ModelHeader {
    private val MAGIC = "LITERTLM".toByteArray(Charsets.US_ASCII)

    val SIZE: Int get() = MAGIC.size

    /** True when [start] (the first bytes of the file) begins with the magic. */
    fun isValid(start: ByteArray): Boolean =
        start.size >= MAGIC.size && MAGIC.indices.all { start[it] == MAGIC[it] }
}
