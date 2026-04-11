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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

class DesktopAudioRecorderPlayer : AudioRecorderPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    @Volatile private var clip: Clip? = null
    @Volatile private var durationMs: Long = 0L
    private var tickerJob: Job? = null
    private val playbackListeners = mutableListOf<(PlaybackProgress) -> Unit>()
    private var properties = AudioRecorderPlayerProperties()

    override suspend fun startRecording(filePath: String?): Result<String> =
        Result.failure(UnsupportedOperationException("Recording not supported on JVM target."))

    override suspend fun pauseRecording(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Recording not supported on JVM target."))

    override suspend fun resumeRecording(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Recording not supported on JVM target."))

    override suspend fun stopRecording(): Result<String> =
        Result.failure(UnsupportedOperationException("Recording not supported on JVM target."))

    override suspend fun startPlaying(filePath: String?): Result<Unit> {
        val path = filePath
            ?: return Result.failure(IllegalArgumentException("filePath is required on JVM target."))
        return startPlaying(AudioSource.File(path))
    }

    override suspend fun startPlaying(source: AudioSource): Result<Unit> = runCatching {
        val path = when (source) {
            is AudioSource.File -> source.path
            is AudioSource.Url -> throw UnsupportedOperationException(
                "URL playback not supported on JVM target."
            )
        }
        lock.withLock {
            releaseClipLocked()
            val stream = AudioSystem.getAudioInputStream(File(path))
            val newClip = AudioSystem.getClip().apply {
                open(stream)
                microsecondPosition = 0L
                start()
            }
            clip = newClip
            durationMs = newClip.microsecondLength / 1000L
        }
        startTicker()
    }

    override suspend fun pausePlaying(): Result<Unit> = runCatching {
        lock.withLock {
            clip?.stop()
        }
        tickerJob?.cancel()
        tickerJob = null
    }

    override suspend fun resumePlaying(): Result<Unit> = runCatching {
        lock.withLock {
            clip?.start()
        }
        startTicker()
    }

    override suspend fun stopPlaying(): Result<Unit> = runCatching {
        tickerJob?.cancel()
        tickerJob = null
        lock.withLock {
            val c = clip
            if (c != null) {
                c.stop()
                c.microsecondPosition = 0L
            }
        }
        emitProgress(0L)
    }

    override suspend fun seekTo(position: Long): Result<Unit> = runCatching {
        lock.withLock {
            clip?.microsecondPosition = position * 1000L
        }
    }

    override suspend fun setVolume(volume: Float): Result<Unit> = runCatching {
        lock.withLock {
            val c = clip ?: return@withLock
            if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val ctrl = c.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val clamped = volume.coerceIn(0.0001f, 1f)
                val db = (20.0 * log10(clamped.toDouble())).toFloat()
                ctrl.value = db.coerceIn(ctrl.minimum, ctrl.maximum)
            }
        }
    }

    override fun addRecordingListener(listener: (RecordingProgress) -> Unit) {
        // Recording not supported.
    }

    override fun addPlaybackListener(listener: (PlaybackProgress) -> Unit) {
        synchronized(playbackListeners) {
            playbackListeners += listener
        }
    }

    override fun addAudioMeteringListener(listener: (AudioMeteringInfo) -> Unit) {
        // Metering not supported.
    }

    override fun removeAudioMeteringListener() {
        // Metering not supported.
    }

    override fun removeListeners() {
        synchronized(playbackListeners) {
            playbackListeners.clear()
        }
        tickerJob?.cancel()
        tickerJob = null
        scope.launch {
            lock.withLock { releaseClipLocked() }
        }
    }

    override suspend fun getRecordingInfo(): Result<RecordingInfo?> = Result.success(null)

    override fun setPlayerProperties(properties: AudioRecorderPlayerProperties) {
        this.properties = properties
    }

    override fun setRecorderProperties(audioSet: RecorderAudioSet) {
        // Recording not supported.
    }

    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> =
        Result.failure(UnsupportedOperationException("Playback speed not supported on JVM target."))

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val interval = properties.updateIntervalMs.coerceAtLeast(8L)
            while (isActive) {
                val c = clip
                if (c == null) {
                    delay(interval)
                    continue
                }
                val posMs = c.microsecondPosition / 1000L
                emitProgress(posMs)
                if (!c.isRunning && posMs >= durationMs && durationMs > 0L) break
                delay(interval)
            }
        }
    }

    private fun emitProgress(posMs: Long) {
        val dur = durationMs
        val progress = PlaybackProgress(
            currentPosition = posMs,
            duration = dur,
            formattedCurrentTime = formatTime(posMs),
            formattedDuration = formatTime(dur),
        )
        val snapshot = synchronized(playbackListeners) { playbackListeners.toList() }
        for (l in snapshot) l(progress)
    }

    private fun releaseClipLocked() {
        val c = clip ?: return
        runCatching { c.stop() }
        runCatching { c.close() }
        clip = null
        durationMs = 0L
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centis = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(minutes, seconds, centis)
    }
}
