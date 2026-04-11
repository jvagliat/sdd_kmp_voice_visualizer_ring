package com.iattraxia.kmp_voice_ring

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "VoiceVisualizerRing",
    ) {
        App()
    }
}