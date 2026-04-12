package com.iattraxia.kmp_voice_ring.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Radix-2 Cooley–Tukey FFT, in-place on separate real/imag arrays.
 * `size` must be a power of two. Used at fixed size 256 by the band-energy parser.
 */
class Fft(private val size: Int) {
    init {
        require(size > 0 && (size and (size - 1)) == 0) { "FFT size must be power of 2, got $size" }
    }

    private val cosTable: DoubleArray
    private val sinTable: DoubleArray
    private val reversed: IntArray

    init {
        val half = size / 2
        cosTable = DoubleArray(half)
        sinTable = DoubleArray(half)
        for (i in 0 until half) {
            val angle = -2.0 * PI * i / size
            cosTable[i] = cos(angle)
            sinTable[i] = sin(angle)
        }
        reversed = IntArray(size)
        var bits = 0
        var n = size
        while (n > 1) { n = n shr 1; bits++ }
        for (i in 0 until size) {
            var x = i
            var r = 0
            for (b in 0 until bits) {
                r = (r shl 1) or (x and 1)
                x = x shr 1
            }
            reversed[i] = r
        }
    }

    /** Real input → magnitude spectrum of length size/2 (bins 0..size/2-1). */
    fun magnitudes(input: DoubleArray, out: DoubleArray) {
        require(input.size == size)
        require(out.size == size / 2)

        val re = DoubleArray(size)
        val im = DoubleArray(size)
        for (i in 0 until size) {
            re[i] = input[reversed[i]]
        }

        var stage = 2
        while (stage <= size) {
            val half = stage / 2
            val tableStep = size / stage
            var i = 0
            while (i < size) {
                var k = 0
                var tIdx = 0
                while (k < half) {
                    val c = cosTable[tIdx]
                    val s = sinTable[tIdx]
                    val j1 = i + k
                    val j2 = j1 + half
                    val tRe = c * re[j2] - s * im[j2]
                    val tIm = s * re[j2] + c * im[j2]
                    re[j2] = re[j1] - tRe
                    im[j2] = im[j1] - tIm
                    re[j1] = re[j1] + tRe
                    im[j1] = im[j1] + tIm
                    k++
                    tIdx += tableStep
                }
                i += stage
            }
            stage = stage shl 1
        }

        for (k in 0 until size / 2) {
            out[k] = sqrt(re[k] * re[k] + im[k] * im[k])
        }
    }
}
