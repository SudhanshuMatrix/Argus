package com.ajayinnovations.argus.core

/**
 * When to free the ~1 GB model. It stays loaded otherwise, so a message never waits for a
 * reload. The levels are Android's ComponentCallbacks2 values (stable since API 14),
 * mirrored here so core stays free of Android imports.
 */
object TrimPolicy {
    const val RUNNING_MODERATE = 5
    const val RUNNING_LOW = 10
    const val RUNNING_CRITICAL = 15
    const val UI_HIDDEN = 20
    const val BACKGROUND = 40
    const val MODERATE = 60
    const val COMPLETE = 80

    /**
     * Moderate pressure or worse, while running (5, 10, 15) or cached (60, 80). UI_HIDDEN
     * (left the app) and BACKGROUND (just became cached) are not reasons to unload.
     */
    fun shouldUnload(level: Int): Boolean =
        level in RUNNING_MODERATE..RUNNING_CRITICAL || level >= MODERATE
}
