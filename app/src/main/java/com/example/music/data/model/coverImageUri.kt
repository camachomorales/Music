package com.example.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val coverImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val isLocal: Boolean = true,
    val songs: List<Song> = emptyList() // Para uso en UI, no se guarda en DB
) {
    // Constructor secundario para Room (sin songs)
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        description: String? = null,
        coverImageUri: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        userId: String? = null,
        isLocal: Boolean = true
    ) : this(id, name, description, coverImageUri, createdAt, userId, isLocal, emptyList())
}