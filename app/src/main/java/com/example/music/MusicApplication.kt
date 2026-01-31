package com.example.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * ✅ UBICACIÓN: app/src/main/java/com/example/music/MusicApplication.kt
 *
 * Esta clase debe ser declarada en AndroidManifest.xml:
 * <application
 *     android:name=".MusicApplication"
 *     ...
 * >
 */
@HiltAndroidApp
class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar otras cosas si necesitas
    }
}