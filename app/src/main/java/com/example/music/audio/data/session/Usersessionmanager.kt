package com.example.music.data.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ✅ UBICACIÓN: app/src/main/java/com/example/music/data/session/UserSessionManager.kt
 *
 * VERSIÓN SIN HILT - Instanciar manualmente
 */

class UserSessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    data class UserSession(
        val userId: String? = null,
        val email: String? = null,
        val name: String? = null,
        val authToken: String? = null,
        val isLoggedIn: Boolean = false
    )

    private val _sessionFlow = MutableStateFlow(loadSession())
    val sessionFlow: StateFlow<UserSession> = _sessionFlow.asStateFlow()

    private val _isLoggedInFlow = MutableStateFlow(loadSession().isLoggedIn)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private fun loadSession(): UserSession {
        return UserSession(
            userId = prefs.getString("user_id", null),
            email = prefs.getString("email", null),
            name = prefs.getString("name", null),
            authToken = prefs.getString("auth_token", null),
            isLoggedIn = prefs.getBoolean("is_logged_in", false)
        )
    }

    fun saveUserSession(userId: String, email: String?, name: String?, authToken: String?) {
        prefs.edit().apply {
            putString("user_id", userId)
            putString("email", email)
            putString("name", name)
            putString("auth_token", authToken)
            putBoolean("is_logged_in", true)
            apply()
        }

        val session = UserSession(
            userId = userId,
            email = email,
            name = name,
            authToken = authToken,
            isLoggedIn = true
        )
        _sessionFlow.value = session
        _isLoggedInFlow.value = true
    }

    fun clearUserSession() {
        prefs.edit().clear().apply()

        _sessionFlow.value = UserSession()
        _isLoggedInFlow.value = false
    }

    fun getUserId(): String? = _sessionFlow.value.userId

    companion object {
        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}