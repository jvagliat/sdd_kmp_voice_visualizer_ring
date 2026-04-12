package com.iattraxia.kmp_voice_ring

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    raiseWindowsTimerResolution()
    application {
        val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "VoiceVisualizerRing",
        ) {
            App()
        }
    }
}

// On Windows the default kernel timer granularity is ~15.6ms, which caps any
// sub-frame delay()/sleep() to ~32Hz. A daemon thread parked in a long Thread.sleep
// keeps the JVM's internal timeBeginPeriod(1) active for the whole process, so
// delay(16) actually yields ~60Hz. No-op on non-Windows OSes.
private fun raiseWindowsTimerResolution() {
    if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return
    Thread({
        try {
            Thread.sleep(Long.MAX_VALUE)
        } catch (_: InterruptedException) {
        }
    }, "win-timer-hires").apply {
        isDaemon = true
        start()
    }
}