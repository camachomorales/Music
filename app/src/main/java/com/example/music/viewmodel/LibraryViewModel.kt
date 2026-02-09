package com.example.music.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.local.UserEntity
import com.example.music.data.model.Playlist
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.PersistenceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ REPOSITORIO DE PERSISTENCIA
    private val persistenceRepository = PersistenceRepository(application)

    // ==================== ESTADOS ====================

    // Usuario actual
    val currentUser: StateFlow<UserEntity?> = persistenceRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Playlists desde base de datos
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // IDs de favoritos (canciones locales)
    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    // IDs de favoritos (streaming)
    private val _favoriteStreamingSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteStreamingSongIds: StateFlow<Set<String>> = _favoriteStreamingSongIds.asStateFlow()

    // Recently Played Songs (IDs)
    private val _recentlyPlayedSongIds = MutableStateFlow<List<Long>>(emptyList())

    // Recently Played Songs (objetos completos)
    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs.asStateFlow()

    // Cache de canciones para Recently Played
    private val songsCache = mutableMapOf<Long, Song>()

    init {
        Log.d("LibraryViewModel", "🎵 LibraryViewModel initialized")
        loadPersistedData()
    }

    // ==================== LOAD DATA ====================

    private fun loadPersistedData() {
        viewModelScope.launch {
            try {
                // Observar playlists
                persistenceRepository.getPlaylistsFlow(currentUser.value?.id)
                    .collect { loadedPlaylists ->
                        _playlists.value = loadedPlaylists
                        Log.d("LibraryViewModel", "✅ Loaded ${loadedPlaylists.size} playlists from DB")
                        
                        // Actualizar cache de canciones
                        loadedPlaylists.forEach { playlist ->
                            playlist.songs.forEach { song ->
                                songsCache[song.id] = song
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error loading playlists", e)
            }
        }

        viewModelScope.launch {
            try {
                // Observar favoritos locales
                persistenceRepository.getFavoriteSongIdsFlow(currentUser.value?.id)
                    .collect { ids ->
                        _favoriteSongIds.value = ids
                        Log.d("LibraryViewModel", "✅ Loaded ${ids.size} favorite songs")
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error loading favorites", e)
            }
        }

        viewModelScope.launch {
            try {
                // Observar favoritos streaming
                persistenceRepository.getFavoriteStreamingSongIdsFlow(currentUser.value?.id)
                    .collect { ids ->
                        _favoriteStreamingSongIds.value = ids
                        Log.d("LibraryViewModel", "✅ Loaded ${ids.size} streaming favorites")
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error loading streaming favorites", e)
            }
        }

        viewModelScope.launch {
            try {
                // Observar recently played
                persistenceRepository.getRecentlyPlayedFlow(currentUser.value?.id)
                    .collect { songIds ->
                        _recentlyPlayedSongIds.value = songIds
                        
                        // Convertir IDs a Song objects
                        val songs = songIds.mapNotNull { songsCache[it] }
                        _recentlyPlayedSongs.value = songs
                        
                        Log.d("LibraryViewModel", "✅ Loaded ${songs.size} recently played songs")
                    }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error loading recently played", e)
            }
        }
    }

    // ==================== PLAYLIST OPERATIONS ====================

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                val newPlaylist = Playlist(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    songs = emptyList()
                )

                // Guardar en DB
                persistenceRepository.savePlaylist(newPlaylist, currentUser.value?.id)
                
                Log.d("LibraryViewModel", "✅ Playlist created: $name")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error creating playlist", e)
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                persistenceRepository.deletePlaylist(playlistId)
                Log.d("LibraryViewModel", "✅ Playlist deleted: $playlistId")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error deleting playlist", e)
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            try {
                val playlist = _playlists.value.find { it.id == playlistId } ?: return@launch
                val updatedPlaylist = playlist.copy(name = newName)
                
                persistenceRepository.savePlaylist(updatedPlaylist, currentUser.value?.id)
                Log.d("LibraryViewModel", "✅ Playlist renamed: $newName")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error renaming playlist", e)
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            try {
                val playlist = _playlists.value.find { it.id == playlistId } ?: return@launch
                
                if (playlist.songs.any { it.id == song.id }) {
                    Log.d("LibraryViewModel", "⚠️ Song already in playlist")
                    return@launch
                }

                val updatedPlaylist = playlist.copy(
                    songs = playlist.songs + song
                )

                // Agregar a cache
                songsCache[song.id] = song

                persistenceRepository.savePlaylist(updatedPlaylist, currentUser.value?.id)
                Log.d("LibraryViewModel", "✅ Song added to playlist: ${song.title}")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error adding song to playlist", e)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            try {
                val playlist = _playlists.value.find { it.id == playlistId } ?: return@launch
                val updatedPlaylist = playlist.copy(
                    songs = playlist.songs.filter { it.id != songId }
                )

                persistenceRepository.savePlaylist(updatedPlaylist, currentUser.value?.id)
                Log.d("LibraryViewModel", "✅ Song removed from playlist")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error removing song from playlist", e)
            }
        }
    }

    fun reorderPlaylistSongs(playlistId: String, reorderedSongs: List<Song>) {
        viewModelScope.launch {
            try {
                val playlist = _playlists.value.find { it.id == playlistId } ?: return@launch
                val updatedPlaylist = playlist.copy(songs = reorderedSongs)

                persistenceRepository.savePlaylist(updatedPlaylist, currentUser.value?.id)
                Log.d("LibraryViewModel", "✅ Playlist reordered")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error reordering playlist", e)
            }
        }
    }

    // ==================== FAVORITE OPERATIONS ====================

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            try {
                if (_favoriteSongIds.value.contains(song.id)) {
                    persistenceRepository.removeFavorite(song.id, currentUser.value?.id)
                    Log.d("LibraryViewModel", "💔 Removed from favorites: ${song.title}")
                } else {
                    persistenceRepository.addFavorite(song.id, currentUser.value?.id)
                    
                    // Agregar a cache
                    songsCache[song.id] = song
                    
                    Log.d("LibraryViewModel", "❤️ Added to favorites: ${song.title}")
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error toggling favorite", e)
            }
        }
    }

    fun toggleStreamingFavorite(streamingSong: StreamingSong) {
        viewModelScope.launch {
            try {
                if (_favoriteStreamingSongIds.value.contains(streamingSong.id)) {
                    persistenceRepository.removeStreamingFavorite(streamingSong.id, currentUser.value?.id)
                    Log.d("LibraryViewModel", "💔 Removed streaming from favorites: ${streamingSong.title}")
                } else {
                    persistenceRepository.addStreamingFavorite(streamingSong.id, currentUser.value?.id)
                    
                    // Crear Song y agregar a cache
                    val song = Song(
                        id = streamingSong.id.hashCode().toLong(),
                        title = streamingSong.title,
                        artist = streamingSong.artist,
                        album = streamingSong.album ?: "Unknown",
                        duration = streamingSong.duration,
                        path = "streaming://${streamingSong.provider.name.lowercase()}/${streamingSong.id}",
                        albumArtUri = streamingSong.thumbnailUrl,
                        isStreaming = true,
                        streamingId = streamingSong.id,
                        streamingProvider = streamingSong.provider.name
                    )
                    songsCache[song.id] = song
                    
                    Log.d("LibraryViewModel", "❤️ Added streaming to favorites: ${streamingSong.title}")
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error toggling streaming favorite", e)
            }
        }
    }

    // ==================== RECENTLY PLAYED OPERATIONS ====================

    fun addToRecentlyPlayed(song: Song) {
        viewModelScope.launch {
            try {
                // Agregar a cache
                songsCache[song.id] = song
                
                persistenceRepository.addRecentlyPlayed(song.id, currentUser.value?.id)
                Log.d("LibraryViewModel", "📝 Added to recently played: ${song.title}")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error adding to recently played", e)
            }
        }
    }

    fun clearRecentlyPlayed() {
        viewModelScope.launch {
            try {
                persistenceRepository.clearRecentlyPlayed(currentUser.value?.id)
                Log.d("LibraryViewModel", "🗑️ Cleared recently played")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error clearing recently played", e)
            }
        }
    }

    // ==================== USER OPERATIONS ====================

    suspend fun loginUser(email: String, password: String): Result<UserEntity> {
        return try {
            val result = persistenceRepository.loginUser(email, password)
            if (result.isSuccess) {
                Log.d("LibraryViewModel", "✅ User logged in: $email")
                // Recargar datos del usuario
                loadPersistedData()
            }
            result
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "❌ Error logging in", e)
            Result.failure(e)
        }
    }

    suspend fun registerUser(email: String, password: String, userName: String): Result<UserEntity> {
        return try {
            val result = persistenceRepository.registerUser(email, password, userName)
            if (result.isSuccess) {
                Log.d("LibraryViewModel", "✅ User registered: $email")
                // Recargar datos del usuario
                loadPersistedData()
            }
            result
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "❌ Error registering user", e)
            Result.failure(e)
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            try {
                persistenceRepository.logoutUser()
                Log.d("LibraryViewModel", "👋 User logged out")
                
                // Limpiar estados
                _playlists.value = emptyList()
                _favoriteSongIds.value = emptySet()
                _favoriteStreamingSongIds.value = emptySet()
                _recentlyPlayedSongs.value = emptyList()
                songsCache.clear()
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error logging out", e)
            }
        }
    }

    fun syncUserData() {
        viewModelScope.launch {
            try {
                val user = currentUser.value
                if (user != null) {
                    persistenceRepository.syncUserData(user.id)
                    Log.d("LibraryViewModel", "🔄 Syncing user data")
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "❌ Error syncing data", e)
            }
        }
    }
}
