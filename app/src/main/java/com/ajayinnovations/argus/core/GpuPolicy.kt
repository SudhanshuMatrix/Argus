package com.ajayinnovations.argus.core

/**
 * When to use the GPU. CPU is the default everywhere; the GPU is opt-in ("Use GPU").
 * A thrown GPU error disables it at once. Dying during a GPU call might be a native crash
 * or an ordinary kill (swipe-away, OEM task killer, update), so only two deaths in a row
 * disable it.
 */
object GpuPolicy {

    const val DEATHS_TO_DISABLE = 2

    fun shouldUseGpu(optedIn: Boolean, disabled: Boolean): Boolean = optedIn && !disabled

    /** At load time: was the app killed during a GPU call last time? */
    fun deathsAfterStart(callWasInProgress: Boolean, deathsInARow: Int): Int =
        if (callWasInProgress) deathsInARow + 1 else deathsInARow

    fun shouldDisable(deathsInARow: Int): Boolean = deathsInARow >= DEATHS_TO_DISABLE
}
