package com.example.music.data.local.dao

import androidx.room.*
import com.example.music.data.model.FavoriteLocalSong
import kotlinx.coroutines.flow.Flow

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