package com.iattraxia.kmp_voice_ring.audio

import kotlin.math.sqrt

data class AmplitudeTrack(
    val bucketMs: Int,
    val totalDurationMs: Long,
    val buckets: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AmplitudeTrack) return false
        return bucketMs == other.bucketMs &&
            totalDurationMs == other.totalDurationMs &&
            buckets.contentEquals(other.buckets)
    }

    override fun hashCode(): Int {
        var result = bucketMs
        result = 31 * result + totalDurationMs.hashCode()
        result = 31 * result + buckets.contentHashCode()
        return result
    }
}

fun parseWavAmplitudes(bytes: ByteArray, bucketMs: Int = 16): AmplitudeTrack {
    require(bucketMs > 0) { "bucketMs must be > 0" }
    require(bytes.size >= 44) { "WAV too small: ${bytes.size} bytes" }
    require(readAscii(bytes, 0, 4) == "RIFF") { "Not a RIFF file" }
    require(readAscii(bytes, 8, 4) == "WAVE") { "Not a WAVE file" }

    var audioFormat = 0
    var numChannels = 0
    var sampleRate = 0
    var bitsPerSample = 0
    var dataOffset = -1
    var dataSize = 0

    var p = 12
    while (p + 8 <= bytes.size) {
        val id = readAscii(bytes, p, 4)
        val size = readU32LE(bytes, p + 4)
        val bodyStart = p + 8
        when (id) {
            "fmt " -> {
                audioFormat = readU16LE(bytes, bodyStart)
                numChannels = readU16LE(bytes, bodyStart + 2)
                sampleRate = readU32LE(bytes, bodyStart + 4).toInt()
                bitsPerSample = readU16LE(bytes, bodyStart + 14)
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

    val bytesPerSample = bitsPerSample / 8
    val frameBytes = bytesPerSample * numChannels
    val totalFrames = dataSize / frameBytes
    val samplesPerBucket = (sampleRate.toLong() * bucketMs / 1000L).toInt().coerceAtLeast(1)
    val bucketCount = (totalFrames + samplesPerBucket - 1) / samplesPerBucket
    val buckets = FloatArray(bucketCount)
    val maxAbs = when (bitsPerSample) {
        8 -> 128.0
        16 -> 32768.0
        24 -> 8388608.0
        32 -> 2147483648.0
        else -> 1.0
    }

    var frameIdx = 0
    var bucketIdx = 0
    var sumSquares = 0.0
    var countInBucket = 0
    var peak = 0f

    var cursor = dataOffset
    val end = dataOffset + totalFrames * frameBytes
    while (cursor < end) {
        val sample = readSampleLE(bytes, cursor, bitsPerSample).toDouble()
        sumSquares += sample * sample
        countInBucket++
        cursor += frameBytes
        frameIdx++
        if (countInBucket >= samplesPerBucket) {
            val rms = (sqrt(sumSquares / countInBucket) / maxAbs).toFloat()
            buckets[bucketIdx++] = rms
            if (rms > peak) peak = rms
            sumSquares = 0.0
            countInBucket = 0
        }
    }
    if (countInBucket > 0 && bucketIdx < bucketCount) {
        val rms = (sqrt(sumSquares / countInBucket) / maxAbs).toFloat()
        buckets[bucketIdx++] = rms
        if (rms > peak) peak = rms
    }

    if (peak > 0f) {
        val inv = 1f / peak
        for (i in 0 until bucketIdx) buckets[i] = (buckets[i] * inv).coerceIn(0f, 1f)
    }

    val totalDurationMs = totalFrames.toLong() * 1000L / sampleRate
    return AmplitudeTrack(bucketMs = bucketMs, totalDurationMs = totalDurationMs, buckets = buckets)
}

private fun readAscii(b: ByteArray, off: Int, len: Int): String {
    val chars = CharArray(len)
    for (i in 0 until len) chars[i] = (b[off + i].toInt() and 0xFF).toChar()
    return chars.concatToString()
}

private fun readU16LE(b: ByteArray, off: Int): Int =
    (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)

private fun readU32LE(b: ByteArray, off: Int): Long =
    (b[off].toLong() and 0xFF) or
        ((b[off + 1].toLong() and 0xFF) shl 8) or
        ((b[off + 2].toLong() and 0xFF) shl 16) or
        ((b[off + 3].toLong() and 0xFF) shl 24)

private fun readSampleLE(b: ByteArray, off: Int, bits: Int): Int = when (bits) {
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
