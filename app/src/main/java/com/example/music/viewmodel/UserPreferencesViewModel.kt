package com.example.music.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.music.data.model.*
import com.example.music.data.repository.AuthRepository
import com.example.music.data.repository.AuthResult
import com.example.music.data.repository.SettingsRepository
import com.example.music.data.repository.SyncResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserPreferencesViewModel(context: Context) : ViewModel() {

    private val settingsRepository = SettingsRepository(context)
    private val authRepository = AuthRepository(context)

    // User preferences flow
    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    // Account type derived from preferences
    val accountType: StateFlow<AccountType> = userPreferences
        .map { it.getAccountType() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountType.GUEST
        )

    // Auth state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.initialize()
        }
    }

    // ==================== AUTH FUNCTIONS ====================

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    settingsRepository.savePreferences(result.user)
                    onResult(true, "Login successful")
                }
                is AuthResult.Error -> {
                    onResult(false, result.message)
                }
            }
            _isLoading.value = false
        }
    }

    fun register(email: String, password: String, name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepository.register(email, password, name)) {
                is AuthResult.Success -> {
                    settingsRepository.savePreferences(result.user)
                    onResult(true, "Registration successful")
                }
                is AuthResult.Error -> {
                    onResult(false, result.message)
                }
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun syncData() {
        viewModelScope.launch {
            when (authRepository.syncData()) {
                is SyncResult.Success -> Log.d("UserPreferencesVM", "Sync successful")
                is SyncResult.NotLoggedIn -> Log.w("UserPreferencesVM", "Not logged in")
                is SyncResult.Error -> Log.e("UserPreferencesVM", "Sync failed")
            }
        }
    }

    // ==================== APPEARANCE ====================

    fun toggleDarkTheme() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(isDarkTheme = !current.isDarkTheme)
            )
        }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(accentColor = color)
            )
        }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(fontSize = size)
            )
        }
    }

    fun toggleAnimations() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(enableAnimations = !current.enableAnimations)
            )
        }
    }

    // ==================== PLAYBACK ====================

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(audioQuality = quality)
            )
        }
    }

    fun setStreamingQuality(quality: AudioQuality) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(streamingQuality = quality)
            )
        }
    }

    fun setDownloadQuality(quality: AudioQuality) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(downloadQuality = quality)
            )
        }
    }

    fun setCrossfadeDuration(duration: Int) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(crossfadeDuration = duration)
            )
        }
    }

    fun toggleGaplessPlayback() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(gaplessPlayback = !current.gaplessPlayback)
            )
        }
    }

    fun toggleNormalizeVolume() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(normalizeVolume = !current.normalizeVolume)
            )
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(equalizerPreset = preset)
            )
        }
    }

    // ==================== DOWNLOADS ====================

    fun toggleDownloadOnlyOnWifi() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(downloadOnlyOnWifi = !current.downloadOnlyOnWifi)
            )
        }
    }

    fun toggleAutoDownloadFavorites() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(autoDownloadFavorites = !current.autoDownloadFavorites)
            )
        }
    }

    fun setMaxCacheSize(sizeMB: Long) {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(maxCacheSize = sizeMB)
            )
        }
    }

    fun toggleAutoDeleteCache() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(autoDeleteCache = !current.autoDeleteCache)
            )
        }
    }

    // ==================== NOTIFICATIONS ====================

    fun toggleNotifications() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(showNotifications = !current.showNotifications)
            )
        }
    }

    fun toggleLockScreenControls() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(showPlaybackControls = !current.showPlaybackControls)
            )
        }
    }

    fun toggleShowAlbumArt() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(showAlbumArt = !current.showAlbumArt)
            )
        }
    }

    // ==================== AUTH STATE MANAGEMENT ====================

    fun updateLoginState(isLoggedIn: Boolean, isAdmin: Boolean = false, email: String? = null, userName: String = "User") {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(
                    isLoggedIn = isLoggedIn,
                    isAdmin = isAdmin,
                    userEmail = email,
                    userName = userName
                )
            )
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            val current = userPreferences.value
            settingsRepository.savePreferences(
                current.copy(
                    isLoggedIn = false,
                    isAdmin = false,
                    userEmail = null,
                    userName = "Guest"
                )
            )
        }
    }
}

