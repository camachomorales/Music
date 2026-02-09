package com.example.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.repository.SyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
    object NotAuthenticated : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Verifica el estado de autenticación al iniciar la app
     */
    private fun checkAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Success(currentUser)
            // Sincronizar datos desde la nube
            syncFromCloud()
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    /**
     * Iniciar sesión con email y contraseña
     */
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    _authState.value = AuthState.Success(user)
                    // Sincronizar datos desde la nube después de iniciar sesión
                    syncFromCloud()
                } else {
                    _authState.value = AuthState.Error("Error al iniciar sesión")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Registrar nuevo usuario
     */
    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    _authState.value = AuthState.Success(user)
                    // Los datos locales existentes se sincronizarán automáticamente
                    syncToCloud()
                } else {
                    _authState.value = AuthState.Error("Error al crear cuenta")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Cerrar sesión
     */
    fun signOut(clearLocalData: Boolean = false) {
        viewModelScope.launch {
            try {
                if (clearLocalData) {
                    // Limpiar datos locales si el usuario lo desea
                    // COMENTADO TEMPORALMENTE
                    // syncRepository.clearLocalData()
                }

                auth.signOut()
                _authState.value = AuthState.NotAuthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al cerrar sesión")
            }
        }
    }

    /**
     * Continuar sin cuenta
     */
    fun continueWithoutAccount() {
        _authState.value = AuthState.NotAuthenticated
    }

    /**
     * Sincronizar datos a la nube
     */
    fun syncToCloud() {
        if (!syncRepository.isUserLoggedIn()) {
            return
        }

        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Syncing
                syncRepository.syncToCloud()
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error al sincronizar")
            }
        }
    }

    /**
     * Sincronizar datos desde la nube
     */
    fun syncFromCloud() {
        if (!syncRepository.isUserLoggedIn()) {
            return
        }

        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Syncing
                syncRepository.syncFromCloud()
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error al sincronizar")
            }
        }
    }

    /**
     * Obtiene el usuario actual
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Verifica si hay usuario logueado
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}