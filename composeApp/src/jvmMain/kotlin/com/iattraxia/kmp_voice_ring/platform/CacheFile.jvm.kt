package com.iattraxia.kmp_voice_ring.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths

actual suspend fun writeBytesToCache(relativeName: String, bytes: ByteArray): String =
    withContext(Dispatchers.IO) {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val path = Paths.get(tmpDir, "kmp_voice_ring", relativeName)
        Files.createDirectories(path.parent)
        Files.write(path, bytes)
        path.toAbsolutePath().toString()
    }
