package com.mobileorienteering.util

import android.content.Context
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.mobileorienteering.R
import com.mobileorienteering.data.model.SettingsModel
import com.mobileorienteering.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class FeedbackManager @Inject constructor(
    @field:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private val controlPointSoundId by lazy {
        soundPool.load(context, R.raw.control_point_sound, 1)
    }

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val settingsFlow = settingsRepository.settingsFlow.stateIn(
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
