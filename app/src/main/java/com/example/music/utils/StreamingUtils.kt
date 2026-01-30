// app/src/main/java/com/example/music/utils/StreamingUtils.kt
package com.example.music.utils

/**
 * Extrae el ID de video desde un path de streaming
 * Ejemplo: "streaming://youtube/ABC123" -> "ABC123"
 */
fun getVideoIdFromPath(path: String): String? {
    return when {
        path.startsWith("streaming://youtube/") ->
            path.removePrefix("streaming://youtube/")
        path.startsWith("streaming://jiosaavn/") ->
            path.removePrefix("streaming://jiosaavn/")
        path.startsWith("streaming://spotify/") ->
            path.removePrefix("streaming://spotify/")
        else -> null
    }
}

/**
 * Verifica si una canción es de streaming
 */
fun isStreamingSong(path: String): Boolean {
    return path.startsWith("streaming://")
}