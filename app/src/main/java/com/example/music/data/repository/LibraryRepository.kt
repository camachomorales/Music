package com.example.music.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.music.data.model.Playlist
import com.example.music.data.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "library_prefs")

class LibraryRepository(private val context: Context) {

    private val gson = Gson()

    // Keys para DataStore
    private val PLAYLISTS_KEY = stringPreferencesKey("playlists")
    private val FAVORITES_KEY = stringPreferencesKey("favorite_song_ids")
    private val STREAMING_FAVORITES_KEY = stringPreferencesKey("streaming_favorite_ids")
    private val RECENTLY_PLAYED_KEY = stringPreferencesKey("recently_played_songs")

    // ==================== PLAYLISTS ====================

    suspend fun getAllPlaylists(): List<Playlist> {
        val prefs = context.dataStore.data.first()
        val json = prefs[PLAYLISTS_KEY] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun savePlaylists(playlists: List<Playlist>) {
        context.dataStore.edit { prefs ->
            prefs[PLAYLISTS_KEY] = gson.toJson(playlists)
        }
    }

    suspend fun createPlaylist(name: String, description: String) {
        val playlists = getAllPlaylists().toMutableList()
        val newPlaylist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name,
            description = description,
            songs = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        playlists.add(newPlaylist)
        savePlaylists(playlists)
    }

    suspend fun deletePlaylist(playlistId: String) {
        val playlists = getAllPlaylists().filter { it.id != playlistId }
        savePlaylists(playlists)
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) {
        val playlists = getAllPlaylists().map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(name = newName)
            } else {
                playlist
            }
        }
        savePlaylists(playlists)
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        val playlists = getAllPlaylists().map { playlist ->
            if (playlist.id == playlistId) {
                val updatedSongs = playlist.songs.toMutableList()
                // Evitar duplicados
                if (!updatedSongs.any { it.id == song.id }) {
                    updatedSongs.add(song)
                }
                playlist.copy(songs = updatedSongs)
            } else {
                playlist
            }
        }
        savePlaylists(playlists)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        val playlists = getAllPlaylists().map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songs = playlist.songs.filter { it.id != songId })
            } else {
                playlist
            }
        }
        savePlaylists(playlists)
    }

    suspend fun reorderPlaylistSongs(playlistId: String, newOrder: List<Song>) {
        val playlists = getAllPlaylists().map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songs = newOrder)
            } else {
                playlist
            }
        }
        savePlaylists(playlists)
    }

    // ==================== FAVORITES (LOCAL) ====================

    suspend fun getFavoriteSongIds(): Set<Long> {
        val prefs = context.dataStore.data.first()
        val json = prefs[FAVORITES_KEY] ?: return emptySet()
        return try {
            val type = object : TypeToken<Set<Long>>() {}.type
            gson.fromJson(json, type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun saveFavoriteSongIds(ids: Set<Long>) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITES_KEY] = gson.toJson(ids)
        }
    }

    suspend fun toggleFavorite(songId: Long) {
        val favorites = getFavoriteSongIds().toMutableSet()
        if (favorites.contains(songId)) {
            favorites.remove(songId)
        } else {
            favorites.add(songId)
        }
        saveFavoriteSongIds(favorites)
    }

    suspend fun addToFavorites(songId: Long) {
        val favorites = getFavoriteSongIds().toMutableSet()
        favorites.add(songId)
        saveFavoriteSongIds(favorites)
    }

    suspend fun removeFromFavorites(songId: Long) {
        val favorites = getFavoriteSongIds().toMutableSet()
        favorites.remove(songId)
        saveFavoriteSongIds(favorites)
    }

    // ==================== FAVORITES (STREAMING) ====================

    suspend fun getFavoriteStreamingSongIds(): Set<String> {
        val prefs = context.dataStore.data.first()
        val json = prefs[STREAMING_FAVORITES_KEY] ?: return emptySet()
        return try {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun saveFavoriteStreamingSongIds(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[STREAMING_FAVORITES_KEY] = gson.toJson(ids)
        }
    }

    suspend fun toggleStreamingFavorite(streamingId: String) {
        val favorites = getFavoriteStreamingSongIds().toMutableSet()
        if (favorites.contains(streamingId)) {
            favorites.remove(streamingId)
        } else {
            favorites.add(streamingId)
        }
        saveFavoriteStreamingSongIds(favorites)
    }

    suspend fun addStreamingToFavorites(streamingId: String) {
        val favorites = getFavoriteStreamingSongIds().toMutableSet()
        favorites.add(streamingId)
        saveFavoriteStreamingSongIds(favorites)
    }

    suspend fun removeStreamingFromFavorites(streamingId: String) {
        val favorites = getFavoriteStreamingSongIds().toMutableSet()
        favorites.remove(streamingId)
        saveFavoriteStreamingSongIds(favorites)
    }

    // ==================== RECENTLY PLAYED ====================

    suspend fun getRecentlyPlayedSongs(): List<Song> {
        val prefs = context.dataStore.data.first()
        val json = prefs[RECENTLY_PLAYED_KEY] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Song>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveRecentlyPlayedSongs(songs: List<Song>) {
        context.dataStore.edit { prefs ->
            prefs[RECENTLY_PLAYED_KEY] = gson.toJson(songs)
        }
    }

    suspend fun addToRecentlyPlayed(song: Song) {
        val recentSongs = getRecentlyPlayedSongs().toMutableList()
        recentSongs.removeAll { it.id == song.id }
        recentSongs.add(0, song)
        val limitedSongs = recentSongs.take(50)

        Log.d("LibraryRepository", "Saving ${limitedSongs.size} recent songs")
        saveRecentlyPlayedSongs(limitedSongs)

        // Verificar que se guardó
        val saved = getRecentlyPlayedSongs()
        Log.d("LibraryRepository", "Verified: ${saved.size} recent songs saved")
    }

    suspend fun clearRecentlyPlayed() {
        saveRecentlyPlayedSongs(emptyList())
    }
}