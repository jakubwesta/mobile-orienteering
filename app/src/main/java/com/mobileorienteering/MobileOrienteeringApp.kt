package com.mobileorienteering

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class MobileOrienteeringApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicjalizacja MapLibre z domyślnym tile serverem
        MapLibre.getInstance(this)
    }
}