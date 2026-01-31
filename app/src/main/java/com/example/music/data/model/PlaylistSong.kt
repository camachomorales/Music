package com.example.music.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistSong(
    val playlistId: String,
    val songId: String, // Puede ser local (Long convertido a String) o streaming
    val songType: String, // "local" o "streaming"
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)