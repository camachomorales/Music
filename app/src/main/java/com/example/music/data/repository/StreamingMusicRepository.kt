// app/src/main/java/com/example/music/data/repository/StreamingMusicRepository.kt
package com.example.music.data.repository

import android.util.Log
import com.example.music.data.api.MusicProviderManager
import com.example.music.data.api.MusicProviderType
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StreamingMusicRepository {

    private val TAG = "StreamingRepo"
    private val providerManager = MusicProviderManager()

    init {
        // ✅ Configuración: InnerTube (YouTube Music nativo) + JioSaavn
        Log.d(TAG, "🎵 Inicializando providers: InnerTube + JioSaavn")
        providerManager.setEnabledProviders(
            setOf(
                MusicProviderType.INNERTUBE,     // YouTube Music nativo (InnerTube API)
                MusicProviderType.JIOSAAVN        // JioSaavn para música india
            )
        )
    }

    suspend fun search(query: String, limit: Int = 30): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Buscando: '$query'")

            val results = providerManager.search(query, limit)

            Log.d(TAG, "✅ ${results.size} canciones encontradas")
            results.forEach { song ->
                Log.d(TAG, "🎵 ${song.title} - ${song.artist} (${song.provider.displayName})")
            }

            results
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en búsqueda: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getStreamUrl(song: StreamingSong): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Obteniendo stream URL para: ${song.id}")

            val streamUrl = providerManager.getStreamUrl(song.id, song.provider)

            if (streamUrl != null) {
                Log.d(TAG, "✅ Stream URL obtenida")
            } else {
                Log.w(TAG, "⚠️ No se pudo obtener stream URL")
            }

            streamUrl
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream URL: ${e.message}", e)
            null
        }
    }

    suspend fun getTrending(limit: Int = 30): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔥 Obteniendo trending")
            providerManager.getTrending(limit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo trending: ${e.message}", e)
            emptyList()
        }
    }
}