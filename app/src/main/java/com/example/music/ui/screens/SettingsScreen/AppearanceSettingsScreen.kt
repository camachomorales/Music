package com.example.music.ui.screens.SettingsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun AppearanceSettingsScreen(
    userPreferences: UserPreferences,
    onToggleDarkTheme: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onToggleAlbumArt: () -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onThemeChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }

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
                text = "Appearance",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Theme Section
        Text(
            text = "Theme",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsCardWithToggle(
            icon = Icons.Default.DarkMode,
            title = "Dark theme",
            subtitle = "Use dark theme throughout the app",
            isEnabled = userPreferences.isDarkTheme,
            onToggle = onToggleDarkTheme
        )

        SettingsCardWithToggle(
            icon = Icons.Default.Palette,
            title = "Dynamic colors",
            subtitle = "Use colors from your wallpaper (Android 12+)",
            isEnabled = userPreferences.useDynamicColors,
            onToggle = onToggleDynamicColor
        )

        SettingsCard(
            icon = Icons.Default.ColorLens,
            title = "Theme color",
            subtitle = userPreferences.themeColor ?: "Cyan (Default)",
            onClick = { showThemeDialog = true }
        )

        // Layout Section
        Text(
            text = "Layout",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsCard(
            icon = Icons.Default.GridView,
            title = "Grid columns",
            subtitle = "${userPreferences.gridColumns} columns",
            onClick = { showGridDialog = true }
        )

        SettingsCardWithToggle(
            icon = Icons.Default.Image,
            title = "Show album art",
            subtitle = "Display album artwork throughout the app",
            isEnabled = userPreferences.showAlbumArtInApp,
            onToggle = onToggleAlbumArt
        )

        // Preview Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Text(
                text = "Preview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(userPreferences.gridColumns) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF00D9FF).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = Color(0xFF00D9FF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Theme Color Dialog
    if (showThemeDialog) {
        ThemeColorDialog(
            currentTheme = userPreferences.themeColor ?: "Cyan",
            onDismiss = { showThemeDialog = false },
            onSelect = {
                onThemeChange(it)
                showThemeDialog = false
            }
        )
    }

    // Grid Columns Dialog
    if (showGridDialog) {
        GridColumnsDialog(
            currentColumns = userPreferences.gridColumns,
            onDismiss = { showGridDialog = false },
            onSelect = {
                onGridColumnsChange(it)
                showGridDialog = false
            }
        )
    }
}

@Composable
private fun ThemeColorDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val themes = listOf(
        "Cyan" to Color(0xFF00D9FF),
        "Pink" to Color(0xFFFF006E),
        "Purple" to Color(0xFF9D4EDD),
        "Green" to Color(0xFF06FFA5),
        "Orange" to Color(0xFFFF9E00),
        "Blue" to Color(0xFF0099CC)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A2F3D),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Theme Color",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (name, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (name == currentTheme)
                                    color.copy(alpha = 0.2f)
                                else
                                    Color.Transparent
                            )
                            .clickable { onSelect(name) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        if (name == currentTheme) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun GridColumnsDialog(
    currentColumns: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A2F3D),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Grid Columns",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { columns ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (columns == currentColumns)
                                    Color(0xFF00D9FF).copy(alpha = 0.2f)
                                else
                                    Color.Transparent
                            )
                            .clickable { onSelect(columns) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$columns ${if (columns == 1) "column" else "columns"}",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        if (columns == currentColumns) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00D9FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
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

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
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