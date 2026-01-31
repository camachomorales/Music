package com.example.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.MusicRepository
import com.example.music.data.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ✅ UBICACIÓN: app/src/main/java/com/example/music/ui/viewmodel/HomeViewModel.kt
 */

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    // ========== ESTADOS ==========

    val isLoggedIn = userSessionManager.isLoggedInFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false
        )

    val userSession = userSessionManager.sessionFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserSessionManager.UserSession(
                userId = null,
                email = null,
                name = null,
                authToken = null,
                isLoggedIn = false
            )
        )

    // Favoritos locales
    private val _favoriteLocalSongs = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteLocalSongs: StateFlow<Set<Long>> = _favoriteLocalSongs.asStateFlow()

    // Favoritos streaming
    private val _favoriteStreamingSongs = MutableStateFlow<Set<String>>(emptySet())
    val favoriteStreamingSongs: StateFlow<Set<String>> = _favoriteStreamingSongs.asStateFlow()

    // Historial de búsqueda
    val searchHistory = musicRepository.getSearchHistory()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Playlists
    val playlists = musicRepository.getPlaylists()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        loadFavorites()
    }

    // ========== FAVORITOS ==========

    private fun loadFavorites() {
        viewModelScope.launch {
            // Cargar favoritos locales
            musicRepository.getFavoriteLocalSongs().collect { favorites ->
                _favoriteLocalSongs.value = favorites.map { it.songId }.toSet()
            }
        }

        viewModelScope.launch {
            // Cargar favoritos streaming
            musicRepository.getFavoriteStreamingSongs().collect { favorites ->
                _favoriteStreamingSongs.value = favorites.map { it.streamingSongId }.toSet()
            }
        }
    }

    fun isLocalSongFavorite(songId: Long): Boolean {
        return _favoriteLocalSongs.value.contains(songId)
    }

    fun isStreamingSongFavorite(songId: String): Boolean {
        return _favoriteStreamingSongs.value.contains(songId)
    }

    fun toggleLocalSongFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleLocalSongFavorite(song)
        }
    }

    fun toggleStreamingSongFavorite(streamingSong: StreamingSong) {
        viewModelScope.launch {
            musicRepository.toggleStreamingSongFavorite(streamingSong)
        }
    }

    // ========== BÚSQUEDA ==========

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            musicRepository.addSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            musicRepository.clearSearchHistory()
        }
    }

    // ========== PLAYLISTS ==========

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name, description)
        }
    }

    // ========== SESIÓN ==========

    /**
     * Llamar cuando el usuario inicie sesión exitosamente
     */
    fun onLoginSuccess(userId: String, email: String?, name: String?, authToken: String?) {
        viewModelScope.launch {
            // 1. Guardar sesión
            userSessionManager.saveUserSession(userId, email, name, authToken)

            // 2. Sincronizar datos (migrar locales + descargar del servidor)
            musicRepository.syncDataOnLogin(userId)
        }
    }

    /**
     * Llamar cuando el usuario cierre sesión
     */
    fun onLogout() {
        viewModelScope.launch {
            // 1. Limpiar datos locales (sin userId)
            musicRepository.clearLocalData()

            // 2. Limpiar sesión
            userSessionManager.clearUserSession()
        }
    }
}