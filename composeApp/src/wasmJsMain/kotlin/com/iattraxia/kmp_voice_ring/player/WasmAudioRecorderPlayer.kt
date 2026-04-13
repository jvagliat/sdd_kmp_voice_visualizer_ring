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

// Browser HTMLAudioElement surface we actually use
private external interface JsAudio : JsAny {
    val currentTime: Double
    val duration: Double
    val ended: Boolean
    fun play(): JsAny?
    fun pause()
}

// js() must be the sole expression in a top-level function body (Kotlin/Wasm constraint).
private fun newAudioJs(src: String): JsAny? = js("new Audio(src)")

/** Create an HTMLAudioElement with the given src (data: URL or http URL). */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun newAudio(src: String): JsAudio =
    newAudioJs(src).unsafeCast<JsAudio>()

/**
 * wasmJs audio player backed by the browser's HTMLAudioElement.
 * Accepts a data:audio/wav;base64,... URL as produced by CacheFile.wasmJs.kt.
 * A coroutine ticker reads currentTime/duration from the element and emits
 * PlaybackProgress so the ViewModel can drive the amplitude visualizer.
 */
class WasmAudioRecorderPlayer : AudioRecorderPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private val listeners = mutableListOf<(PlaybackProgress) -> Unit>()
    private var properties = AudioRecorderPlayerProperties()

    private var jsAudio: JsAudio? = null

    override suspend fun startPlaying(source: AudioSource): Result<Unit> = runCatching {
        stopTicker()
        jsAudio?.pause()

        val src = when (source) {
            is AudioSource.File -> source.path
            is AudioSource.Url -> source.url
        }

        val audio = newAudio(src)
        jsAudio = audio
        audio.play()
        startTicker(audio)
    }

    override suspend fun startPlaying(filePath: String?): Result<Unit> =
        startPlaying(
            AudioSource.File(
                filePath ?: return Result.failure(IllegalArgumentException("null path"))
            )
        )

    override suspend fun pausePlaying(): Result<Unit> = runCatching {
        jsAudio?.pause()
        stopTicker()
    }

    override suspend fun resumePlaying(): Result<Unit> = runCatching {
        val audio = jsAudio ?: return@runCatching
        audio.play()
        startTicker(audio)
    }

    override suspend fun stopPlaying(): Result<Unit> = runCatching {
        jsAudio?.pause()
        jsAudio = null
        stopTicker()
        emitProgress(0L, 0L)
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

    // ── ticker ──────────────────────────────────────────────────────────────

    private fun startTicker(audio: JsAudio) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val interval = properties.updateIntervalMs.coerceAtLeast(8L)
            while (isActive) {
                delay(interval)
                val posMs = (audio.currentTime * 1000.0).toLong()
                val rawDur = audio.duration
                val durMs = if (!rawDur.isNaN() && rawDur > 0.0) (rawDur * 1000.0).toLong() else 0L
                emitProgress(posMs, durMs)
                if (audio.ended) {
                    stopTicker()
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun emitProgress(posMs: Long, durMs: Long) {
        val p = PlaybackProgress(
            currentPosition = posMs,
            duration = durMs,
            formattedCurrentTime = posMs.fmtTime(),
            formattedDuration = durMs.fmtTime(),
        )
        for (l in listeners.toList()) l(p)
    }

    // ── unsupported ──────────────────────────────────────────────────────────

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
