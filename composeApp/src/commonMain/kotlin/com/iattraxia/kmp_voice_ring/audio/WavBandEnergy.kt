package com.iattraxia.kmp_voice_ring.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max

/**
 * Precomputes a band-energy track replicating the HTML prototype's AnalyserNode.
 *
 * Pipeline (per 256-sample frame, hop 128):
 *  1. Mix L+R PCM → mono float in [-1, 1].
 *  2. Apply Blackman window (Web Audio default).
 *  3. Real FFT → magnitude spectrum, normalized by frame size.
 *  4. Per-bin IIR smoothing across frames: s[k] = 0.85 * s_prev[k] + 0.15 * mag[k].
 *  5. Convert to dB in [minDb, maxDb], map to [0, 255] byte equivalent.
 *  6. Average bytes in bins [binLo..binHi] inclusive (prototype: 2..14 → 13 bins).
 *  7. Bucket frames into bucketMs windows (mean of byte-level values).
 *  8. Normalize to [0, 1] using p99 (transient-robust).
 */
fun parseWavBandEnergies(
    bytes: ByteArray,
    bucketMs: Int = 16,
    fftSize: Int = 256,
    hopSize: Int = 128,
    smoothing: Double = 0.85,
    minDb: Double = -100.0,
    maxDb: Double = -30.0,
    binLo: Int = 2,
    binHi: Int = 14,
): AmplitudeTrack {
    require(bucketMs > 0) { "bucketMs must be > 0" }
    require(fftSize > 0 && (fftSize and (fftSize - 1)) == 0) { "fftSize must be power of 2" }
    require(hopSize in 1..fftSize) { "hopSize must be in 1..fftSize" }
    require(binLo in 0..binHi && binHi < fftSize / 2) { "invalid binLo/binHi" }

    val header = readWavHeader(bytes)
    val samples = decodePcmToMono(bytes, header)
    val sampleRate = header.sampleRate
    val totalDurationMs = samples.size.toLong() * 1000L / sampleRate

    val fft = Fft(fftSize)
    val window = blackmanWindow(fftSize)
    val frameBuf = DoubleArray(fftSize)
    val mags = DoubleArray(fftSize / 2)
    val smoothed = DoubleArray(fftSize / 2)
    val dbSpan = maxDb - minDb
    val bandCount = binHi - binLo + 1
    val framesPerSecond = sampleRate.toDouble() / hopSize
    val framesPerBucket = max(1, (framesPerSecond * bucketMs / 1000.0).toInt())
    val actualBucketMs = framesPerBucket.toDouble() * hopSize * 1000.0 / sampleRate

    val bucketCount = ((samples.size - fftSize) / hopSize + 1).let { frames ->
        if (frames <= 0) 1 else (frames + framesPerBucket - 1) / framesPerBucket
    }
    val buckets = FloatArray(bucketCount)

    var frameIdx = 0
    var bucketIdx = 0
    var bucketAccum = 0.0
    var bucketFramesUsed = 0
    var cursor = 0
    while (cursor + fftSize <= samples.size) {
        for (i in 0 until fftSize) {
            frameBuf[i] = samples[cursor + i] * window[i]
        }
        fft.magnitudes(frameBuf, mags)

        val norm = 1.0 / fftSize
        var bandSum = 0.0
        for (k in binLo..binHi) {
            val m = mags[k] * norm
            val s = smoothing * smoothed[k] + (1.0 - smoothing) * m
            smoothed[k] = s
            val db = if (s > 0.0) 20.0 * log10(s) else minDb
            val clamped = db.coerceIn(minDb, maxDb)
            val byteVal = ((clamped - minDb) * 255.0 / dbSpan)
            bandSum += byteVal
        }
        val frameValue = bandSum / bandCount
        bucketAccum += frameValue
        bucketFramesUsed++

        frameIdx++
        cursor += hopSize
        if (bucketFramesUsed >= framesPerBucket && bucketIdx < bucketCount) {
            buckets[bucketIdx++] = (bucketAccum / bucketFramesUsed).toFloat()
            bucketAccum = 0.0
            bucketFramesUsed = 0
        }
    }
    if (bucketFramesUsed > 0 && bucketIdx < bucketCount) {
        buckets[bucketIdx++] = (bucketAccum / bucketFramesUsed).toFloat()
    }

    val peak = percentile(buckets, bucketIdx, 0.99f)
    if (peak > 0f) {
        val inv = 1f / peak
        for (i in 0 until bucketIdx) buckets[i] = (buckets[i] * inv).coerceIn(0f, 1f)
    } else {
        for (i in 0 until bucketIdx) buckets[i] = 0f
    }

    return AmplitudeTrack(
        bucketMs = actualBucketMs,
        totalDurationMs = totalDurationMs,
        buckets = buckets,
    )
}

private data class WavHeader(
    val audioFormat: Int,
    val numChannels: Int,
    val sampleRate: Int,
    val bitsPerSample: Int,
    val dataOffset: Int,
    val dataSize: Int,
)

private fun readWavHeader(bytes: ByteArray): WavHeader {
    require(bytes.size >= 44) { "WAV too small: ${bytes.size} bytes" }
    require(readAsciiLocal(bytes, 0, 4) == "RIFF") { "Not a RIFF file" }
    require(readAsciiLocal(bytes, 8, 4) == "WAVE") { "Not a WAVE file" }

    var audioFormat = 0
    var numChannels = 0
    var sampleRate = 0
    var bitsPerSample = 0
    var dataOffset = -1
    var dataSize = 0

    var p = 12
    while (p + 8 <= bytes.size) {
        val id = readAsciiLocal(bytes, p, 4)
        val size = readU32LELocal(bytes, p + 4)
        val bodyStart = p + 8
        when (id) {
            "fmt " -> {
                audioFormat = readU16LELocal(bytes, bodyStart)
                numChannels = readU16LELocal(bytes, bodyStart + 2)
                sampleRate = readU32LELocal(bytes, bodyStart + 4).toInt()
                bitsPerSample = readU16LELocal(bytes, bodyStart + 14)
            }
            "data" -> {
                dataOffset = bodyStart
                dataSize = size.toInt().coerceAtMost(bytes.size - bodyStart)
            }
        }
        if (dataOffset >= 0 && audioFormat != 0) break
        p = bodyStart + size.toInt() + (size.toInt() and 1)
    }

    require(audioFormat == 1) { "Only PCM (audioFormat=1) supported, got $audioFormat" }
    require(numChannels in 1..2) { "Only mono/stereo supported, got $numChannels channels" }
    require(bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 24 || bitsPerSample == 32) {
        "Unsupported bitsPerSample=$bitsPerSample"
    }
    require(dataOffset >= 0) { "No data chunk found" }

    return WavHeader(audioFormat, numChannels, sampleRate, bitsPerSample, dataOffset, dataSize)
}

private fun decodePcmToMono(bytes: ByteArray, h: WavHeader): DoubleArray {
    val bytesPerSample = h.bitsPerSample / 8
    val frameBytes = bytesPerSample * h.numChannels
    val totalFrames = h.dataSize / frameBytes
    val maxAbs = when (h.bitsPerSample) {
        8 -> 128.0
        16 -> 32768.0
        24 -> 8388608.0
        32 -> 2147483648.0
        else -> 1.0
    }
    val out = DoubleArray(totalFrames)
    var cursor = h.dataOffset
    for (i in 0 until totalFrames) {
        if (h.numChannels == 1) {
            out[i] = readSampleLELocal(bytes, cursor, h.bitsPerSample).toDouble() / maxAbs
        } else {
            val l = readSampleLELocal(bytes, cursor, h.bitsPerSample).toDouble()
            val r = readSampleLELocal(bytes, cursor + bytesPerSample, h.bitsPerSample).toDouble()
            out[i] = ((l + r) * 0.5) / maxAbs
        }
        cursor += frameBytes
    }
    return out
}

private fun blackmanWindow(n: Int): DoubleArray {
    val w = DoubleArray(n)
    val a0 = 0.42
    val a1 = 0.5
    val a2 = 0.08
    val denom = (n - 1).toDouble()
    for (i in 0 until n) {
        val t = i / denom
        w[i] = a0 - a1 * cos(2.0 * PI * t) + a2 * cos(4.0 * PI * t)
    }
    return w
}

private fun percentile(buckets: FloatArray, count: Int, p: Float): Float {
    if (count <= 0) return 0f
    val copy = FloatArray(count) { buckets[it] }
    copy.sort()
    val idx = ((count - 1) * p).toInt().coerceIn(0, count - 1)
    return copy[idx]
}

private fun readAsciiLocal(b: ByteArray, off: Int, len: Int): String {
    val chars = CharArray(len)
    for (i in 0 until len) chars[i] = (b[off + i].toInt() and 0xFF).toChar()
    return chars.concatToString()
}

private fun readU16LELocal(b: ByteArray, off: Int): Int =
    (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)

private fun readU32LELocal(b: ByteArray, off: Int): Long =
    (b[off].toLong() and 0xFF) or
        ((b[off + 1].toLong() and 0xFF) shl 8) or
        ((b[off + 2].toLong() and 0xFF) shl 16) or
        ((b[off + 3].toLong() and 0xFF) shl 24)

private fun readSampleLELocal(b: ByteArray, off: Int, bits: Int): Int = when (bits) {
    8 -> (b[off].toInt() and 0xFF) - 128
    16 -> {
        val lo = b[off].toInt() and 0xFF
        val hi = b[off + 1].toInt()
        (hi shl 8) or lo
    }
    24 -> {
        val b0 = b[off].toInt() and 0xFF
        val b1 = b[off + 1].toInt() and 0xFF
        val b2 = b[off + 2].toInt()
        (b2 shl 16) or (b1 shl 8) or b0
    }
    32 -> {
        (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            (b[off + 3].toInt() shl 24)
    }
    else -> 0
}
