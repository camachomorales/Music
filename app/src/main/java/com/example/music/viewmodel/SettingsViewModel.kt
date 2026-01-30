package com.example.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.model.*
import com.example.music.data.repository.AuthRepository
import com.example.music.data.repository.AuthResult
import com.example.music.data.repository.SettingsRepository
import com.example.music.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para Settings y Account
 * Gestiona autenticación, preferencias y sincronización
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val authRepository = AuthRepository(application)

    // Preferencias del usuario
    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    // Estado de autenticación
    private val _currentUser = MutableStateFlow<UserPreferences?>(null)
    val currentUser: StateFlow<UserPreferences?> = _currentUser.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success messages
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadPreferences()
        initializeAuth()
    }

    // ==================== PREFERENCES ====================

    private fun loadPreferences() {
        viewModelScope.launch {
            settingsRepository.userPreferencesFlow.collect { prefs ->
                _userPreferences.value = prefs
            }
        }
    }

    fun savePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            settingsRepository.savePreferences(preferences)
        }
    }

    // ==================== APPEARANCE ====================

    fun toggleDarkTheme() {
        val current = _userPreferences.value
        savePreferences(current.copy(isDarkTheme = !current.isDarkTheme))
    }

    fun setAccentColor(color: AccentColor) {
        val current = _userPreferences.value
        savePreferences(current.copy(accentColor = color))
    }

    fun setFontSize(size: FontSize) {
        val current = _userPreferences.value
        savePreferences(current.copy(fontSize = size))
    }

    fun toggleAnimations() {
        val current = _userPreferences.value
        savePreferences(current.copy(enableAnimations = !current.enableAnimations))
    }

    // ==================== PLAYBACK ====================

    fun setAudioQuality(quality: AudioQuality) {
        val current = _userPreferences.value
        savePreferences(current.copy(audioQuality = quality))
    }

    fun setStreamingQuality(quality: AudioQuality) {
        val current = _userPreferences.value
        savePreferences(current.copy(streamingQuality = quality))
    }

    fun setDownloadQuality(quality: AudioQuality) {
        val current = _userPreferences.value
        savePreferences(current.copy(downloadQuality = quality))
    }

    fun setCrossfadeDuration(duration: Int) {
        val current = _userPreferences.value
        savePreferences(current.copy(crossfadeDuration = duration.coerceIn(0, 12)))
    }

    fun toggleGaplessPlayback() {
        val current = _userPreferences.value
        savePreferences(current.copy(gaplessPlayback = !current.gaplessPlayback))
    }

    fun toggleNormalizeVolume() {
        val current = _userPreferences.value
        savePreferences(current.copy(normalizeVolume = !current.normalizeVolume))
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        val current = _userPreferences.value
        savePreferences(current.copy(equalizerPreset = preset))
    }

    // ==================== DOWNLOADS ====================

    fun toggleDownloadOnlyOnWifi() {
        val current = _userPreferences.value
        savePreferences(current.copy(downloadOnlyOnWifi = !current.downloadOnlyOnWifi))
    }

    fun toggleAutoDownloadFavorites() {
        val current = _userPreferences.value
        savePreferences(current.copy(autoDownloadFavorites = !current.autoDownloadFavorites))
    }

    fun setMaxCacheSize(sizeMB: Int) {
        val current = _userPreferences.value
        savePreferences(current.copy(maxCacheSize = sizeMB))
    }

    fun toggleAutoDeleteCache() {
        val current = _userPreferences.value
        savePreferences(current.copy(autoDeleteCache = !current.autoDeleteCache))
    }

    fun setCacheLocation(location: String) {
        val current = _userPreferences.value
        savePreferences(current.copy(cacheLocation = location))
    }

    // ==================== NOTIFICATIONS ====================

    fun toggleNotifications() {
        val current = _userPreferences.value
        savePreferences(current.copy(showNotifications = !current.showNotifications))
    }

    fun togglePlaybackControls() {
        val current = _userPreferences.value
        savePreferences(current.copy(showPlaybackControls = !current.showPlaybackControls))
    }

    fun toggleAlbumArt() {
        val current = _userPreferences.value
        savePreferences(current.copy(showAlbumArt = !current.showAlbumArt))
    }

    // ==================== ADVANCED (Admin only) ====================

    fun toggleDeveloperMode() {
        if (!isAdmin()) return
        val current = _userPreferences.value
        savePreferences(current.copy(developerMode = !current.developerMode))
    }

    fun toggleDebugInfo() {
        if (!isAdmin()) return
        val current = _userPreferences.value
        savePreferences(current.copy(showDebugInfo = !current.showDebugInfo))
    }

    fun toggleExperimentalFeatures() {
        if (!isAdmin()) return
        val current = _userPreferences.value
        savePreferences(current.copy(allowExperimentalFeatures = !current.allowExperimentalFeatures))
    }

    // ==================== AUTHENTICATION ====================

    private fun initializeAuth() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.initialize()
            authRepository.currentUser.collect { user ->
                _currentUser.value = user
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    savePreferences(result.user)
                    _successMessage.value = if (result.user.isAdmin) {
                        "Welcome back, Developer! 🎯"
                    } else {
                        "Welcome back, ${result.user.userName}!"
                    }
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    fun register(email: String, password: String, userName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = authRepository.register(email, password, userName)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    savePreferences(result.user)
                    _successMessage.value = "Account created successfully!"
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // No limpiar preferencias - mantener configuraciones
            _successMessage.value = "Logged out. You're now in Guest mode."
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true

            when (val result = authRepository.syncData()) {
                is SyncResult.Success -> {
                    _successMessage.value = "Data synced successfully"
                }
                is SyncResult.NotLoggedIn -> {
                    _errorMessage.value = "Please log in to sync data"
                }
                is SyncResult.Error -> {
                    _errorMessage.value = "Sync failed: ${result.message}"
                }
            }

            _isSyncing.value = false
        }
    }

    // ==================== STATISTICS ====================

    fun incrementSongPlayed() {
        viewModelScope.launch {
            val current = _userPreferences.value
            settingsRepository.updateStatistics(
                songsPlayed = current.totalSongsPlayed + 1
            )
        }
    }

    fun addPlaybackTime(millis: Long) {
        viewModelScope.launch {
            val current = _userPreferences.value
            settingsRepository.updateStatistics(
                playbackTime = current.totalPlaybackTime + millis
            )
        }
    }

    // ==================== HELPERS ====================

    fun isLoggedIn(): Boolean = _currentUser.value?.isLoggedIn == true
    fun isAdmin(): Boolean = _currentUser.value?.isAdmin == true
    fun isGuest(): Boolean = !isLoggedIn()

    fun getAccountType(): AccountType {
        return _currentUser.value?.getAccountType() ?: AccountType.GUEST
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}