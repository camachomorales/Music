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

    // Agregar al final de tu MusicRepository.kt

    suspend fun getSearchHistory(): Flow<List<SearchHistory>> {
        return searchHistoryDao.getSearchHistory(userSessionManager.getCurrentUserId())
    }

    suspend fun addSearchQuery(query: String) {
        val userId = userSessionManager.getCurrentUserId()
        searchHistoryDao.insert(
            SearchHistory(
                query = query,
                userId = userId,
                isLocal = userId == null,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearAllHistory()
    }

    suspend fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylists(userSessionManager.getCurrentUserId())
    }

    suspend fun createPlaylist(name: String, description: String?) {
        val userId = userSessionManager.getCurrentUserId()
        playlistDao.insert(
            Playlist(
                name = name,
                description = description,
                userId = userId,
                isLocal = userId == null
            )
        )
    }

    suspend fun getFavoriteLocalSongs(): Flow<List<FavoriteLocalSong>> {
        return favoriteLocalSongDao.getFavorites(userSessionManager.getCurrentUserId())
    }

    suspend fun getFavoriteStreamingSongs(): Flow<List<FavoriteStreamingSong>> {
        return favoriteStreamingSongDao.getFavorites(userSessionManager.getCurrentUserId())
    }

    suspend fun toggleLocalSongFavorite(song: Song) {
        val userId = userSessionManager.getCurrentUserId()
        val songId = song.id.toLongOrNull() ?: return

        val isFavorite = favoriteLocalSongDao.isFavorite(songId, userId)

        if (isFavorite) {
            favoriteLocalSongDao.delete(songId, userId)
        } else {
            favoriteLocalSongDao.insert(
                FavoriteLocalSong(
                    songId = songId,
                    userId = userId,
                    isLocal = userId == null
                )
            )
        }
    }

    suspend fun toggleStreamingSongFavorite(streamingSong: StreamingSong) {
        val userId = userSessionManager.getCurrentUserId()

        val isFavorite = favoriteStreamingSongDao.isFavorite(streamingSong.id, userId)

        if (isFavorite) {
            favoriteStreamingSongDao.delete(streamingSong.id, userId)
        } else {
            favoriteStreamingSongDao.insert(
                FavoriteStreamingSong(
                    streamingSongId = streamingSong.id,
                    userId = userId,
                    title = streamingSong.title,
                    artist = streamingSong.artist,
                    albumArt = streamingSong.albumArt,
                    isLocal = userId == null
                )
            )
        }
    }

    suspend fun syncDataOnLogin(userId: String) {
        // Migrar favoritos locales
        favoriteLocalSongDao.migrateToUser(userId)
        favoriteStreamingSongDao.migrateToUser(userId)

        // Migrar playlists
        playlistDao.migrateToUser(userId)

        // Migrar historial
        searchHistoryDao.migrateToUser(userId)
    }

    suspend fun clearLocalData() {
        favoriteLocalSongDao.deleteLocalFavorites()
        favoriteStreamingSongDao.deleteLocalFavorites()
        playlistDao.deleteLocalPlaylists()
        searchHistoryDao.clearLocalHistory()
    }
}