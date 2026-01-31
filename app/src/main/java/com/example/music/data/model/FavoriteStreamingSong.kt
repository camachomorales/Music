package com.example.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_streaming_songs")
data class FavoriteStreamingSong(
    @PrimaryKey
    val streamingSongId: String,
    val userId: String?,
    val isLocal: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),

    // Datos de la canción de streaming
    val title: String,
    val artist: String,
    val albumArt: String?
)