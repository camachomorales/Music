package com.example.music.data.local.dao

import androidx.room.*
import com.example.music.data.model.SearchHistory
import kotlinx.coroutines.flow.Flow

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