package com.aimong.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AimongApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: 앱 초기화 (Firebase, ML Kit 등)
    }
}
