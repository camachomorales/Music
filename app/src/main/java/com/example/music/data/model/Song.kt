package com.example.music.data.model

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L,
    val uri: Uri? = null,
    val albumArtUri: Uri? = null,
    val playlistId: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)