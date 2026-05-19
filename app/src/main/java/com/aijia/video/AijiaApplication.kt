package com.aijia.video

import android.app.Application

class AijiaApplication : Application() {
    companion object {
        lateinit var instance: AijiaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
