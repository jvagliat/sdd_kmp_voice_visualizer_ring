package com.iattraxia.kmp_voice_ring

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun isMobilePlatform(): Boolean = false