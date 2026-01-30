package com.example.music.data.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class SongCacheManager(private val context: Context) {

    private val TAG = "SongCacheManager"
    private val cacheDir = File(context.cacheDir, "song_cache")
    private val maxCacheSize = 500L * 1024 * 1024 // 500MB máximo

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            Log.d(TAG, "📁 Cache directory creado: ${cacheDir.absolutePath}")
        }
    }

    /**
     * Verificar si una canción está en cache
     */
    fun isCached(songId: String): Boolean {
        val cacheFile = getCacheFile(songId)
        val exists = cacheFile.exists() && cacheFile.length() > 0
        if (exists) {
            Log.d(TAG, "✅ Canción en cache: $songId (${cacheFile.length() / 1024}KB)")
        }
        return exists
    }

    /**
     * Obtener ruta del archivo en cache
     */
    fun getCachedPath(songId: String): String? {
        val cacheFile = getCacheFile(songId)
        return if (cacheFile.exists()) {
            Log.d(TAG, "📂 Obteniendo cache: $songId")
            cacheFile.absolutePath
        } else {
            null
        }
    }

    /**
     * Descargar canción a cache en background
     */
    suspend fun downloadToCache(url: String, songId: String, onProgress: (Int) -> Unit = {}): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile(songId)

                // Si ya existe, no descargar de nuevo
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    Log.d(TAG, "⚡ Ya existe en cache: $songId")
                    return@withContext true
                }

                Log.d(TAG, "📥 Descargando: $songId")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                val fileSize = connection.contentLength
                var downloadedSize = 0L

                connection.inputStream.use { input ->
                    cacheFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead

                            // Reportar progreso cada 100KB
                            if (fileSize > 0 && downloadedSize % 102400 == 0L) {
                                val progress = (downloadedSize * 100 / fileSize).toInt()
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                }

                Log.d(TAG, "✅ Descargado: $songId (${cacheFile.length() / 1024}KB)")

                // Limpiar cache si excede el límite
                cleanupIfNeeded()

                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error descargando $songId: ${e.message}", e)

                // Eliminar archivo parcial
                getCacheFile(songId).delete()
                false
            }
        }
    }

    /**
     * Pre-cargar solo los primeros segundos (para inicio rápido)
     */
    suspend fun preloadChunk(url: String, songId: String, chunkSizeKB: Int = 512): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val chunkFile = getChunkFile(songId)

                if (chunkFile.exists()) {
                    Log.d(TAG, "⚡ Chunk ya existe: $songId")
                    return@withContext true
                }

                Log.d(TAG, "📥 Pre-cargando chunk: $songId (${chunkSizeKB}KB)")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.setRequestProperty("Range", "bytes=0-${chunkSizeKB * 1024}")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                connection.inputStream.use { input ->
                    chunkFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "✅ Chunk pre-cargado: $songId (${chunkFile.length() / 1024}KB)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error pre-cargando chunk: ${e.message}")
                false
            }
        }
    }

    /**
     * Limpiar cache antiguo si excede el límite
     */
    private fun cleanupIfNeeded() {
        try {
            val files = cacheDir.listFiles() ?: return
            val totalSize = files.sumOf { it.length() }

            if (totalSize > maxCacheSize) {
                Log.d(TAG, "🧹 Limpiando cache (${totalSize / 1024 / 1024}MB > ${maxCacheSize / 1024 / 1024}MB)")

                // Ordenar por última modificación (más antiguos primero)
                val sortedFiles = files.sortedBy { it.lastModified() }

                var currentSize = totalSize
                for (file in sortedFiles) {
                    if (currentSize <= maxCacheSize * 0.8) break // Dejar al 80%

                    val fileSize = file.length()
                    if (file.delete()) {
                        currentSize -= fileSize
                        Log.d(TAG, "🗑️ Eliminado: ${file.name} (${fileSize / 1024}KB)")
                    }
                }

                Log.d(TAG, "✅ Cache limpiado: ${currentSize / 1024 / 1024}MB")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando cache: ${e.message}")
        }
    }

    /**
     * Eliminar una canción específica del cache
     */
    fun deleteCached(songId: String) {
        val cacheFile = getCacheFile(songId)
        val chunkFile = getChunkFile(songId)

        if (cacheFile.delete()) {
            Log.d(TAG, "🗑️ Eliminado de cache: $songId")
        }
        chunkFile.delete()
    }

    /**
     * Limpiar todo el cache
     */
    fun clearAll() {
        try {
            val files = cacheDir.listFiles() ?: return
            var deleted = 0

            for (file in files) {
                if (file.delete()) deleted++
            }

            Log.d(TAG, "🧹 Cache limpiado completamente: $deleted archivos")
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando cache: ${e.message}")
        }
    }

    /**
     * Obtener tamaño total del cache
     */
    fun getCacheSize(): Long {
        return try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Obtener cantidad de canciones en cache
     */
    fun getCachedSongsCount(): Int {
        return try {
            cacheDir.listFiles()?.count { it.name.startsWith("song_") } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getCacheFile(songId: String): File {
        return File(cacheDir, "song_$songId.mp3")
    }

    private fun getChunkFile(songId: String): File {
        return File(cacheDir, "chunk_$songId.mp3")
    }
}