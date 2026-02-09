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
fun NotificationsSettingsScreen(
    userPreferences: UserPreferences,
    onToggleNotifications: () -> Unit,
    onToggleLockScreen: () -> Unit,
    onToggleMediaButtons: () -> Unit,
    onToggleShowAlbumArt: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
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
                headlineContent = { Text("Show Notifications") },
                supportingContent = { Text("Display playback notifications") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.showNotifications,
                        onCheckedChange = { onToggleNotifications() }
                    )
                }
            )

            Divider()

            // Show Playback Controls
            ListItem(
                headlineContent = { Text("Playback Controls") },
                supportingContent = { Text("Show controls in notification") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.showPlaybackControls,
                        onCheckedChange = { onToggleLockScreen() }
                    )
                }
            )

            Divider()

            // Show Album Art
            ListItem(
                headlineContent = { Text("Album Art") },
                supportingContent = { Text("Show album art in notification") },
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