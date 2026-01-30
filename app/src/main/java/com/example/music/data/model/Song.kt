package com.example.music.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArtUri: String? = null,
    val isStreaming: Boolean = false,
    // ✅ NUEVO: Preservar información de streaming original
    val streamingId: String? = null,        // ID original de YouTube/JioSaavn (ej: "8SbUC-UaAxE")
    val streamingProvider: String? = null   // "INNERTUBE", "JIOSAAVN", etc.
)

