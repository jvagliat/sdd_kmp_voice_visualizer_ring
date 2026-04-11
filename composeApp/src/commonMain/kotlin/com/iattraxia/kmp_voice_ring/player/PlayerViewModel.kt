package com.iattraxia.kmp_voice_ring.player

import com.iattraxia.kmp_voice_ring.audio.AmplitudeTrack
import com.iattraxia.kmp_voice_ring.audio.parseWavAmplitudes
import com.iattraxia.kmp_voice_ring.debug.FpsMeter
import com.iattraxia.kmp_voice_ring.platform.writeBytesToCache
import io.github.hyochan.audio.AudioRecorderPlayer
import io.github.hyochan.audio.AudioSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import voicevisualizerring.composeapp.generated.resources.Res

sealed interface PlayerState {
    data object Idle : PlayerState
    data object Loading : PlayerState
    data object Ready : PlayerState
    data object Playing : PlayerState
    data object Paused : PlayerState
    data class Error(val msg: String) : PlayerState
}

class PlayerViewModel(
    private val player: AudioRecorderPlayer,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _volume = MutableStateFlow(0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val volumeFpsMeter = FpsMeter()
    val volumeFps: StateFlow<Float> = volumeFpsMeter.fps

    private var amplitudes: AmplitudeTrack? = null
    private var cachedPath: String? = null

    init {
        player.addPlaybackListener { progress ->
            _positionMs.value = progress.currentPosition
            if (progress.duration > 0L) _durationMs.value = progress.duration

            val track = amplitudes
            if (track != null && track.buckets.isNotEmpty()) {
                val idx = (progress.currentPosition / track.bucketMs)
                    .toInt()
                    .coerceIn(0, track.buckets.size - 1)
                _volume.value = track.buckets[idx]
                volumeFpsMeter.tick()
            }

            if (progress.duration > 0L &&
                progress.currentPosition >= progress.duration &&
                _state.value == PlayerState.Playing
            ) {
                _state.value = PlayerState.Ready
                _positionMs.value = 0L
                _volume.value = 0f
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(assetRelativePath: String) {
        _state.value = PlayerState.Loading
        try {
            val bytes = Res.readBytes(assetRelativePath)
            val track = parseWavAmplitudes(bytes)
            amplitudes = track
            _durationMs.value = track.totalDurationMs

            val fileName = assetRelativePath.substringAfterLast('/')
            cachedPath = writeBytesToCache(fileName, bytes)

            _state.value = PlayerState.Ready
        } catch (t: Throwable) {
            _state.value = PlayerState.Error(t.message ?: t::class.simpleName ?: "load failed")
        }
    }

    fun togglePlayPause() {
        val path = cachedPath ?: return
        scope.launch {
            when (_state.value) {
                PlayerState.Ready -> {
                    player.startPlaying(AudioSource.File(path))
                        .onSuccess { _state.value = PlayerState.Playing }
                        .onFailure {
                            _state.value = PlayerState.Error(it.message ?: "play failed")
                        }
                }
                PlayerState.Paused -> {
                    player.resumePlaying()
                        .onSuccess { _state.value = PlayerState.Playing }
                        .onFailure {
                            _state.value = PlayerState.Error(it.message ?: "resume failed")
                        }
                }
                PlayerState.Playing -> {
                    player.pausePlaying()
                        .onSuccess { _state.value = PlayerState.Paused }
                        .onFailure {
                            _state.value = PlayerState.Error(it.message ?: "pause failed")
                        }
                }
                else -> Unit
            }
        }
    }

    fun stop() {
        scope.launch {
            player.stopPlaying()
                .onSuccess {
                    _state.value = PlayerState.Ready
                    _positionMs.value = 0L
                    _volume.value = 0f
                }
                .onFailure {
                    _state.value = PlayerState.Error(it.message ?: "stop failed")
                }
        }
    }

    fun dispose() {
        player.removeListeners()
    }
}
