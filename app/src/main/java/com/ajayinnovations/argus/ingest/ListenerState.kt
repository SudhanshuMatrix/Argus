package com.ajayinnovations.argus.ingest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether Android has the notification listener connected right now (in-process only). */
object ListenerState {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    internal fun set(connected: Boolean) {
        _connected.value = connected
    }
}
