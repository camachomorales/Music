package com.example.music.data.model
/*
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,  // ✅ NOMBRE CORRECTO
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isUserCreated: Boolean = true
)*/

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val coverUri: String? = null
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverUri: String? = null,
    val year: Int? = null,
    val songs: List<Song> = emptyList()
)

data class Artist(
    val id: String,
    val name: String,
    val imageUri: String? = null,
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList()
)




