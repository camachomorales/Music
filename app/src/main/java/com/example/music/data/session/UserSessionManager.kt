package com.example.music.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ UBICACIÓN: app/src/main/java/com/example/music/data/session/UserSessionManager.kt
 *
 * Gestiona la sesión del usuario usando DataStore (persistente entre cierres de app)
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    private val dataStore = context.dataStore

    // ========== FLOWS OBSERVABLES ==========

    val userIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    val userEmailFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN_KEY] ?: false
    }

    val authTokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN_KEY]
    }

    // ========== MÉTODOS SUSPEND ==========

    suspend fun saveUserSession(
        userId: String,
        email: String?,
        name: String?,
        authToken: String?
    ) {
        dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[IS_LOGGED_IN_KEY] = true
            email?.let { prefs[USER_EMAIL_KEY] = it }
            name?.let { prefs[USER_NAME_KEY] = it }
            authToken?.let { prefs[AUTH_TOKEN_KEY] = it }
        }
    }

    suspend fun clearUserSession() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun updateAuthToken(token: String) {
        dataStore.edit { prefs ->
            prefs[AUTH_TOKEN_KEY] = token
        }
    }

    // ========== MÉTODOS DE LECTURA DIRECTA ==========

    suspend fun getCurrentUserId(): String? {
        return userIdFlow.first()
    }

    suspend fun getCurrentUserEmail(): String? {
        return userEmailFlow.first()
    }

    suspend fun getCurrentUserName(): String? {
        return userNameFlow.first()
    }

    suspend fun isLoggedIn(): Boolean {
        return isLoggedInFlow.first()
    }

    suspend fun getAuthToken(): String? {
        return authTokenFlow.first()
    }

    // ========== DATOS DE SESIÓN COMPLETOS ==========

    data class UserSession(
        val userId: String?,
        val email: String?,
        val name: String?,
        val authToken: String?,
        val isLoggedIn: Boolean
    )

    val sessionFlow: Flow<UserSession> = dataStore.data.map { prefs ->
        UserSession(
            userId = prefs[USER_ID_KEY],
            email = prefs[USER_EMAIL_KEY],
            name = prefs[USER_NAME_KEY],
            authToken = prefs[AUTH_TOKEN_KEY],
            isLoggedIn = prefs[IS_LOGGED_IN_KEY] ?: false
        )
    }

    suspend fun getSession(): UserSession {
        return sessionFlow.first()
    }
}