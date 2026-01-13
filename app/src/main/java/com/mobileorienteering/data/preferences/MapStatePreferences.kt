package com.mobileorienteering.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.model.domain.CheckpointDto
import com.mobileorienteering.data.model.domain.SavedMapState
import com.mobileorienteering.data.model.domain.toCheckpoint
import com.mobileorienteering.data.model.domain.toDto
import com.mobileorienteering.data.model.domain.Checkpoint
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mapStateDataStore by preferencesDataStore("map_state")

@Singleton
class MapStatePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val checkpointListType = Types.newParameterizedType(
        List::class.java,
        CheckpointDto::class.java
    )
    private val checkpointAdapter = moshi.adapter<List<CheckpointDto>>(checkpointListType)

    companion object {
        private val CHECKPOINTS_JSON = stringPreferencesKey("checkpoints_json")
        private val CURRENT_MAP_ID = longPreferencesKey("current_map_id")
        private val CURRENT_MAP_NAME = stringPreferencesKey("current_map_name")
        private val IS_TRACKING = booleanPreferencesKey("is_tracking")
    }

    val savedStateFlow: Flow<SavedMapState> = context.mapStateDataStore.data.map { prefs ->
        val checkpointsJson = prefs[CHECKPOINTS_JSON] ?: "[]"
        val checkpoints = try {
            checkpointAdapter.fromJson(checkpointsJson)?.map { it.toCheckpoint() } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        SavedMapState(
            checkpoints = checkpoints,
            currentMapId = prefs[CURRENT_MAP_ID],
            currentMapName = prefs[CURRENT_MAP_NAME],
            isTracking = prefs[IS_TRACKING] ?: false
        )
    }

    suspend fun getSavedState(): SavedMapState {
        return savedStateFlow.first()
    }

    suspend fun saveCheckpoints(checkpoints: List<Checkpoint>) {
        val dtoList = checkpoints.map { it.toDto() }
        val json = checkpointAdapter.toJson(dtoList)
        context.mapStateDataStore.edit { prefs ->
            prefs[CHECKPOINTS_JSON] = json
        }
    }

    suspend fun saveCurrentMap(mapId: Long?, mapName: String?) {
        context.mapStateDataStore.edit { prefs ->
            if (mapId != null) {
                prefs[CURRENT_MAP_ID] = mapId
            } else {
                prefs.remove(CURRENT_MAP_ID)
            }
            if (mapName != null) {
                prefs[CURRENT_MAP_NAME] = mapName
            } else {
                prefs.remove(CURRENT_MAP_NAME)
            }
        }
    }

    suspend fun saveTrackingState(isTracking: Boolean) {
        context.mapStateDataStore.edit { prefs ->
            prefs[IS_TRACKING] = isTracking
        }
    }

    suspend fun clearState() {
        context.mapStateDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}