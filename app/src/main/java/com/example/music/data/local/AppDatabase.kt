package com.example.music.data.local

import androidx.room.*
import com.example.music.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * ✅ UBICACIÓN: app/src/main/java/com/example/music/data/local/AppDatabase.kt
 */

@Database(
    entities = [
        FavoriteLocalSong::class,
        FavoriteStreamingSong::class,
        Playlist::class,
        PlaylistSong::class,
        SearchHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteLocalSongDao(): FavoriteLocalSongDao
    abstract fun favoriteStreamingSongDao(): FavoriteStreamingSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}

// ========== DAOs ==========

@Dao
interface FavoriteLocalSongDao {
    @Query("SELECT * FROM favorite_local_songs WHERE userId = :userId OR userId IS NULL ORDER BY addedAt DESC")
    fun getFavorites(userId: String?): Flow<List<FavoriteLocalSong>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_local_songs WHERE songId = :songId AND (userId = :userId OR userId IS NULL))")
    suspend fun isFavorite(songId: Long, userId: String?): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteLocalSong)

    @Query("DELETE FROM favorite_local_songs WHERE songId = :songId AND (userId = :userId OR userId IS NULL)")
    suspend fun delete(songId: Long, userId: String?)

    @Query("DELETE FROM favorite_local_songs WHERE isLocal = 1 AND userId IS NULL")
    suspend fun deleteLocalFavorites()

    @Query("UPDATE favorite_local_songs SET userId = :userId, isLocal = 0 WHERE userId IS NULL")
    suspend fun migrateToUser(userId: String)
}

@Dao
interface FavoriteStreamingSongDao {
    @Query("SELECT * FROM favorite_streaming_songs WHERE userId = :userId OR userId IS NULL ORDER BY addedAt DESC")
    fun getFavorites(userId: String?): Flow<List<FavoriteStreamingSong>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_streaming_songs WHERE streamingSongId = :songId AND (userId = :userId OR userId IS NULL))")
    suspend fun isFavorite(songId: String, userId: String?): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteStreamingSong)

    @Query("DELETE FROM favorite_streaming_songs WHERE streamingSongId = :songId AND (userId = :userId OR userId IS NULL)")
    suspend fun delete(songId: String, userId: String?)

    @Query("DELETE FROM favorite_streaming_songs WHERE isLocal = 1 AND userId IS NULL")
    suspend fun deleteLocalFavorites()

    @Query("UPDATE favorite_streaming_songs SET userId = :userId, isLocal = 0 WHERE userId IS NULL")
    suspend fun migrateToUser(userId: String)
}

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

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE userId = :userId OR userId IS NULL ORDER BY timestamp DESC LIMIT 20")
    fun getSearchHistory(userId: String?): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistory)

    @Query("DELETE FROM search_history WHERE userId IS NULL AND isLocal = 1")
    suspend fun clearLocalHistory()

    @Query("DELETE FROM search_history")
    suspend fun clearAllHistory()

    @Query("UPDATE search_history SET userId = :userId, isLocal = 0 WHERE userId IS NULL")
    suspend fun migrateToUser(userId: String)
}

@Database(
    entities = [
        Playlist::class,
        Song::class,
        SearchHistory::class
    ],
    version = 1,
    exportSchema = false
)
@Database(
    entities = [
        Playlist::class,
        Song::class,
        SearchHistory::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
