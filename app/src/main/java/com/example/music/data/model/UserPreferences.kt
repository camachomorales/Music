package com.example.music.data.model

/**
 * Modelo de preferencias del usuario
 * Contiene todas las configuraciones y datos de la cuenta
 */
data class UserPreferences(
    // ==================== ACCOUNT ====================
    val userId: String? = null,
    val userName: String = "Guest",
    val userEmail: String? = null,
    val isLoggedIn: Boolean = false,
    val isAdmin: Boolean = false,
    val profileImageUrl: String? = null,
    val accountCreatedAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0,

    // ==================== STATISTICS ====================
    val totalSongsPlayed: Long = 0,
    val totalPlaybackTime: Long = 0,
    val favoriteGenre: String? = null,
    val mostPlayedSong: String? = null,
    val songsDownloaded: Int = 0,
    val playlistsCreated: Int = 0,

    // ==================== APPEARANCE ====================
    val isDarkTheme: Boolean = true,
    val accentColor: AccentColor = AccentColor.CYAN,
    val fontSize: FontSize = FontSize.MEDIUM,
    val enableAnimations: Boolean = true,

    // ==================== PLAYBACK ====================
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val streamingQuality: AudioQuality = AudioQuality.NORMAL,
    val downloadQuality: AudioQuality = AudioQuality.HIGH,
    val crossfadeDuration: Int = 0,
    val gaplessPlayback: Boolean = true,
    val normalizeVolume: Boolean = false,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,

    // ==================== DOWNLOADS ====================
    val downloadOnlyOnWifi: Boolean = true,
    val autoDownloadFavorites: Boolean = false,
    val maxCacheSize: Long = 500,
    val autoDeleteCache: Boolean = true,
    val cacheLocation: String = "internal",

    // ==================== NOTIFICATIONS ====================
    val showNotifications: Boolean = true,
    val showPlaybackControls: Boolean = true,
    val showAlbumArt: Boolean = true,

    // ==================== ADVANCED ====================
    val developerMode: Boolean = false,
    val showDebugInfo: Boolean = false,
    val allowExperimentalFeatures: Boolean = false,


) {
    /**
     * Obtener tipo de cuenta del usuario
     */
    fun getAccountType(): AccountType {
        return when {
            isAdmin -> AccountType.ADMIN
            isLoggedIn -> AccountType.LOCAL
            else -> AccountType.GUEST
        }
    }

    /**
     * Verificar si es cuenta admin
     */
    fun isAdminAccount(): Boolean {
        return userEmail == AdminCredentials.ADMIN_EMAIL && isAdmin
    }
}

// ==================== ENUMS ====================

enum class AccentColor(val displayName: String) {
    RED("Red"),
    PINK("Pink"),
    PURPLE("Purple"),
    BLUE("Blue"),
    CYAN("Cyan"),
    TEAL("Teal"),
    GREEN("Green"),
    YELLOW("Yellow"),
    ORANGE("Orange")
}

enum class FontSize(val displayName: String, val scale: Float) {
    SMALL("Small", 0.9f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.1f),
    EXTRA_LARGE("Extra Large", 1.2f)
}

enum class AudioQuality(val displayName: String, val bitrate: Int) {
    LOW("Low (96 kbps)", 96),
    NORMAL("Normal (128 kbps)", 128),
    HIGH("High (256 kbps)", 256),
    LOSSLESS("Lossless (FLAC)", 1411)
}

enum class StreamingQuality(val displayName: String, val bitrate: Int) {
    LOW("Low (96 kbps)", 96),
    NORMAL("Normal (128 kbps)", 128),
    HIGH("High (256 kbps)", 256),
    VERY_HIGH("Very High (320 kbps)", 320)
}

enum class EqualizerPreset(val displayName: String) {
    FLAT("Flat"),
    ACOUSTIC("Acoustic"),
    BASS_BOOST("Bass Boost"),
    BASS_REDUCER("Bass Reducer"),
    CLASSICAL("Classical"),
    DANCE("Dance"),
    DEEP("Deep"),
    ELECTRONIC("Electronic"),
    HIP_HOP("Hip-Hop"),
    JAZZ("Jazz"),
    LATIN("Latin"),
    LOUDNESS("Loudness"),
    LOUNGE("Lounge"),
    PIANO("Piano"),
    POP("Pop"),
    RNB("R&B"),
    ROCK("Rock"),
    SMALL_SPEAKERS("Small Speakers"),
    SPOKEN_WORD("Spoken Word"),
    TREBLE_BOOST("Treble Boost"),
    TREBLE_REDUCER("Treble Reducer"),
    VOCAL_BOOST("Vocal Boost")
}
