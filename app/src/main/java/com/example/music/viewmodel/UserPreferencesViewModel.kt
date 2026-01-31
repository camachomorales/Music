package com.example.music.viewmodel

import androidx.lifecycle.ViewModel
import com.example.music.data.model.*
import com.example.music.data.model.AccountType
import com.example.music.data.model.AudioQuality
import com.example.music.data.model.EqualizerPreset
import com.example.music.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesViewModel : ViewModel() {
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

    fun toggleDarkTheme() {
        _userPreferences.value = _userPreferences.value.copy(
            isDarkTheme = !_userPreferences.value.isDarkTheme
        )
    }

    fun toggleDownloadOnlyOnWifi() {
        _userPreferences.value = _userPreferences.value.copy(
            downloadOnlyOnWifi = !_userPreferences.value.downloadOnlyOnWifi
        )
    }

    fun toggleNotifications() {
        _userPreferences.value = _userPreferences.value.copy(
            showNotifications = !_userPreferences.value.showNotifications
        )
    }

    fun setAccountType(type: AccountType) {
        _accountType.value = type

        // Si es admin, actualiza el nombre
        if (type == AccountType.ADMIN) {
            _userPreferences.value = _userPreferences.value.copy(
                userName = "Developer",
                developerMode = true,
                allowExperimentalFeatures = true
            )
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        _userPreferences.value = _userPreferences.value.copy(
            audioQuality = quality
        )
    }

    fun setStreamingQuality(quality: AudioQuality) {
        _userPreferences.value = _userPreferences.value.copy(
            streamingQuality = quality
        )
    }

    fun setDownloadQuality(quality: AudioQuality) {
        _userPreferences.value = _userPreferences.value.copy(
            downloadQuality = quality
        )
    }

    fun toggleGaplessPlayback() {
        _userPreferences.value = _userPreferences.value.copy(
            gaplessPlayback = !_userPreferences.value.gaplessPlayback
        )
    }

    fun toggleNormalizeVolume() {
        _userPreferences.value = _userPreferences.value.copy(
            normalizeVolume = !_userPreferences.value.normalizeVolume
        )
    }

    fun setCrossfadeDuration(seconds: Int) {
        _userPreferences.value = _userPreferences.value.copy(
            crossfadeDuration = seconds.coerceIn(0, 12)
        )
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _userPreferences.value = _userPreferences.value.copy(
            equalizerPreset = preset
        )
    }

    fun loginAsAdmin() {
        setAccountType(AccountType.ADMIN)
    }
}