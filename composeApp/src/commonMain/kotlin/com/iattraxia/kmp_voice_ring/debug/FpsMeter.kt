package com.iattraxia.kmp_voice_ring.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.TimeSource

class FpsMeter {
    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private var windowStart: TimeSource.Monotonic.ValueTimeMark? = null
    private var count = 0

    fun tick() {
        val now = TimeSource.Monotonic.markNow()
        val start = windowStart
        if (start == null) {
            windowStart = now
            count = 1
            return
        }
        count++
        val elapsedMs = (now - start).inWholeMilliseconds
        if (elapsedMs >= 1000L) {
            _fps.value = count * 1000f / elapsedMs
            windowStart = now
            count = 0
        }
    }

    fun reset() {
        windowStart = null
        count = 0
        _fps.value = 0f
    }
}
