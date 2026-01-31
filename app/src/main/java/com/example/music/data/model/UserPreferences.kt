package com.example.music.data.model

data class UserPreferences(
    // Account
    val userId: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val isLoggedIn: Boolean = false,
    val isAdmin: Boolean = false,
    val accountType: AccountType = AccountType.FREE,

    // Appearance
    val isDarkTheme: Boolean = false,
    val useDynamicColors: Boolean = true,
    val themeColor: String = "blue",
    val gridColumns: Int = 2,
    val showAlbumArtInApp: Boolean = true,
    val enableAnimations: Boolean = true,

    // Audio
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val streamingQuality: AudioQuality = AudioQuality.NORMAL,
    val downloadQuality: AudioQuality = AudioQuality.HIGH,
    val gaplessPlayback: Boolean = true,
    val normalizeVolume: Boolean = false,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val crossfadeDuration: Int = 0,

    // Storage
    val maxCacheSize: Int = 500,
    val autoDownloadSongs: Boolean = false,
    val downloadOnlyOnWifi: Boolean = true,
    val autoDownloadFavorites: Boolean = false,
    val autoDeleteCache: Boolean = true,

    // Notifications
    val showNotifications: Boolean = true,
    val showLockScreenControls: Boolean = true,
    val showAlbumArtInNotification: Boolean = true,
    val useMediaButtons: Boolean = true,
    val showPlaybackControls: Boolean = true,
    val showAlbumArt: Boolean = true,

    // Privacy
    val saveListenHistory: Boolean = true,
    val saveSearchHistory: Boolean = true,
    val sendAnonymousUsageStats: Boolean = false,
    val sendAnonymousStats: Boolean = false,

    // Stats
    val totalSongsPlayed: Int = 0,
    val totalPlaybackTime: Long = 0L,
    val playlistsCreated: Int = 0,
    val accountCreatedAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long? = null,

    // Advanced
    val developerMode: Boolean = false,
    val showDebugInfo: Boolean = false,
    val allowExperimentalFeatures: Boolean = false
)

enum class AccountType {
    FREE,
    PREMIUM,
    ADMIN,
    LOCAL,  // Agregar
    GUEST   // Agregar
}

enum class AudioQuality(val displayName: String) {
    LOW("Low (96 kbps)"),
    NORMAL("Normal (128 kbps)"),
    HIGH("High (256 kbps)"),
    VERY_HIGH("Very High (320 kbps)")
}

enum class EqualizerPreset(val displayName: String) {
    FLAT("Flat"),
    ACOUSTIC("Acoustic"),
    BASS_BOOST("Bass Boost"),
    CLASSICAL("Classical"),
    DANCE("Dance"),
    DEEP("Deep"),
    ELECTRONIC("Electronic"),
    FOLK("Folk"),
    HEAVY_METAL("Heavy Metal"),
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
    TREBLE_BOOST("Treble Boost"),
    VOCAL_BOOSTER("Vocal Booster")
}


enum class AccentColor {
    BLUE, RED, GREEN, PURPLE, ORANGE, PINK
}

enum class FontSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}