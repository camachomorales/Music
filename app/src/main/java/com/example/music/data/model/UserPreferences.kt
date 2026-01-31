package com.example.music.data.model

import kotlinx.serialization.Serializable

/**
 * Modelo de datos para las preferencias del usuario
 * Se guarda localmente con DataStore
 * Se sincroniza con Firebase si el usuario inicia sesión
 */
@Serializable
data class UserPreferences(
    // === ACCOUNT ===
    val userId: String? = null,  // null = guest, UUID = usuario local, email@domain = Firebase
    val userName: String = "Guest",
    val userEmail: String? = null,
    val isLoggedIn: Boolean = false,
    val isAdmin: Boolean = false, // ✅ Usuario desarrollador con permisos especiales
    val profileImageUrl: String? = null,
    val accountCreatedAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0,

    // === STATISTICS ===
    val totalSongsPlayed: Long = 0,
    val totalPlaybackTime: Long = 0, // milisegundos
    val favoriteGenre: String? = null,
    val mostPlayedSong: String? = null,
    val songsDownloaded: Int = 0,
    val playlistsCreated: Int = 0,

    // === APPEARANCE ===
    val isDarkTheme: Boolean = true,
    val accentColor: AccentColor = AccentColor.CYAN,
    val fontSize: FontSize = FontSize.MEDIUM,
    val enableAnimations: Boolean = true,

    // === PLAYBACK ===
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val streamingQuality: AudioQuality = AudioQuality.NORMAL, // Calidad para streaming online
    val downloadQuality: AudioQuality = AudioQuality.HIGH, // Calidad para descargas
    val crossfadeDuration: Int = 0, // 0-12 segundos
    val gaplessPlayback: Boolean = true,
    val normalizeVolume: Boolean = false,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,

    // === DOWNLOADS ===
    val downloadOnlyOnWifi: Boolean = true,
    val autoDownloadFavorites: Boolean = false,
    val maxCacheSize: Int = 500, // MB
    val autoDeleteCache: Boolean = true, // Eliminar cache antiguo automáticamente
    val cacheLocation: String = "internal", // "internal" o "external"

    // === NOTIFICATIONS ===
    val showNotifications: Boolean = true,
    val showPlaybackControls: Boolean = true,
    val showAlbumArt: Boolean = true,

    // === ADVANCED (Solo Admin) ===
    val developerMode: Boolean = false,
    val showDebugInfo: Boolean = false,
    val allowExperimentalFeatures: Boolean = false
)

enum class AccentColor(val hex: String, val displayName: String) {
    CYAN("#00D9FF", "Cyan"),
    PURPLE("#9D4EDD", "Purple"),
    GREEN("#06FFA5", "Green"),
    PINK("#FF006E", "Pink"),
    ORANGE("#FF9E00", "Orange"),
    RED("#EF233C", "Red")
}

enum class FontSize(val scale: Float, val displayName: String) {
    SMALL(0.9f, "Small"),
    MEDIUM(1.0f, "Medium"),
    LARGE(1.1f, "Large"),
    EXTRA_LARGE(1.2f, "Extra Large")
}

enum class AudioQuality(val bitrate: Int, val displayName: String) {
    LOW(96, "Low (96kbps) - Saves data"),
    NORMAL(128, "Normal (128kbps)"),
    HIGH(256, "High (256kbps)"),
    EXTREME(320, "Extreme (320kbps) - Best quality")
}

enum class EqualizerPreset(val displayName: String) {
    FLAT("Flat"),
    ACOUSTIC("Acoustic"),
    BASS_BOOSTER("Bass Booster"),
    BASS_REDUCER("Bass Reducer"),
    CLASSICAL("Classical"),
    DANCE("Dance"),
    DEEP("Deep"),
    ELECTRONIC("Electronic"),
    HIP_HOP("Hip Hop"),
    JAZZ("Jazz"),
    LATIN("Latin"),
    LOUDNESS("Loudness"),
    LOUNGE("Lounge"),
    PIANO("Piano"),
    POP("Pop"),
    R_AND_B("R&B"),
    ROCK("Rock"),
    SMALL_SPEAKERS("Small Speakers"),
    SPOKEN_WORD("Spoken Word"),
    TREBLE_BOOSTER("Treble Booster"),
    TREBLE_REDUCER("Treble Reducer"),
    VOCAL_BOOSTER("Vocal Booster")
}

/**
 * Tipo de cuenta de usuario
 */
enum class AccountType {
    GUEST,      // Sin login - datos se pierden al desinstalar
    LOCAL,      // Con email/password - sincroniza con Firebase
    ADMIN       // Desarrollador - acceso a features experimentales
}

/**
 * Extensión para obtener el tipo de cuenta
 */
fun UserPreferences.getAccountType(): AccountType {
    return when {
        isAdmin -> AccountType.ADMIN
        isLoggedIn -> AccountType.LOCAL
        else -> AccountType.GUEST
    }
}

/**
 * Credenciales de Admin (para desarrollo)
 * ⚠️ En producción, esto debe estar en un servidor seguro
 */
object AdminCredentials {
    const val ADMIN_EMAIL = "admin@musicapp.dev"
    const val ADMIN_PASSWORD = "MusicApp2024!" // ⚠️ Cambiar en producción
    const val ADMIN_USERNAME = "Developer"
}