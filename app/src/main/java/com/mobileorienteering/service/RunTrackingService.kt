package com.mobileorienteering.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mobileorienteering.MainActivity
import com.mobileorienteering.R
import com.mobileorienteering.data.model.domain.Checkpoint
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import com.mobileorienteering.data.preferences.SettingsPreferences
import com.mobileorienteering.util.manager.FeedbackManager
import com.mobileorienteering.util.manager.LocationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class RunTrackingService : Service() {

    @Inject lateinit var locationManager: LocationManager
    @Inject lateinit var feedbackManager: FeedbackManager
    @Inject lateinit var settingsPreferences: SettingsPreferences

    private val binder = RunBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var locationJob: Job? = null
    private var timerJob: Job? = null

    private var lastLocation: Location? = null
    private var checkpointRadius: Int = 10

    private val _runState = MutableStateFlow(RunState())
    val runState: StateFlow<RunState> = _runState.asStateFlow()

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "run_tracking_channel"

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_CHECKPOINTS = "extra_checkpoints"
        const val EXTRA_MAP_ID = "extra_map_id"
        const val EXTRA_MAP_NAME = "extra_map_name"
    }

    inner class RunBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeSettings()
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsPreferences.settingsFlow.collect { settings ->
                checkpointRadius = settings.gpsAccuracy
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val checkpointsJson = intent.getStringExtra(EXTRA_CHECKPOINTS)
                val mapId = intent.getLongExtra(EXTRA_MAP_ID, 0L)
                val mapName = intent.getStringExtra(EXTRA_MAP_NAME) ?: "Unknown"

                if (checkpointsJson != null) {
                    val checkpoints = deserializeCheckpoints(checkpointsJson)
                    startRun(checkpoints, mapId, mapName)
                }
            }
            ACTION_STOP -> {
                stopRun()
            }
        }
        return START_STICKY
    }

    private fun startRun(checkpoints: List<Checkpoint>, mapId: Long, mapName: String) {
        val notification = createNotification("Starting run...", "0/0 checkpoints")
        startForeground(NOTIFICATION_ID, notification)

        _runState.value = RunState(
            isActive = true,
            startTime = Instant.now(),
            checkpoints = checkpoints,
            mapId = mapId,
            mapName = mapName,
            totalCheckpoints = checkpoints.size
        )

        startLocationUpdates()
        startTimer()
    }

    fun stopRun(): RunState {
        val finalState = _runState.value.copy(isActive = false)

        locationJob?.cancel()
        timerJob?.cancel()

        _runState.value = finalState

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return finalState
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            locationManager.getLocationUpdates(
                intervalMillis = 2000L,
                minimalDistance = 5f
            ).catch { e ->
                _runState.update { it.copy(error = "GPS error: ${e.message}") }
            }.collect { location ->
                updateLocation(location)
            }
        }
    }

    private fun updateLocation(location: Location) {
        val distance = lastLocation?.distanceTo(location)?.toDouble() ?: 0.0

        _runState.update { state ->
            val newPathData = state.pathData + PathPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = Instant.now()
            )

            state.copy(
                currentLocation = location,
                distance = state.distance + distance,
                pathData = newPathData
            )
        }

        lastLocation = location
        checkCheckpointVisit(location)
        updateNotification()
    }

    private fun checkCheckpointVisit(location: Location) {
        val state = _runState.value
        if (state.nextCheckpointIndex >= state.checkpoints.size) return

        val nextCheckpoint = state.checkpoints[state.nextCheckpointIndex]
        val checkpointLocation = Location("checkpoint").apply {
            latitude = nextCheckpoint.position.latitude
            longitude = nextCheckpoint.position.longitude
        }

        val distanceToCheckpoint = location.distanceTo(checkpointLocation)

        if (distanceToCheckpoint <= checkpointRadius) {
            val visitedPoint = VisitedControlPoint(
                controlPointName = nextCheckpoint.name,
                order = state.nextCheckpointIndex + 1,
                visitedAt = Instant.now(),
                latitude = nextCheckpoint.position.latitude,
                longitude = nextCheckpoint.position.longitude
            )

            val newNextIndex = state.nextCheckpointIndex + 1
            val allVisited = newNextIndex >= state.checkpoints.size

            _runState.update { current ->
                current.copy(
                    visitedCheckpointIndices = current.visitedCheckpointIndices + current.nextCheckpointIndex,
                    checkpointVisitTimes = current.checkpointVisitTimes + (current.nextCheckpointIndex to Instant.now()),
                    visitedControlPoints = current.visitedControlPoints + visitedPoint,
                    nextCheckpointIndex = newNextIndex,
                    autoFinished = allVisited
                )
            }

            if (allVisited) {
                feedbackManager.playFinishFeedback()
            } else {
                feedbackManager.playControlPointFeedback()
            }

            updateNotification()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                _runState.update { state ->
                    state.startTime?.let { start ->
                        val elapsed = Duration.between(start, Instant.now()).seconds
                        state.copy(elapsedSeconds = elapsed)
                    } ?: state
                }
                updateNotification()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Run Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows run progress"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_runs_filled)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val state = _runState.value
        val time = formatDuration(state.elapsedSeconds)
        val checkpoints = "${state.visitedCheckpointIndices.size}/${state.totalCheckpoints}"
        val distance = formatDistance(state.distance)

        val notification = createNotification(
            "Run in progress • $time",
            "$checkpoints checkpoints • $distance"
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.2f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationJob?.cancel()
        timerJob?.cancel()
        serviceScope.cancel()
    }

    // Serializacja checkpointów do przekazania przez Intent
    private fun deserializeCheckpoints(json: String): List<Checkpoint> {
        return try {
            val checkpointStrings = json.removeSurrounding("[", "]").split("},{")
            checkpointStrings.mapNotNull { str ->
                val clean = str.removePrefix("{").removeSuffix("}")
                val parts = clean.split(",").associate { part ->
                    val (key, value) = part.split(":")
                    key.trim().removeSurrounding("\"") to value.trim().removeSurrounding("\"")
                }

                val lat = parts["lat"]?.toDoubleOrNull() ?: return@mapNotNull null
                val lng = parts["lng"]?.toDoubleOrNull() ?: return@mapNotNull null
                val name = parts["name"] ?: ""
                val id = parts["id"] ?: java.util.UUID.randomUUID().toString()

                Checkpoint(
                    id = id,
                    position = org.maplibre.spatialk.geojson.Position(lng, lat),
                    name = name
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class RunState(
    val isActive: Boolean = false,
    val startTime: Instant? = null,
    val elapsedSeconds: Long = 0,
    val checkpoints: List<Checkpoint> = emptyList(),
    val visitedCheckpointIndices: Set<Int> = emptySet(),
    val checkpointVisitTimes: Map<Int, Instant> = emptyMap(),
    val visitedControlPoints: List<VisitedControlPoint> = emptyList(),
    val nextCheckpointIndex: Int = 0,
    val totalCheckpoints: Int = 0,
    val distance: Double = 0.0,
    val pathData: List<PathPoint> = emptyList(),
    val currentLocation: Location? = null,
    val mapId: Long = 0,
    val mapName: String = "",
    val error: String? = null,
    val autoFinished: Boolean = false
) {
    val isCompleted: Boolean
        get() = visitedCheckpointIndices.size == totalCheckpoints && totalCheckpoints > 0

    val durationString: String
        get() {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val secs = elapsedSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%02d:%02d", minutes, secs)
            }
        }
}

fun List<Checkpoint>.toServiceJson(): String {
    return this.joinToString(separator = ",", prefix = "[", postfix = "]") { cp ->
        "{\"id\":\"${cp.id}\",\"lat\":${cp.position.latitude},\"lng\":${cp.position.longitude},\"name\":\"${cp.name}\"}"
    }
}