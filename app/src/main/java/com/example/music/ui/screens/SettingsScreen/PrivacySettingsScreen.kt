package com.example.music.ui.screens.SettingsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music.data.model.UserPreferences

@Composable
fun PrivacySettingsScreen(
    userPreferences: UserPreferences,
    onToggleListenHistory: () -> Unit,
    onToggleSearchHistory: () -> Unit,
    onToggleAnonymousUsage: () -> Unit,
    onClearListenHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onExportData: () -> Unit,
    onBackClick: () -> Unit
) {
    var showClearListenHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Privacy & Data",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Privacy Section
        Text(
            text = "Privacy",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsCardWithToggle(
            icon = Icons.Default.History,
            title = "Save listen history",
            subtitle = "Track songs you've played",
            isEnabled = userPreferences.saveListenHistory,
            onToggle = onToggleListenHistory
        )

        SettingsCardWithToggle(
            icon = Icons.Default.Search,
            title = "Save search history",
            subtitle = "Remember your searches",
            isEnabled = userPreferences.saveSearchHistory,
            onToggle = onToggleSearchHistory
        )

        SettingsCardWithToggle(
            icon = Icons.Default.BarChart,
            title = "Anonymous usage statistics",
            subtitle = "Help improve the app",
            isEnabled = userPreferences.sendAnonymousUsageStats,
            onToggle = onToggleAnonymousUsage
        )

        // Data Management
        Text(
            text = "Data Management",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Button(
            onClick = { showClearListenHistoryDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9E00).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = Color(0xFFFF9E00),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Clear Listen History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9E00)
            )
        }

        Button(
            onClick = { showClearSearchHistoryDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9E00).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = Color(0xFFFF9E00),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Clear Search History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9E00)
            )
        }

        // Data Export
        Text(
            text = "Data Export",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Button(
            onClick = onExportData,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00D9FF).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = Color(0xFF00D9FF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Export My Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D9FF)
            )
        }

        // Info Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF00D9FF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Export includes your playlists, favorites, and settings. Personal data is encrypted.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF9D4EDD).copy(alpha = 0.2f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF9D4EDD),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Your Privacy Matters",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "We don't sell your data. Anonymous statistics help us fix bugs and improve performance.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Clear Listen History Dialog
    if (showClearListenHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearListenHistoryDialog = false },
            containerColor = Color(0xFF0A2F3D),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Clear Listen History?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete your entire listening history. This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearListenHistory()
                        showClearListenHistoryDialog = false
                    }
                ) {
                    Text(
                        text = "Clear",
                        color = Color(0xFFFF9E00),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearListenHistoryDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )
    }

    // Clear Search History Dialog
    if (showClearSearchHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchHistoryDialog = false },
            containerColor = Color(0xFF0A2F3D),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Clear Search History?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will delete all your previous searches. This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearSearchHistory()
                        showClearSearchHistoryDialog = false
                    }
                ) {
                    Text(
                        text = "Clear",
                        color = Color(0xFFFF9E00),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchHistoryDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsCardWithToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00D9FF),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00D9FF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}