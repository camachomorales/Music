package com.example.music.data.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val songs: List<Song>,
    val year: Int? = null
)