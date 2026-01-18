package com.mobileorienteering.util.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mobileorienteering.MainActivity
import com.mobileorienteering.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(AndroidNotificationManager::class.java)

    companion object {
        const val RUN_TRACKING_CHANNEL_ID = "run_tracking_channel"
        const val RUN_TRACKING_NOTIFICATION_ID = 1001
    }

    init {
        createRunTrackingChannel()
    }

    private fun createRunTrackingChannel() {
        val channel = NotificationChannel(
            RUN_TRACKING_CHANNEL_ID,
            "Run Tracking",
            AndroidNotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows run progress"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    fun buildRunNotification(
        title: String,
        content: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, RUN_TRACKING_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_runs_filled)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
    }

    fun buildRunProgressNotification(
        elapsedSeconds: Long,
        visitedCheckpoints: Int,
        totalCheckpoints: Int,
        distanceMeters: Double
    ): Notification {
        val time = formatDuration(elapsedSeconds)
        val checkpoints = "$visitedCheckpoints/$totalCheckpoints"
        val distance = formatDistance(distanceMeters)

        return buildRunNotification(
            title = "Run in progress • $time",
            content = "$checkpoints checkpoints • $distance"
        )
    }

    fun notify(notificationId: Int, notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
        }
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format(Locale.getDefault(), "%.2f km", meters / 1000)
        } else {
            String.format(Locale.getDefault(), "%.0f m", meters)
        }
    }
}
