package com.iattraxia.kmp_voice_ring.platform

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun writeBytesToCache(relativeName: String, bytes: ByteArray): String {
    val base64 = Base64.encode(bytes)
    return "data:audio/wav;base64,$base64"
}
