package com.example.music.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.model.*
import com.example.music.data.repository.AuthRepository
import com.example.music.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserPreferencesViewModel(context: Context) : ViewModel() {

    private val authRepository = AuthRepository(context)

    private val _userPreferences = MutableStateFlow(
        UserPreferences(
            userName = "Guest",
            isDarkTheme = true,
            audioQuality = AudioQuality.HIGH,
            streamingQuality = AudioQuality.NORMAL,
            downloadQuality = AudioQuality.HIGH,
            gaplessPlayback = true,
            normalizeVolume = false,
            showNotifications = true,
            downloadOnlyOnWifi = true,
            developerMode = false,
            allowExperimentalFeatures = false,
            maxCacheSize = 500,
            crossfadeDuration = 0,
            equalizerPreset = EqualizerPreset.FLAT
        )
    )
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private val _accountType = MutableStateFlow(AccountType.GUEST)
    val accountType: StateFlow<AccountType> = _accountType.asStateFlow()

    // === ACCOUNT ===
    fun setAccountType(type: AccountType) {
        _accountType.value = type

        if (type == AccountType.ADMIN) {
            _userPreferences.value = _userPreferences.value.copy(
                userName = "Developer",
                developerMode = true,
                allowExperimentalFeatures = true
            )
        }
    }

    fun loginAsAdmin() {
        setAccountType(AccountType.ADMIN)
    }

    // === APPEARANCE ===
    fun toggleDarkTheme() {
        _userPreferences.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun setGridColumns(columns: Int) {
        _userPreferences.update { it.copy(gridColumns = columns) }
    }

    fun setThemeColor(color: String) {
        _userPreferences.update { it.copy(themeColor = color) }
    }

    fun toggleDynamicColors() {
        _userPreferences.update { it.copy(useDynamicColors = !it.useDynamicColors) }
    }

    // === PLAYBACK ===
    fun setAudioQuality(quality: AudioQuality) {
        _userPreferences.update { it.copy(audioQuality = quality) }
    }

    fun setStreamingQuality(quality: AudioQuality) {
        _userPreferences.update { it.copy(streamingQuality = quality) }
    }

    fun setDownloadQuality(quality: AudioQuality) {
        _userPreferences.update { it.copy(downloadQuality = quality) }
    }

    fun toggleGaplessPlayback() {
        _userPreferences.update { it.copy(gaplessPlayback = !it.gaplessPlayback) }
    }

    fun toggleNormalizeVolume() {
        _userPreferences.update { it.copy(normalizeVolume = !it.normalizeVolume) }
    }

    fun setCrossfadeDuration(seconds: Int) {
        _userPreferences.update { it.copy(crossfadeDuration = seconds.coerceIn(0, 12)) }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _userPreferences.update { it.copy(equalizerPreset = preset) }
    }

    // === STORAGE ===
    fun setMaxCacheSize(size: Int) {
        _userPreferences.update { it.copy(maxCacheSize = size) }
    }

    fun toggleAutoDownloadFavorites() {
        _userPreferences.update { it.copy(autoDownloadFavorites = !it.autoDownloadFavorites) }
    }

    fun toggleDownloadOnlyOnWifi() {
        _userPreferences.update { it.copy(downloadOnlyOnWifi = !it.downloadOnlyOnWifi) }
    }

    // === NOTIFICATIONS ===
    fun toggleNotifications() {
        _userPreferences.update { it.copy(showNotifications = !it.showNotifications) }
    }

    fun toggleLockScreenControls() {
        _userPreferences.update { it.copy(showLockScreenControls = !it.showLockScreenControls) }
    }

    fun toggleShowAlbumArt() {
        _userPreferences.update { it.copy(showAlbumArt = !it.showAlbumArt) }
    }

    // === PRIVACY ===
    fun toggleListenHistory() {
        _userPreferences.update { it.copy(saveListenHistory = !it.saveListenHistory) }
    }

    fun toggleSearchHistory() {
        _userPreferences.update { it.copy(saveSearchHistory = !it.saveSearchHistory) }
    }

    fun toggleAnonymousUsage() {
        _userPreferences.update { it.copy(sendAnonymousStats = !it.sendAnonymousStats) }
    }

    fun updateUserInfo(email: String?, name: String) {
        _userPreferences.update {
            it.copy(
                userEmail = email,
                userName = name
            )
        }
    }

    fun syncData() {
        _userPreferences.update {
            it.copy(lastSyncTimestamp = System.currentTimeMillis())
        }
    }

    // === AUTHENTICATION ===

    /**
     * Login con email y password
     */
    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    _userPreferences.value = result.user
                    _accountType.value = if (result.user.isAdmin) AccountType.ADMIN else AccountType.LOCAL
                    onResult(true, "Logged in successfully!")
                }
                is AuthResult.Error -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    /**
     * Registro de nueva cuenta - FUNCIÓN ÚNICA
     */
    fun register(email: String, password: String, name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.register(email, password, name)) {
                is AuthResult.Success -> {
                    _userPreferences.update {
                        it.copy(
                            userId = result.user.userId,
                            userName = name,
                            userEmail = email,
                            isLoggedIn = true,
                            accountCreatedAt = System.currentTimeMillis()
                        )
                    }
                    _accountType.value = AccountType.LOCAL
                    onResult(true, "Account created successfully!")
                }
                is AuthResult.Error -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    /**
     * Logout - volver a modo guest
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _userPreferences.value = UserPreferences()
            _accountType.value = AccountType.GUEST
        }
    }
}