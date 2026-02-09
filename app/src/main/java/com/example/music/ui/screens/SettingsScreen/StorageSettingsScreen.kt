package com.example.music.ui.screens.SettingsScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.music.data.model.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    userPreferences: UserPreferences,
    cacheSize: Long,
    downloadedSize: Long,
    onClearCache: () -> Unit,
    onClearDownloads: () -> Unit,
    onMaxCacheSizeChange: (Long) -> Unit,
    onToggleAutoDownload: () -> Unit,
    onToggleDownloadOverCellular: () -> Unit,
    onBackClick: () -> Unit
) {
    var showCacheSizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Settings") },
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
                .padding(16.dp)
        ) {
            // Storage Overview Section
            Text(
                text = "Storage Overview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cache Size:")
                        Text("${cacheSize / 1024 / 1024} MB")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Downloads:")
                        Text("${downloadedSize / 1024 / 1024} MB")
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Cache Management
            Text(
                text = "Cache Management",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Max Cache Size
            ListItem(
                headlineContent = { Text("Max Cache Size") },
                supportingContent = { Text("${userPreferences.maxCacheSize} MB") },
                modifier = Modifier.clickable { showCacheSizeDialog = true }
            )

            Divider()

            // Auto Delete Cache
            ListItem(
                headlineContent = { Text("Auto Delete Cache") },
                supportingContent = { Text("Automatically clear old cache") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.autoDeleteCache,
                        onCheckedChange = { /* Toggle auto delete */ }
                    )
                }
            )

            Divider()

            // Clear Cache Button
            Button(
                onClick = onClearCache,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear Cache")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Download Settings
            Text(
                text = "Download Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Download Only on WiFi
            ListItem(
                headlineContent = { Text("Download on WiFi Only") },
                supportingContent = { Text("Only download when connected to WiFi") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.downloadOnlyOnWifi,
                        onCheckedChange = { onToggleDownloadOverCellular() }
                    )
                }
            )

            Divider()

            // Auto Download Favorites
            ListItem(
                headlineContent = { Text("Auto Download Favorites") },
                supportingContent = { Text("Automatically download favorite songs") },
                trailingContent = {
                    Switch(
                        checked = userPreferences.autoDownloadFavorites,
                        onCheckedChange = { onToggleAutoDownload() }
                    )
                }
            )

            Divider()

            // Clear Downloads Button
            Button(
                onClick = onClearDownloads,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All Downloads")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Storage Location
            Text(
                text = "Storage Location",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Cache Location") },
                supportingContent = { Text(userPreferences.cacheLocation) }
            )
        }

        // Cache Size Dialog
        if (showCacheSizeDialog) {
            AlertDialog(
                onDismissRequest = { showCacheSizeDialog = false },
                title = { Text("Max Cache Size") },
                text = {
                    Column {
                        Text("Select maximum cache size:")
                        Spacer(modifier = Modifier.height(16.dp))
                        val sizes = listOf(100L, 250L, 500L, 1000L, 2000L, 5000L)
                        sizes.forEach { size ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMaxCacheSizeChange(size)
                                        showCacheSizeDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = userPreferences.maxCacheSize == size,
                                    onClick = {
                                        onMaxCacheSizeChange(size)
                                        showCacheSizeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$size MB")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCacheSizeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}