package com.example.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ✅ UBICACIÓN: app/src/main/java/com/example/music/viewmodel/HomeViewModel.kt
 *
 * VERSIÓN SIMPLIFICADA SIN HILT (usar mientras configuras las dependencias)
 */

class HomeViewModel : ViewModel() {

    // ========== ESTADOS ==========

    val isLoggedIn = MutableStateFlow(false)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false
        )

    data class UserSession(
        val userId: String? = null,
        val email: String? = null,
        val name: String? = null,
        val authToken: String? = null,
        val isLoggedIn: Boolean = false
    )

    val userSession = MutableStateFlow(
        UserSession(
            userId = null,
            email = null,
            name = null,
            authToken = null,
            isLoggedIn = false
        )
    ).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UserSession()
    )

    // Favoritos locales
    private val _favoriteLocalSongs = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteLocalSongs: StateFlow<Set<Long>> = _favoriteLocalSongs.asStateFlow()

    // Favoritos streaming
    private val _favoriteStreamingSongs = MutableStateFlow<Set<String>>(emptySet())
    val favoriteStreamingSongs: StateFlow<Set<String>> = _favoriteStreamingSongs.asStateFlow()

    // Historial de búsqueda
    val searchHistory = MutableStateFlow<List<String>>(emptyList())
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Playlists
    data class SimplePlaylist(
        val id: String,
        val name: String,
        val songs: List<Song> = emptyList()
    )

    val playlists = MutableStateFlow<List<SimplePlaylist>>(emptyList())
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // ========== FAVORITOS ==========

    fun isLocalSongFavorite(songId: Long): Boolean {
        return _favoriteLocalSongs.value.contains(songId)
    }

    fun isStreamingSongFavorite(songId: String): Boolean {
        return _favoriteStreamingSongs.value.contains(songId)
    }

    fun toggleLocalSongFavorite(song: Song) {
        viewModelScope.launch {
            val favorites = _favoriteLocalSongs.value.toMutableSet()
            if (favorites.contains(song.id)) {
                favorites.remove(song.id)
            } else {
                favorites.add(song.id)
            }
            _favoriteLocalSongs.value = favorites
        }
    }

    fun toggleStreamingSongFavorite(streamingSong: StreamingSong) {
        viewModelScope.launch {
            val favorites = _favoriteStreamingSongs.value.toMutableSet()
            if (favorites.contains(streamingSong.id)) {
                favorites.remove(streamingSong.id)
            } else {
                favorites.add(streamingSong.id)
            }
            _favoriteStreamingSongs.value = favorites
        }
    }

    // ========== BÚSQUEDA ==========

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            val currentHistory = searchHistory.value.toMutableList()
            currentHistory.remove(query) // Remover si ya existe
            currentHistory.add(0, query) // Agregar al inicio
            if (currentHistory.size > 20) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            // Aquí deberías guardar en la base de datos
            // searchHistoryRepository.add(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            // Aquí deberías limpiar la base de datos
            // searchHistoryRepository.clearAll()
        }
    }

    // ========== PLAYLISTS ==========

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            // Aquí deberías guardar en la base de datos
            // playlistRepository.create(name, description)
        }
    }

    // ========== SESIÓN ==========

    fun onLoginSuccess(userId: String, email: String?, name: String?, authToken: String?) {
        viewModelScope.launch {
            // Actualizar sesión
            // userSessionManager.saveUserSession(userId, email, name, authToken)

            // Sincronizar datos
            // musicRepository.syncDataOnLogin(userId)
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            // Limpiar datos
            _favoriteLocalSongs.value = emptySet()
            _favoriteStreamingSongs.value = emptySet()

            // Limpiar sesión
            // userSessionManager.clearUserSession()
        }
    }
}