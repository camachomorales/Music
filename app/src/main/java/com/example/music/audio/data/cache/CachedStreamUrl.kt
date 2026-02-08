// app/src/main/java/com/example/music/data/cache/StreamUrlCache.kt
package com.example.music.data.cache

import android.util.Log
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.StreamingMusicRepository
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

data class CachedStreamUrl(
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresIn: Long = 3600000 // 1 hora
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > expiresIn
}

class StreamUrlCache(
    private val streamingRepository: StreamingMusicRepository
) {
    private val TAG = "StreamUrlCache"

    // Cache thread-safe
    private val cache = ConcurrentHashMap<String, CachedStreamUrl>()

    // Jobs activos de pre-carga
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Obtiene URL del cache o la descarga si no existe
     * ⚡ RÁPIDO: Si está en cache, retorna inmediatamente
     * 🌐 Si no está, descarga y cachea
     */
    suspend fun getStreamUrl(song: StreamingSong): String? = withContext(Dispatchers.IO) {
        // 1. Verificar cache
        val cached = cache[song.id]
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "⚡ Cache HIT: ${song.title}")
            return@withContext cached.url
        }

        // 2. Cache miss - descargar
        Log.d(TAG, "🌐 Cache MISS: ${song.title} - Descargando...")
        val url = streamingRepository.getStreamUrl(song)

        if (url != null) {
            cache[song.id] = CachedStreamUrl(url)
            Log.d(TAG, "✅ Cacheada: ${song.title}")
        }

        url
    }

    /**
     * Pre-carga canciones en segundo plano
     * 🔥 NO bloquea - se ejecuta en paralelo
     */
    fun preloadSongs(songs: List<StreamingSong>, priority: Int = 0) {
        songs.forEachIndexed { index, song ->
            // Cancelar job anterior si existe
            activeJobs[song.id]?.cancel()

            // Calcular delay según prioridad
            val delay = when (priority) {
                0 -> index * 200L  // Siguiente canción: carga inmediata
                1 -> index * 500L  // Segunda siguiente: pequeño delay
                else -> index * 1000L  // Resto: más delay
            }

            val job = scope.launch {
                try {
                    delay(delay)

                    // Si ya está en cache y no expiró, saltar
                    val cached = cache[song.id]
                    if (cached != null && !cached.isExpired()) {
                        Log.d(TAG, "⏭️ Ya cacheada: ${song.title}")
                        return@launch
                    }

                    Log.d(TAG, "📦 Pre-cargando [P$priority]: ${song.title}")
                    val url = streamingRepository.getStreamUrl(song)

                    if (url != null) {
                        cache[song.id] = CachedStreamUrl(url)
                        Log.d(TAG, "✅ Pre-cargada: ${song.title}")
                    } else {
                        Log.w(TAG, "⚠️ Fallo pre-carga: ${song.title}")
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "🚫 Pre-carga cancelada: ${song.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error pre-cargando ${song.title}: ${e.message}")
                }
            }

            activeJobs[song.id] = job
        }
    }

    /**
     * Limpia cache expirado y cancela jobs
     */
    fun cleanup() {
        // Cancelar todos los jobs activos
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()

        // Remover URLs expiradas
        cache.entries.removeIf { it.value.isExpired() }

        Log.d(TAG, "🧹 Cache limpiado. Items restantes: ${cache.size}")
    }

    /**
     * Limpia completamente el cache
     */
    fun clear() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        cache.clear()
        Log.d(TAG, "🗑️ Cache completamente limpiado")
    }

    fun getCacheSize(): Int = cache.size
}