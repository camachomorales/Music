package com.example.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.music.data.model.Song

class MusicRepository(private val context: Context) {

    fun getSongsFromDevice(): List<Song> {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(dataColumn) ?: ""

                // Obtener la URI del álbum de arte
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    cursor.getLong(albumIdColumn)
                ).toString()

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    path = path,
                    albumArtUri = albumArtUri
                )

                songs.add(song)
            }
        }

        return songs
    }
}


/**
 * ✅ UBICACIÓN: app/src/main/java/com/example/music/data/repository/MusicRepositoryExtensions.kt
 *
 * Funciones adicionales que deben agregarse a MusicRepository:
 */

// Agregar estas funciones a tu MusicRepository existente:

/*
    // ========== SEARCH HISTORY ==========
    fun getSearchHistory(): Flow<List<String>>
    suspend fun addSearchQuery(query: String)
    suspend fun clearSearchHistory()

    // ========== PLAYLISTS ==========
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String, description: String?)

    // ========== FAVORITOS LOCALES ==========
    fun getFavoriteLocalSongs(): Flow<List<FavoriteLocalSong>>
    suspend fun toggleLocalSongFavorite(song: Song)

    // ========== FAVORITOS STREAMING ==========
    fun getFavoriteStreamingSongs(): Flow<List<FavoriteStreamingSong>>
    suspend fun toggleStreamingSongFavorite(streamingSong: StreamingSong)

    // ========== SINCRONIZACIÓN ==========
    suspend fun syncDataOnLogin(userId: String)
    suspend fun clearLocalData()
*/

// Modelos de datos para favoritos (agregar a tu carpeta de modelos):

data class FavoriteLocalSong(
    val id: Long = 0,
    val songId: Long,
    val userId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

data class FavoriteStreamingSong(
    val id: Long = 0,
    val streamingSongId: String,
    val userId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

data class SearchHistoryEntry(
    val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)