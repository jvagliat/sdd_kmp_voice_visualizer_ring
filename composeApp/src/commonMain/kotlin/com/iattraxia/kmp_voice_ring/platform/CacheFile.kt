package com.iattraxia.kmp_voice_ring.platform

expect suspend fun writeBytesToCache(relativeName: String, bytes: ByteArray): String
