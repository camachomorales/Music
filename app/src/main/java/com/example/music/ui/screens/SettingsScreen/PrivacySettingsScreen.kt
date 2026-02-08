package com.example.music.ui.screens.SettingsScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.music.data.model.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    userPreferences: UserPreferences,
    onToggleNotifications: () -> Unit,
    onToggleLockScreenControls: () -> Unit,
    onToggleShowAlbumArt: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings") },
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
                .padding(16.dp)
        ) {
            // Show Notifications
            ListItem(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text("Show playback notifications") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.showNotifications,
                        onCheckedChange = { onToggleNotifications() }
                    )
                }
            )

            Divider()

            // Lock Screen Controls
            ListItem(
                headlineContent = { Text("Lock Screen Controls") },
                supportingContent = { Text("Show playback controls on lock screen") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.showPlaybackControls,
                        onCheckedChange = { onToggleLockScreenControls() }
                    )
                }
            )

            Divider()

            // Show Album Art
            ListItem(
                headlineContent = { Text("Album Art") },
                supportingContent = { Text("Display album artwork") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.showAlbumArt,
                        onCheckedChange = { onToggleShowAlbumArt() }
                    )
                }
            )
        }
    }
}