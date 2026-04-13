package com.iattraxia.kmp_voice_ring.platform

import android.content.Context

internal object AppContextHolder {
    lateinit var context: Context
        private set

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }
}
