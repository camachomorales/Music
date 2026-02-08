package com.example.music.ui.screens.SettingsScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.music.data.model.AudioQuality
import com.example.music.data.model.EqualizerPreset
import com.example.music.data.model.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    userPreferences: UserPreferences,
    onAudioQualityChange: (AudioQuality) -> Unit,
    onStreamingQualityChange: (AudioQuality) -> Unit,
    onDownloadQualityChange: (AudioQuality) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onToggleGapless: () -> Unit,
    onToggleNormalize: () -> Unit,
    onEqualizerChange: (EqualizerPreset) -> Unit,
    onBackClick: () -> Unit
) {
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showStreamingQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Audio Quality
            ListItem(
                headlineContent = { Text("Audio Quality") },
                supportingContent = { Text(userPreferences.audioQuality.displayName) },
                modifier = Modifier.clickable { showAudioQualityDialog = true }
            )

            Divider()

            // Streaming Quality
            ListItem(
                headlineContent = { Text("Streaming Quality") },
                supportingContent = { Text(userPreferences.streamingQuality.displayName) },
                modifier = Modifier.clickable { showStreamingQualityDialog = true }
            )

            Divider()

            // Download Quality
            ListItem(
                headlineContent = { Text("Download Quality") },
                supportingContent = { Text(userPreferences.downloadQuality.displayName) },
                modifier = Modifier.clickable { showDownloadQualityDialog = true }
            )

            Divider()

            // Crossfade
            ListItem(
                headlineContent = { Text("Crossfade") },
                supportingContent = { Text("${userPreferences.crossfadeDuration}s") },
                trailingContent = {
                    Slider(
                        value = userPreferences.crossfadeDuration.toFloat(),
                        onValueChange = { onCrossfadeChange(it.toInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        modifier = Modifier.width(150.dp)
                    )
                }
            )

            Divider()

            // Gapless Playback
            ListItem(
                headlineContent = { Text("Gapless Playback") },
                supportingContent = { Text("Seamless transitions between tracks") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.gaplessPlayback,
                        onCheckedChange = { onToggleGapless() }
                    )
                }
            )

            Divider()

            // Normalize Volume
            ListItem(
                headlineContent = { Text("Normalize Volume") },
                supportingContent = { Text("Equalize volume across all tracks") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.normalizeVolume,
                        onCheckedChange = { onToggleNormalize() }
                    )
                }
            )

            Divider()

            // Equalizer
            ListItem(
                headlineContent = { Text("Equalizer") },
                supportingContent = { Text(userPreferences.equalizerPreset.displayName) },
                modifier = Modifier.clickable { showEqualizerDialog = true }
            )
        }

        // Dialogs
        if (showAudioQualityDialog) {
            QualityDialog(
                title = "Audio Quality",
                selectedQuality = userPreferences.audioQuality,
                onQualitySelected = {
                    onAudioQualityChange(it)
                    showAudioQualityDialog = false
                },
                onDismiss = { showAudioQualityDialog = false }
            )
        }

        if (showStreamingQualityDialog) {
            QualityDialog(
                title = "Streaming Quality",
                selectedQuality = userPreferences.audioQuality, // Note: usando audioQuality como proxy
                onQualitySelected = {
                    onStreamingQualityChange(it)
                    showStreamingQualityDialog = false
                },
                onDismiss = { showStreamingQualityDialog = false }
            )
        }

        if (showDownloadQualityDialog) {
            QualityDialog(
                title = "Download Quality",
                selectedQuality = userPreferences.audioQuality, // Note: usando audioQuality como proxy
                onQualitySelected = {
                    onDownloadQualityChange(it)
                    showDownloadQualityDialog = false
                },
                onDismiss = { showDownloadQualityDialog = false }
            )
        }

        if (showEqualizerDialog) {
            EqualizerDialog(
                selectedPreset = userPreferences.equalizerPreset,
                onPresetSelected = {
                    onEqualizerChange(it)
                    showEqualizerDialog = false
                },
                onDismiss = { showEqualizerDialog = false }
            )
        }
    }
}

@Composable
fun QualityDialog(
    title: String,
    selectedQuality: AudioQuality,
    onQualitySelected: (AudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                AudioQuality.values().forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(quality) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = quality == selectedQuality,
                            onClick = { onQualitySelected(quality) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(quality.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EqualizerDialog(
    selectedPreset: EqualizerPreset,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Equalizer Preset") },
        text = {
            Column {
                EqualizerPreset.values().forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPresetSelected(preset) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preset == selectedPreset,
                            onClick = { onPresetSelected(preset) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(preset.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}