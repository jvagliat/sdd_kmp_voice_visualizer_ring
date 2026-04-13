package com.iattraxia.kmp_voice_ring.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun writeBytesToCache(relativeName: String, bytes: ByteArray): String =
    withContext(Dispatchers.IO) {
        val file = File(AppContextHolder.context.cacheDir, relativeName)
        file.writeBytes(bytes)
        file.absolutePath
    }
