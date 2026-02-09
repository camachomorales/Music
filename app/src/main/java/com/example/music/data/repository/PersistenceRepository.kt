package com.example.music.data.repository

import android.content.Context
import android.util.Log
import com.example.music.data.local.*
import com.example.music.data.model.Playlist
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersistenceRepository(context: Context) {
    
    private val database = MusicDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val playlistDao = database.playlistDao()
    private val favoriteDao = database.favoriteDao()
    private val recentlyPlayedDao = database.recentlyPlayedDao()

    // ==================== USER OPERATIONS ====================

    suspend fun getCurrentUser(): UserEntity? {
        return try {
            userDao.getUserById("current") // Usamos un ID fijo para el usuario actual
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error getting current user", e)
            null
        }
    }

    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow()
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> {
        return try {
            // TODO: Implementar validación real con backend
            val existingUser = userDao.getUserByEmail(email)
            
            if (existingUser != null) {
                // Usuario existe
                Result.success(existingUser)
            } else {
                // Usuario no existe, crearlo
                val newUser = UserEntity(
                    id = "current",
                    email = email,
                    userName = email.substringBefore("@"),
                    isAdmin = email.contains("admin")
                )
                userDao.insertUser(newUser)
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error logging in user", e)
            Result.failure(e)
        }
    }

    suspend fun registerUser(email: String, password: String, userName: String): Result<UserEntity> {
        return try {
            val existingUser = userDao.getUserByEmail(email)
            
            if (existingUser != null) {
                Result.failure(Exception("User already exists"))
            } else {
                val newUser = UserEntity(
                    id = "current",
                    email = email,
                    userName = userName,
                    isAdmin = false
                )
                userDao.insertUser(newUser)
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error registering user", e)
            Result.failure(e)
        }
    }

    suspend fun logoutUser() {
        try {
            val currentUser = getCurrentUser()
            currentUser?.let { userDao.deleteUser(it) }
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error logging out user", e)
        }
    }

    // ==================== PLAYLIST OPERATIONS ====================

    fun getPlaylistsFlow(userId: String?): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsForUser(userId).map { playlistsWithSongs ->
            playlistsWithSongs.map { it.toPlaylist() }
        }
    }

    suspend fun savePlaylist(playlist: Playlist, userId: String?) {
        try {
            val playlistEntity = PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                userId = userId,
                coverUri = playlist.coverUri,
                createdAt = playlist.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            
            playlistDao.insertPlaylist(playlistEntity)
            
            // Guardar canciones
            playlistDao.clearPlaylistSongs(playlist.id)
            playlist.songs.forEachIndexed { index, song ->
                val songEntity = song.toSongEntity()
                playlistDao.insertSong(songEntity)
                
                val crossRef = PlaylistSongCrossRef(
                    playlistId = playlist.id,
                    songId = song.id,
                    position = index
                )
                playlistDao.insertPlaylistSong(crossRef)
            }
            
            Log.d("PersistenceRepository", "✅ Playlist saved: ${playlist.name} with ${playlist.songs.size} songs")
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "❌ Error saving playlist: ${playlist.name}", e)
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        try {
            val playlist = playlistDao.getPlaylistById(playlistId)
            playlist?.let { playlistDao.deletePlaylist(it) }
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error deleting playlist", e)
        }
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        try {
            val playlistEntity = PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                userId = null, // Mantener userId original
                coverUri = playlist.coverUri,
                createdAt = playlist.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            playlistDao.updatePlaylist(playlistEntity)
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error updating playlist", e)
        }
    }

    // ==================== FAVORITE OPERATIONS ====================

    fun getFavoriteSongIdsFlow(userId: String?): Flow<Set<Long>> {
        return favoriteDao.getFavoriteSongIds(userId).map { it.filterNotNull().toSet() }
    }

    fun getFavoriteStreamingSongIdsFlow(userId: String?): Flow<Set<String>> {
        return favoriteDao.getFavoriteStreamingSongIds(userId).map { it.filterNotNull().toSet() }
    }

    suspend fun addFavorite(songId: Long, userId: String?) {
        try {
            val favorite = FavoriteEntity(
                songId = songId,
                streamingSongId = null,
                userId = userId
            )
            favoriteDao.insertFavorite(favorite)
            Log.d("PersistenceRepository", "✅ Added favorite: songId=$songId")
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error adding favorite", e)
        }
    }

    suspend fun addStreamingFavorite(streamingSongId: String, userId: String?) {
        try {
            val favorite = FavoriteEntity(
                songId = null,
                streamingSongId = streamingSongId,
                userId = userId
            )
            favoriteDao.insertFavorite(favorite)
            Log.d("PersistenceRepository", "✅ Added streaming favorite: $streamingSongId")
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error adding streaming favorite", e)
        }
    }

    suspend fun removeFavorite(songId: Long, userId: String?) {
        try {
            favoriteDao.deleteFavoriteBySongId(songId, userId)
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error removing favorite", e)
        }
    }

    suspend fun removeStreamingFavorite(streamingSongId: String, userId: String?) {
        try {
            favoriteDao.deleteFavoriteByStreamingSongId(streamingSongId, userId)
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error removing streaming favorite", e)
        }
    }

    // ==================== RECENTLY PLAYED OPERATIONS ====================

    fun getRecentlyPlayedFlow(userId: String?): Flow<List<Long>> {
        return recentlyPlayedDao.getRecentlyPlayedForUser(userId).map { list ->
            list.map { it.songId }
        }
    }

    suspend fun addRecentlyPlayed(songId: Long, userId: String?) {
        try {
            val recentlyPlayed = RecentlyPlayedEntity(
                songId = songId,
                userId = userId
            )
            recentlyPlayedDao.insertRecentlyPlayed(recentlyPlayed)
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error adding recently played", e)
        }
    }

    suspend fun clearRecentlyPlayed(userId: String?) {
        try {
            recentlyPlayedDao.clearRecentlyPlayed(userId)
        } catch (e: Exception) {
            Log.e("PersistenceRepository", "Error clearing recently played", e)
        }
    }

    // ==================== SYNC OPERATIONS ====================

    suspend fun syncUserData(userId: String) {
        // TODO: Implementar sincronización con backend
        Log.d("PersistenceRepository", "🔄 Syncing data for user: $userId")
    }

    // ==================== CONVERSION FUNCTIONS ====================

    private fun PlaylistWithSongs.toPlaylist(): Playlist {
        return Playlist(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            coverUri = playlist.coverUri,
            songs = songs.map { it.toSong() },
            createdAt = playlist.createdAt
        )
    }

    private fun SongEntity.toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            path = path,
            albumArtUri = albumArtUri,
            isStreaming = isStreaming,
            streamingId = streamingId,
            streamingProvider = streamingProvider
        )
    }

    private fun Song.toSongEntity(): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            path = path,
            albumArtUri = albumArtUri,
            isStreaming = isStreaming,
            streamingId = streamingId,
            streamingProvider = streamingProvider
        )
    }
}
