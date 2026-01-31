package com.example.music.data.model

data class Artist(
    val id: String,
    val name: String,
    val albumArtUri: String?,
    val albums: List<Album>,
    val songs: List<Song>
)