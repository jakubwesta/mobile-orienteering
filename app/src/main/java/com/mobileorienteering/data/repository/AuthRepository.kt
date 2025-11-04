package com.mobileorienteering.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore("auth")

class AuthRepository(private val context: Context) {

    companion object {
        private val TOKEN = stringPreferencesKey("token")
        private val USER_ID = stringPreferencesKey("user_id")
    }

    val isLoggedInFlow: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        !prefs[TOKEN].isNullOrEmpty()
    }

    val authModelFlow: Flow<AuthModel?> = context.authDataStore.data.map { prefs ->
        val token = prefs[TOKEN]
        val userId = prefs[USER_ID]
        if (token != null && userId != null) AuthModel(token, userId) else null
    }

    // TODO: Api
    suspend fun login(model: LoginModel): AuthModel {
        val auth = AuthModel(
            token = "fake-token-${model.email}",
            userId = "user-${model.email.hashCode()}"
        )
        context.authDataStore.edit { prefs ->
            prefs[TOKEN] = auth.token
            prefs[USER_ID] = auth.userId
        }
        return auth
    }

    // TODO: Api
    suspend fun register(model: RegisterModel): AuthModel {
        val auth = AuthModel(
            token = "fake-token-${model.email}",
            userId = "user-${model.email.hashCode()}"
        )
        context.authDataStore.edit { prefs ->
            prefs[TOKEN] = auth.token
            prefs[USER_ID] = auth.userId
        }
        return auth
    }

    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs.remove(TOKEN)
            prefs.remove(USER_ID)
        }
    }

    suspend fun getCurrentAuth(): AuthModel? {
        val prefs = context.authDataStore.data.first()
        val token = prefs[TOKEN]
        val userId = prefs[USER_ID]
        return if (token != null && userId != null) AuthModel(token, userId) else null
    }
}
