package com.iattraxia.kmp_voice_ring.player

import io.github.hyochan.audio.AudioMeteringInfo
import io.github.hyochan.audio.AudioRecorderPlayer
import io.github.hyochan.audio.AudioRecorderPlayerProperties
import io.github.hyochan.audio.AudioSource
import io.github.hyochan.audio.PlaybackProgress
import io.github.hyochan.audio.RecorderAudioSet
import io.github.hyochan.audio.RecordingInfo
import io.github.hyochan.audio.RecordingProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * wasmJs audio player. Audio playback is not supported in the browser target;
 * this implementation drives position via a monotonic timer so the ring
 * animates correctly using the pre-computed WAV amplitude data.
 */
class WasmAudioRecorderPlayer : AudioRecorderPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private val listeners = mutableListOf<(PlaybackProgress) -> Unit>()
    private var properties = AudioRecorderPlayerProperties()

    private var startMark: TimeMark? = null
    private var pausedMs: Long = 0L

    override suspend fun startPlaying(source: AudioSource): Result<Unit> = runCatching {
        stopTicker()
        pausedMs = 0L
        startMark = TimeSource.Monotonic.markNow()
        startTicker()
    }

    override suspend fun startPlaying(filePath: String?): Result<Unit> =
        startPlaying(AudioSource.File(filePath ?: return Result.failure(IllegalArgumentException("null path"))))

    override suspend fun pausePlaying(): Result<Unit> = runCatching {
        pausedMs = currentPositionMs()
        startMark = null
        stopTicker()
    }

    override suspend fun resumePlaying(): Result<Unit> = runCatching {
        startMark = TimeSource.Monotonic.markNow()
        startTicker()
    }

    override suspend fun stopPlaying(): Result<Unit> = runCatching {
        stopTicker()
        startMark = null
        pausedMs = 0L
        emitProgress(0L)
    }

    override fun addPlaybackListener(listener: (PlaybackProgress) -> Unit) {
        listeners += listener
    }

    override fun removeListeners() {
        listeners.clear()
        stopTicker()
    }

    override fun setPlayerProperties(properties: AudioRecorderPlayerProperties) {
        this.properties = properties
    }

    private fun currentPositionMs(): Long =
        pausedMs + (startMark?.elapsedNow()?.inWholeMilliseconds ?: 0L)

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val interval = properties.updateIntervalMs.coerceAtLeast(8L)
            while (isActive) {
                delay(interval)
                emitProgress(currentPositionMs())
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun emitProgress(posMs: Long) {
        val progress = PlaybackProgress(
            currentPosition = posMs,
            duration = 0L, // ViewModel keeps track.totalDurationMs; end-detection via Stop
            formattedCurrentTime = posMs.fmtTime(),
            formattedDuration = "00:00.00",
        )
        for (l in listeners.toList()) l(progress)
    }

    // Unsupported operations
    override suspend fun startRecording(filePath: String?) =
        Result.failure<String>(UnsupportedOperationException("Recording not supported on wasmJs."))
    override suspend fun pauseRecording() =
        Result.failure<Unit>(UnsupportedOperationException("Recording not supported on wasmJs."))
    override suspend fun resumeRecording() =
        Result.failure<Unit>(UnsupportedOperationException("Recording not supported on wasmJs."))
    override suspend fun stopRecording() =
        Result.failure<String>(UnsupportedOperationException("Recording not supported on wasmJs."))
    override fun addRecordingListener(listener: (RecordingProgress) -> Unit) {}
    override fun addAudioMeteringListener(listener: (AudioMeteringInfo) -> Unit) {}
    override fun removeAudioMeteringListener() {}
    override suspend fun getRecordingInfo() = Result.success<RecordingInfo?>(null)
    override fun setRecorderProperties(audioSet: RecorderAudioSet) {}
    override suspend fun seekTo(position: Long) = Result.success(Unit)
    override suspend fun setVolume(volume: Float) = Result.success(Unit)
    override suspend fun setPlaybackSpeed(speed: Float) =
        Result.failure<Unit>(UnsupportedOperationException("Playback speed not supported on wasmJs."))
}

private fun Long.fmtTime(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centis = (this % 1000) / 10
    return "${minutes.pad2()}:${seconds.pad2()}.${centis.pad2()}"
}

private fun Long.pad2(): String = toString().padStart(2, '0')
