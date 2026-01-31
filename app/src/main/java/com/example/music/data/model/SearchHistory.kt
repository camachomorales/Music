package com.example.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val userId: String?,
    val isLocal: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)