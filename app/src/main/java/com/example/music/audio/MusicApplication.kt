package com.example.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar otras librerías si es necesario
    }
}
