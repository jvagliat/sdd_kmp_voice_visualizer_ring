package com.iattraxia.kmp_voice_ring.player

import io.github.hyochan.audio.AudioMeteringInfo
import io.github.hyochan.audio.AudioRecorderPlayer
import io.github.hyochan.audio.AudioRecorderPlayerProperties
import io.github.hyochan.audio.AudioSource
import io.github.hyochan.audio.PlaybackProgress
import io.github.hyochan.audio.RecorderAudioSet
import io.github.hyochan.audio.RecordingInfo
import io.github.hyochan.audio.RecordingProgress
import kotlin.js.JsAny
import kotlin.js.JsFun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WasmAudioRecorderPlayer : AudioRecorderPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioEl: JsAny? = null
    private var tickerJob: Job? = null
    private val listeners = mutableListOf<(PlaybackProgress) -> Unit>()
    private var properties = AudioRecorderPlayerProperties()

    override suspend fun startPlaying(source: AudioSource): Result<Unit> = runCatching {
        val src = when (source) {
            is AudioSource.File -> source.path
            is AudioSource.Url -> throw UnsupportedOperationException("URL source not supported on wasmJs.")
        }
        stopTicker()
        audioEl?.let { jsPause(it) }
        val a = jsNewAudio(src)
        audioEl = a
        jsPlay(a)
        startTicker()
    }

    override suspend fun startPlaying(filePath: String?): Result<Unit> =
        startPlaying(AudioSource.File(filePath ?: return Result.failure(IllegalArgumentException("filePath is null"))))

    override suspend fun pausePlaying(): Result<Unit> = runCatching {
        stopTicker()
        audioEl?.let { jsPause(it) }
    }

    override suspend fun resumePlaying(): Result<Unit> = runCatching {
        audioEl?.let { jsPlay(it) }
        startTicker()
    }

    override suspend fun stopPlaying(): Result<Unit> = runCatching {
        stopTicker()
        audioEl?.let { jsPause(it); jsRewind(it) }
        emitProgress(0L, 0L)
    }

    override fun addPlaybackListener(listener: (PlaybackProgress) -> Unit) {
        listeners += listener
    }

    override fun removeListeners() {
        listeners.clear()
        stopTicker()
        audioEl?.let { jsPause(it) }
        audioEl = null
    }

    override fun setPlayerProperties(properties: AudioRecorderPlayerProperties) {
        this.properties = properties
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val interval = properties.updateIntervalMs.coerceAtLeast(8L)
            while (isActive) {
                delay(interval)
                val a = audioEl ?: continue
                val posMs = jsGetCurrentTimeMs(a).toLong()
                val durMs = jsGetDurationMs(a).toLong()
                emitProgress(posMs, durMs)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun emitProgress(posMs: Long, durMs: Long) {
        val progress = PlaybackProgress(
            currentPosition = posMs,
            duration = durMs,
            formattedCurrentTime = posMs.fmtTime(),
            formattedDuration = durMs.fmtTime(),
        )
        for (l in listeners.toList()) l(progress)
    }

    // Unsupported: recording
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

@JsFun("(src) => new Audio(src)")
private external fun jsNewAudio(src: String): JsAny

@JsFun("(a) => { a.play().catch(e => console.warn('audio play:', e)); }")
private external fun jsPlay(a: JsAny)

@JsFun("(a) => a.pause()")
private external fun jsPause(a: JsAny)

@JsFun("(a) => { a.currentTime = 0; }")
private external fun jsRewind(a: JsAny)

@JsFun("(a) => (a.currentTime * 1000) | 0")
private external fun jsGetCurrentTimeMs(a: JsAny): Int

@JsFun("(a) => isFinite(a.duration) ? ((a.duration * 1000) | 0) : 0")
private external fun jsGetDurationMs(a: JsAny): Int
