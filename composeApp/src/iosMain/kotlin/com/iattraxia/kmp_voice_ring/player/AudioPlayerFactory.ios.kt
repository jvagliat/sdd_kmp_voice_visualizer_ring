package com.iattraxia.kmp_voice_ring.player

import io.github.hyochan.audio.AudioRecorderPlayer
import io.github.hyochan.audio.createAudioRecorderPlayer

actual fun createPlayer(): AudioRecorderPlayer = createAudioRecorderPlayer()
