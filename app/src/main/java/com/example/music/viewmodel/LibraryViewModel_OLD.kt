package com.example.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.model.Playlist
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.LibraryRepository
import com.example.music.data.repository.StreamingMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel_OLD(application: Application) : AndroidViewModel(application) {

    private val libraryRepository = LibraryRepository(application.applicationContext)
    private val streamingRepository = StreamingMusicRepository()

    // ==================== PLAYLISTS (OFFLINE) ====================
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // ==================== STREAMING MODE ====================
    private val _streamingSongs = MutableStateFlow<List<StreamingSong>>(emptyList())
    val streamingSongs: StateFlow<List<StreamingSong>> = _streamingSongs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ==================== FAVORITES (LOCAL) ====================
    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    // ==================== FAVORITES (STREAMING) ====================
    private val _favoriteStreamingSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteStreamingSongIds: StateFlow<Set<String>> = _favoriteStreamingSongIds.asStateFlow()

    // ==================== RECENTLY PLAYED ====================
    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs.asStateFlow()

    // ==================== USER AUTHENTICATION ====================
    data class User(
        val id: String,
        val userName: String,
        val email: String,
        val isAdmin: Boolean = false
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadPlaylists()
        loadFavorites()
        loadStreamingFavorites()
        loadRecentlyPlayed()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            _playlists.value = libraryRepository.getAllPlaylists()
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _favoriteSongIds.value = libraryRepository.getFavoriteSongIds()
        }
    }

    private fun loadStreamingFavorites() {
        viewModelScope.launch {
            _favoriteStreamingSongIds.value = libraryRepository.getFavoriteStreamingSongIds()
        }
    }

    private fun loadRecentlyPlayed() {
        viewModelScope.launch {
            _recentlyPlayedSongs.value = libraryRepository.getRecentlyPlayedSongs()
        }
    }

    // ==================== PLAYLIST METHODS ====================

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            libraryRepository.createPlaylist(name, description)
            loadPlaylists()
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            libraryRepository.deletePlaylist(playlistId)
            loadPlaylists()
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            libraryRepository.renamePlaylist(playlistId, newName)
            loadPlaylists()
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            libraryRepository.addSongToPlaylist(playlistId, song)
            loadPlaylists()
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            libraryRepository.removeSongFromPlaylist(playlistId, songId)
            loadPlaylists()
        }
    }

    fun reorderPlaylistSongs(playlistId: String, newOrder: List<Song>) {
        viewModelScope.launch {
            libraryRepository.reorderPlaylistSongs(playlistId, newOrder)
            loadPlaylists()
        }
    }

    // ==================== FAVORITES METHODS (LOCAL) ====================

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            libraryRepository.toggleFavorite(song.id)
            loadFavorites()
        }
    }

    fun isFavorite(songId: Long): Boolean {
        return _favoriteSongIds.value.contains(songId)
    }

    fun addToFavorites(song: Song) {
        viewModelScope.launch {
            libraryRepository.addToFavorites(song.id)
            loadFavorites()
        }
    }

    fun removeFromFavorites(songId: Long) {
        viewModelScope.launch {
            libraryRepository.removeFromFavorites(songId)
            loadFavorites()
        }
    }

    // ==================== FAVORITES METHODS (STREAMING) ====================

    fun toggleStreamingFavorite(streamingSong: StreamingSong) {
        viewModelScope.launch {
            libraryRepository.toggleStreamingFavorite(streamingSong.id)
            loadStreamingFavorites()
        }
    }

    fun isStreamingFavorite(streamingSongId: String): Boolean {
        return _favoriteStreamingSongIds.value.contains(streamingSongId)
    }

    fun addStreamingToFavorites(streamingSong: StreamingSong) {
        viewModelScope.launch {
            libraryRepository.addStreamingToFavorites(streamingSong.id)
            loadStreamingFavorites()
        }
    }

    fun removeStreamingFromFavorites(streamingSongId: String) {
        viewModelScope.launch {
            libraryRepository.removeStreamingFromFavorites(streamingSongId)
            loadStreamingFavorites()
        }
    }

    // ==================== RECENTLY PLAYED METHODS ====================

    fun addToRecentlyPlayed(song: Song) {
        viewModelScope.launch {
            libraryRepository.addToRecentlyPlayed(song)
            loadRecentlyPlayed()
        }
    }

    fun clearRecentlyPlayed() {
        viewModelScope.launch {
            libraryRepository.clearRecentlyPlayed()
            loadRecentlyPlayed()
        }
    }

    // ==================== STREAMING METHODS ====================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchStreamingMusic() {
        if (_searchQuery.value.isBlank()) return

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = streamingRepository.search(_searchQuery.value, limit = 30)
                _streamingSongs.value = results
            } catch (e: Exception) {
                _streamingSongs.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun playStreamingSong(streamingSong: StreamingSong) {
        viewModelScope.launch {
            // Obtener URL de streaming
            val streamUrl = streamingRepository.getStreamUrl(streamingSong)

            if (streamUrl != null) {
                // Aquí deberías llamar al MusicPlayerViewModel
                // pero como no tenemos referencia, esto se debe manejar
                // desde MainNavigation directamente
            }
        }
    }

    fun getTrendingSongs() {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val trending = streamingRepository.getTrending(limit = 30)
                _streamingSongs.value = trending
            } catch (e: Exception) {
                _streamingSongs.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    // ==================== USER AUTHENTICATION METHODS ====================

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            // TODO: Implementar lógica real de login con tu backend/API
            // Por ahora, simulación básica:
            if (email.isNotBlank() && password.isNotBlank()) {
                val user = User(
                    id = "user_${System.currentTimeMillis()}",
                    userName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    isAdmin = email.contains("admin", ignoreCase = true) // Admin si el email contiene "admin"
                )
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Email and password are required"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(email: String, password: String, name: String): Result<User> {
        return try {
            // TODO: Implementar lógica real de registro con tu backend/API
            if (email.isNotBlank() && password.isNotBlank() && name.isNotBlank()) {
                // Validar formato de email básico
                if (!email.contains("@")) {
                    return Result.failure(Exception("Invalid email format"))
                }

                // Validar longitud de contraseña
                if (password.length < 6) {
                    return Result.failure(Exception("Password must be at least 6 characters"))
                }

                val user = User(
                    id = "user_${System.currentTimeMillis()}",
                    userName = name,
                    email = email,
                    isAdmin = false
                )
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("All fields are required"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            _currentUser.value = null
            // Opcional: Limpiar datos sensibles si es necesario
        }
    }

    suspend fun syncUserData() {
        // TODO: Implementar sincronización real con tu backend
        // Por ahora solo simulamos un proceso
        kotlinx.coroutines.delay(1500)

        // Aquí podrías sincronizar:
        // - Playlists del usuario
        // - Favoritos
        // - Historial de reproducción
        // - Configuraciones

        loadPlaylists()
        loadFavorites()
        loadStreamingFavorites()
        loadRecentlyPlayed()
    }
}