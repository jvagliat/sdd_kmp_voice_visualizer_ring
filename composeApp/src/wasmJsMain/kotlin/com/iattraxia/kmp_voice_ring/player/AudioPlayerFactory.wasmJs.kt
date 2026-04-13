package com.iattraxia.kmp_voice_ring.player

import io.github.hyochan.audio.AudioRecorderPlayer

actual fun createPlayer(): AudioRecorderPlayer = WasmAudioRecorderPlayer()
