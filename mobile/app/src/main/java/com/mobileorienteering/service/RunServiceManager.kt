package com.mobileorienteering.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.mobileorienteering.data.model.domain.Checkpoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunServiceManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var service: RunTrackingService? = null
    private var isBound = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _runState = MutableStateFlow(RunState())
    val runState: StateFlow<RunState> = _runState.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val runBinder = binder as? RunTrackingService.RunBinder
            service = runBinder?.getService()
            isBound = true
            _isServiceRunning.value = true

            service?.let { svc ->
                scope.launch {
                    svc.runState.collect { state ->
                        _runState.value = state
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            _isServiceRunning.value = false
        }
    }

    fun startRun(checkpoints: List<Checkpoint>, mapId: Long, mapName: String) {
        val intent = Intent(context, RunTrackingService::class.java).apply {
            action = RunTrackingService.ACTION_START
            putExtra(RunTrackingService.EXTRA_CHECKPOINTS, checkpoints.toServiceJson())
            putExtra(RunTrackingService.EXTRA_MAP_ID, mapId)
            putExtra(RunTrackingService.EXTRA_MAP_NAME, mapName)
        }

        context.startForegroundService(intent)
        bindToService()
    }

    fun stopRun(): RunState {
        val finalState = service?.stopRun() ?: _runState.value
        unbindFromService()
        _runState.value = RunState()
        _isServiceRunning.value = false
        return finalState
    }

    fun bindToService() {
        if (!isBound) {
            val intent = Intent(context, RunTrackingService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindFromService() {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {
            }
            isBound = false
            service = null
        }
    }

    fun tryReconnect() {
        if (!isBound) {
            val intent = Intent(context, RunTrackingService::class.java)
            try {
                context.bindService(intent, serviceConnection, 0)
            } catch (_: Exception) {
            }
        }
    }
}
