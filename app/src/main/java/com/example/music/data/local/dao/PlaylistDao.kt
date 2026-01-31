package com.example.music.data.local.dao

import androidx.room.*
import com.example.music.data.model.Playlist
import com.example.music.data.model.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE userId = :userId OR userId IS NULL ORDER BY updatedAt DESC")
    fun getPlaylists(userId: String?): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist)

    @Update
    suspend fun update(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE isLocal = 1 AND userId IS NULL")
    suspend fun deleteLocalPlaylists()

    @Query("UPDATE playlists SET userId = :userId, isLocal = 0 WHERE userId IS NULL")
    suspend fun migrateToUser(userId: String)

    // Playlist Songs
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    fun getPlaylistSongs(playlistId: String): Flow<List<PlaylistSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSong)

    @Delete
    suspend fun deletePlaylistSong(playlistSong: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteAllPlaylistSongs(playlistId: String)
}