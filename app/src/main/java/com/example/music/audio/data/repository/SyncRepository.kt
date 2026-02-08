package com.example.music.data.repository

import android.util.Log
// import com.example.music.data.local.dao.PlaylistDao  // COMENTADO
import com.example.music.data.local.dao.SongDao
// import com.example.music.data.local.entity.Playlist  // COMENTADO
// import com.example.music.data.local.entity.Song  // COMENTADO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    //private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) {
    private val TAG = "SyncRepository"

    /**
     * Verifica si el usuario tiene sesión iniciada
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Obtiene el ID del usuario actual
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Sincroniza todos los datos locales con Firebase
     */
    suspend fun syncToCloud() {
        val userId = getCurrentUserId() ?: run {
            Log.w(TAG, "No hay usuario logueado, no se puede sincronizar")
            return
        }

        try {
            // Sincronizar playlists - COMENTADO TEMPORALMENTE
            /*
            val playlists = playlistDao.getAllPlaylists().first()
            playlists.forEach { playlist ->
                syncPlaylistToCloud(userId, playlist)
            }
            */

            // Sincronizar favoritos - COMENTADO TEMPORALMENTE
            /*
            val favorites = songDao.getFavoriteSongs().first()
            syncFavoritesToCloud(userId, favorites)
            */

            Log.d(TAG, "Sincronización a la nube completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar a la nube", e)
            throw e
        }
    }

    /**
     * Descarga todos los datos desde Firebase a local
     */
    suspend fun syncFromCloud() {
        val userId = getCurrentUserId() ?: run {
            Log.w(TAG, "No hay usuario logueado, no se puede descargar")
            return
        }

        try {
            // Descargar playlists - COMENTADO TEMPORALMENTE
            /*
            val playlistsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("playlists")
                .get()
                .await()

            playlistsSnapshot.documents.forEach { doc ->
                val playlist = doc.toObject(Playlist::class.java)
                playlist?.let {
                    playlistDao.insertPlaylist(it)
                }
            }
            */

            // Descargar favoritos - COMENTADO TEMPORALMENTE
            /*
            val favoritesDoc = firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document("songs")
                .get()
                .await()

            val favoriteIds = favoritesDoc.get("songIds") as? List<String> ?: emptyList()
            favoriteIds.forEach { songId ->
                songDao.updateFavoriteStatus(songId, true)
            }
            */

            Log.d(TAG, "Sincronización desde la nube completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar desde la nube", e)
            throw e
        }
    }

    /**
     * Sincroniza una playlist específica a Firebase
     */
    /*
    suspend fun syncPlaylistToCloud(userId: String, playlist: Playlist) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlist.id.toString())
                .set(playlist, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar playlist ${playlist.name}", e)
        }
    }
    */

    /**
     * Elimina una playlist de Firebase
     */
    suspend fun deletePlaylistFromCloud(playlistId: Long) {
        val userId = getCurrentUserId() ?: return

        try {
            firestore.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar playlist de la nube", e)
        }
    }

    /**
     * Sincroniza favoritos a Firebase
     */
    /*
    suspend fun syncFavoritesToCloud(userId: String, favorites: List<Song>) {
        try {
            val favoriteIds = favorites.map { it.id }

            firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document("songs")
                .set(mapOf("songIds" to favoriteIds), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar favoritos", e)
        }
    }
    */

    /**
     * Actualiza el estado de favorito de una canción en Firebase
     */
    suspend fun updateFavoriteInCloud(songId: String, isFavorite: Boolean) {
        val userId = getCurrentUserId() ?: return

        try {
            val favoritesRef = firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document("songs")

            if (isFavorite) {
                // Agregar a favoritos
                favoritesRef.update("songIds", com.google.firebase.firestore.FieldValue.arrayUnion(songId))
                    .await()
            } else {
                // Remover de favoritos
                favoritesRef.update("songIds", com.google.firebase.firestore.FieldValue.arrayRemove(songId))
                    .await()
            }
        } catch (e: Exception) {
            // Si el documento no existe, crearlo
            if (isFavorite) {
                try {
                    firestore.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .document("songs")
                        .set(mapOf("songIds" to listOf(songId)))
                        .await()
                } catch (e2: Exception) {
                    Log.e(TAG, "Error al crear documento de favoritos", e2)
                }
            }
        }
    }

    /**
     * Limpia todos los datos locales (cuando se cierra sesión)
     */
    /*
    suspend fun clearLocalData() {
        try {
            playlistDao.deleteAllPlaylists()
            songDao.clearAllFavorites()
            Log.d(TAG, "Datos locales limpiados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar datos locales", e)
        }
    }
    */

    /**
     * Sincronización automática al modificar datos
     */
    /*
    suspend fun onPlaylistCreated(playlist: Playlist) {
        // Guardar localmente
        playlistDao.insertPlaylist(playlist)

        // Si hay usuario logueado, sincronizar a la nube
        getCurrentUserId()?.let { userId ->
            syncPlaylistToCloud(userId, playlist)
        }
    }

    suspend fun onPlaylistDeleted(playlistId: Long) {
        // Eliminar localmente
        playlistDao.deletePlaylist(playlistId)

        // Si hay usuario logueado, eliminar de la nube
        if (isUserLoggedIn()) {
            deletePlaylistFromCloud(playlistId)
        }
    }
    */

    suspend fun onFavoriteToggled(songId: String, isFavorite: Boolean) {
        // Actualizar localmente - COMENTADO TEMPORALMENTE
        // songDao.updateFavoriteStatus(songId, isFavorite)

        // Si hay usuario logueado, sincronizar a la nube
        if (isUserLoggedIn()) {
            updateFavoriteInCloud(songId, isFavorite)
        }
    }
}