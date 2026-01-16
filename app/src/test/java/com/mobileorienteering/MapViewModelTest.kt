package com.mobileorienteering

import com.mobileorienteering.data.model.domain.SavedMapState
import com.mobileorienteering.data.model.domain.SettingsModel
import com.mobileorienteering.data.preferences.MapStatePreferences
import com.mobileorienteering.data.preferences.SettingsPreferences
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.service.RunServiceManager
import com.mobileorienteering.service.RunState
import com.mobileorienteering.ui.screen.main.map.MapViewModel
import com.mobileorienteering.util.manager.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var locationManager: LocationManager
    private lateinit var mapRepository: MapRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var mapStatePreferences: MapStatePreferences
    private lateinit var settingsPreferences: SettingsPreferences
    private lateinit var activityRepository: ActivityRepository
    private lateinit var runServiceManager: RunServiceManager

    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        locationManager = mock()
        mapRepository = mock()
        authRepository = mock()
        mapStatePreferences = mock()
        settingsPreferences = mock()
        activityRepository = mock()
        runServiceManager = mock()

        whenever(locationManager.hasLocationPermission()).thenReturn(false)
        runBlocking {
            whenever(mapStatePreferences.getSavedState()).thenReturn(SavedMapState())
        }
        whenever(settingsPreferences.settingsFlow).thenReturn(flowOf(SettingsModel()))
        whenever(runServiceManager.runState).thenReturn(MutableStateFlow(RunState()))

        viewModel = MapViewModel(
            locationManager = locationManager,
            mapRepository = mapRepository,
            authRepository = authRepository,
            mapStatePreferences = mapStatePreferences,
            settingsPreferences = settingsPreferences,
            activityRepository = activityRepository,
            runServiceManager = runServiceManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addCheckpoint should add checkpoint to state`() = runTest {
        // When
        viewModel.addCheckpoint(longitude = 19.0, latitude = 50.0, name = "Test Point")

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val checkpoints = viewModel.state.value.checkpoints
        assertEquals(1, checkpoints.size)
        assertEquals("Test Point", checkpoints[0].name)
        assertEquals(19.0, checkpoints[0].position.longitude)
        assertEquals(50.0, checkpoints[0].position.latitude)
    }

    @Test
    fun `removeCheckpoint should remove checkpoint from state`() = runTest {
        // Given
        viewModel.addCheckpoint(longitude = 19.0, latitude = 50.0, name = "Point 1")
        viewModel.addCheckpoint(longitude = 20.0, latitude = 51.0, name = "Point 2")
        testDispatcher.scheduler.advanceUntilIdle()

        val checkpointId = viewModel.state.value.checkpoints[0].id

        // When
        viewModel.removeCheckpoint(checkpointId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val checkpoints = viewModel.state.value.checkpoints
        assertEquals(1, checkpoints.size)
        assertEquals("Point 2", checkpoints[0].name)
    }

    @Test
    fun `clearCheckpoints should remove all checkpoints`() = runTest {
        // Given
        viewModel.addCheckpoint(longitude = 19.0, latitude = 50.0)
        viewModel.addCheckpoint(longitude = 20.0, latitude = 51.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearCheckpoints()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.checkpoints.isEmpty())
    }

    @Test
    fun `cameraMoved should set shouldMoveCamera to false`() = runTest {
        // Given - trigger camera move by adding checkpoint
        viewModel.addCheckpoint(longitude = 19.0, latitude = 50.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.cameraMoved()

        // Then
        assertFalse(viewModel.shouldMoveCamera.value)
    }

    @Test
    fun `requestCenterCamera should set centerCameraOnce to true`() = runTest {
        // When
        viewModel.requestCenterCamera()

        // Then
        assertTrue(viewModel.centerCameraOnce.value)
    }

    @Test
    fun `startRun with empty checkpoints should set error`() = runTest {
        // Given - no checkpoints added

        // When
        viewModel.startRun()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("No control points to run", viewModel.state.value.error)
    }
}
