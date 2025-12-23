package com.mobileorienteering.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.firstLaunchDataStore by preferencesDataStore("first_launch")

@Singleton
class FirstLaunchPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    val isFirstLaunchFlow: Flow<Boolean> = context.firstLaunchDataStore.data.map { prefs ->
        prefs[IS_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchDone() {
        context.firstLaunchDataStore.edit { prefs ->
            prefs[IS_FIRST_LAUNCH] = false
        }
    }
}
