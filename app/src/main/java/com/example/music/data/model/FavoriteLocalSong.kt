package com.example.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_local_songs")
data class FavoriteLocalSong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val userId: String?,
    val isLocal: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)