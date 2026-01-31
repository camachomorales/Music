package com.example.music.data.local.dao

import androidx.room.*
import com.example.music.data.model.FavoriteStreamingSong
import kotlinx.coroutines.flow.Flow

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