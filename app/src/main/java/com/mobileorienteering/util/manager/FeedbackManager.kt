package com.mobileorienteering.util.manager

import android.content.Context
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.mobileorienteering.R
import com.mobileorienteering.data.model.domain.SettingsModel
import com.mobileorienteering.data.preferences.SettingsPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    settingsPreferences: SettingsPreferences
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private val controlPointSoundId by lazy {
        soundPool.load(context, R.raw.sound, 1)
    }

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val settingsFlow = settingsPreferences.settingsFlow.stateIn(
        CoroutineScope(Dispatchers.Default),
        SharingStarted.Eagerly,
        SettingsModel()
    )

    fun playControlPointFeedback() {
        val settings = settingsFlow.value
        if (settings.controlPointSound) {
            soundPool.play(controlPointSoundId, 1f, 1f, 1, 0, 1f)
        }
        if (settings.controlPointVibration) {
            vibrate(100)
        }
    }

    fun playFinishFeedback() {
        val settings = settingsFlow.value
        if (settings.controlPointSound) {
            soundPool.play(controlPointSoundId, 1f, 1f, 1, 0, 1f)
        }
        if (settings.controlPointVibration) {
            vibratePattern(pattern = longArrayOf(0, 80, 20, 30, 20, 80))
        }
    }

    fun playSoundEnableFeedback() {
        soundPool.play(controlPointSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playVibrationEnableFeedback() {
        vibrate(100)
    }

    private fun vibrate(durationMs: Long) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, -1)
        )
    }
}
