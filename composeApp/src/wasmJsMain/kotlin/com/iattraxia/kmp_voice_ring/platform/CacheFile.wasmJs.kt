package com.iattraxia.kmp_voice_ring.platform

actual suspend fun writeBytesToCache(relativeName: String, bytes: ByteArray): String =
    throw NotImplementedError("wasmJs cache wiring pending.")
