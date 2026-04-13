package com.iattraxia.kmp_voice_ring

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun isMobilePlatform(): Boolean