package com.mobileorienteering

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class MobileOrienteeringApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
