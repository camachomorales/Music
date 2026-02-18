package com.example.music.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.music.R
import com.example.music.audio.MusicPlayerScreen
import com.example.music.audio.PlayerState
import com.example.music.data.model.Album
import com.example.music.data.model.AppMode
import com.example.music.data.model.Artist
import com.example.music.data.model.Song
import com.example.music.ui.theme.components.BottomNavigationBar
import com.example.music.ui.theme.components.MiniPlayer
import com.example.music.ui.theme.screens.*
import com.example.music.viewmodel.MusicPlayerViewModel
import com.example.music.viewmodel.LibraryViewModel_OLD
import android.widget.Toast
import androidx.compose.material3.*
import com.example.music.data.model.*
import com.example.music.viewmodel.UserPreferencesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.music.ui.screens.LibraryScreen
import com.example.music.ui.screens.SettingsScreen
import com.example.music.ui.screens.SettingsScreen.AccountScreen
import com.example.music.ui.screens.SettingsScreen.PlaybackSettingsScreen

@Composable
fun MainNavigation(viewModel: MusicPlayerViewModel, songs: List<Song>) {
    val navController = rememberNavController()

    // ✅ LibraryViewModel
    val libraryViewModel: LibraryViewModel_OLD = viewModel()

    // Estados del MusicPlayerViewModel
    val streamingSongs by viewModel.streamingSongs.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isSearchingStreaming by viewModel.isSearching.collectAsStateWithLifecycle()
    val localSearchResults by viewModel.localSearchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // ✅ Estados del LibraryViewModel
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val favoriteSongIds by libraryViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val favoriteStreamingSongIds by libraryViewModel.favoriteStreamingSongIds.collectAsStateWithLifecycle()
    val recentlyPlayedSongs by libraryViewModel.recentlyPlayedSongs.collectAsStateWithLifecycle()

    // Determinar si estamos en modo online
    val isOnlineMode = appMode == AppMode.STREAMING

    // Ruta actual
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                // Mini Player (solo visible si hay canción y no estamos en PlayerScreen)
                if (currentSong != null && currentRoute != "player") {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        progress = 0f,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.skipToNext() },
                        onMiniPlayerClick = { navController.navigate("player") }
                    )
                }

                // Bottom Navigation
                if (currentRoute != "player") {
                    BottomNavigationBar(
                        selectedRoute = currentRoute ?: "home",
                        onItemSelected = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo de la app
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Navegación
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                // ==================== HOME SCREEN ====================
                composable("home") {
                    HomeScreen(
                        songs = if (isOnlineMode) emptyList() else songs,
                        streamingSongs = if (isOnlineMode) streamingSongs else emptyList(),
                        appMode = appMode,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, songs)
                            navController.navigate("player")
                        },
                        onStreamingSongClick = { streamingSong ->
                            viewModel.cacheStreamingSong(streamingSong)

                            val song = Song(
                                id = streamingSong.id.hashCode().toLong(),
                                title = streamingSong.title,
                                artist = streamingSong.artist,
                                album = streamingSong.album ?: streamingSong.artist,
                                duration = streamingSong.duration,
                                path = "streaming://${streamingSong.provider.name.lowercase()}/${streamingSong.id}",
                                albumArtUri = streamingSong.thumbnailUrl,
                                isStreaming = true,
                                streamingId = streamingSong.id,
                                streamingProvider = streamingSong.provider.name
                            )

                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playStreamingSong(streamingSong)
                            navController.navigate("player")
                        },
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onSearch = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        searchQuery = searchQuery,
                        isSearching = isSearchingStreaming,
                        isFavorite = { songId ->
                            favoriteSongIds.contains(songId)
                        },
                        onToggleFavorite = { song ->
                            libraryViewModel.toggleFavorite(song)
                        },
                        isStreamingFavorite = { streamingId ->
                            favoriteStreamingSongIds.contains(streamingId)
                        },
                        onToggleStreamingFavorite = { streamingSong ->
                            libraryViewModel.toggleStreamingFavorite(streamingSong)
                        }
                    )
                }

                // ==================== SEARCH SCREEN ====================
                composable("search") {
                    SearchScreen(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        localSearchResults = if (isOnlineMode) emptyList() else localSearchResults,
                        streamingSearchResults = if (isOnlineMode) streamingSongs else emptyList(),
                        isOnlineMode = isOnlineMode,
                        isSearching = isSearchingStreaming,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, localSearchResults)
                            navController.navigate("player")
                        },
                        onStreamingSongClick = { streamingSong ->
                            viewModel.cacheStreamingSong(streamingSong)

                            val song = Song(
                                id = streamingSong.id.hashCode().toLong(),
                                title = streamingSong.title,
                                artist = streamingSong.artist,
                                album = streamingSong.album ?: streamingSong.artist,
                                duration = streamingSong.duration,
                                path = "streaming://${streamingSong.provider.name.lowercase()}/${streamingSong.id}",
                                albumArtUri = streamingSong.thumbnailUrl,
                                isStreaming = true,
                                streamingId = streamingSong.id,
                                streamingProvider = streamingSong.provider.name
                            )

                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playStreamingSong(streamingSong)
                            navController.navigate("player")
                        },
                        isFavorite = { songId ->
                            favoriteSongIds.contains(songId)
                        },
                        onToggleFavorite = { song ->
                            libraryViewModel.toggleFavorite(song)
                        },
                        isStreamingFavorite = { streamingId ->
                            favoriteStreamingSongIds.contains(streamingId)
                        },
                        onToggleStreamingFavorite = { streamingSong ->
                            libraryViewModel.toggleStreamingFavorite(streamingSong)
                        }
                    )
                }

                // ==================== LIBRARY SCREEN ====================
                composable("library") {
                    LibraryScreen(
                        currentMode = appMode,
                        onToggleMode = {
                            viewModel.toggleAppMode()
                        },
                        onPlaylistsClick = {
                            navController.navigate("playlists")
                        },
                        onSongsClick = {
                            navController.navigate("songs")
                        },
                        onAlbumsClick = {
                            navController.navigate("albums")
                        },
                        onArtistsClick = {
                            navController.navigate("artists")
                        },
                        onFavoritesClick = {
                            navController.navigate("favorites")
                        },
                        onRecentlyPlayedClick = {
                            navController.navigate("recently_played")
                        }
                    )
                }

                // ==================== FAVORITES SCREEN ====================
                composable("favorites") {
                    val localFavoriteSongs = remember(playlists, favoriteSongIds) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { favoriteSongIds.contains(it.id) }
                    }

                    val recentFavoriteSongs = remember(recentlyPlayedSongs, favoriteSongIds, favoriteStreamingSongIds) {
                        recentlyPlayedSongs.filter { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }
                                streamingId?.let { favoriteStreamingSongIds.contains(it) } ?: false
                            } else {
                                favoriteSongIds.contains(song.id)
                            }
                        }
                    }

                    val allFavoriteSongs = remember(localFavoriteSongs, recentFavoriteSongs) {
                        (localFavoriteSongs + recentFavoriteSongs).distinctBy { it.id }
                    }

                    FavoritesScreen(
                        favoriteSongs = allFavoriteSongs,
                        appMode = appMode,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, allFavoriteSongs)
                            navController.navigate("player")
                        },
                        onToggleFavorite = { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }

                                streamingId?.let { id ->
                                    val provider = song.streamingProvider?.let { providerName ->
                                        when (providerName.uppercase()) {
                                            "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                            "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                        }
                                    } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                    val streamingSong = com.example.music.data.model.StreamingSong(
                                        id = id,
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        duration = song.duration,
                                        thumbnailUrl = song.albumArtUri,
                                        provider = provider
                                    )

                                    libraryViewModel.toggleStreamingFavorite(streamingSong)
                                }
                            } else {
                                libraryViewModel.toggleFavorite(song)
                            }
                        },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // ==================== RECENTLY PLAYED SCREEN ====================
                composable("recently_played") {
                    RecentlyPlayedScreen(
                        recentlyPlayedSongs = recentlyPlayedSongs,
                        appMode = appMode,
                        favoriteSongIds = favoriteSongIds,
                        favoriteStreamingSongIds = favoriteStreamingSongIds,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, recentlyPlayedSongs)
                            navController.navigate("player")
                        },
                        onToggleFavorite = { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }

                                streamingId?.let { id ->
                                    val provider = song.streamingProvider?.let { providerName ->
                                        when (providerName.uppercase()) {
                                            "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                            "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                        }
                                    } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                    val streamingSong = com.example.music.data.model.StreamingSong(
                                        id = id,
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        duration = song.duration,
                                        thumbnailUrl = song.albumArtUri,
                                        provider = provider
                                    )

                                    libraryViewModel.toggleStreamingFavorite(streamingSong)
                                }
                            } else {
                                libraryViewModel.toggleFavorite(song)
                            }
                        },
                        onClearHistory = {
                            libraryViewModel.clearRecentlyPlayed()
                        },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYLISTS SCREEN ====================
                composable("playlists") {
                    PlaylistsScreen(
                        playlists = playlists,
                        onPlaylistClick = { playlist ->
                            navController.navigate("playlist_detail/${playlist.id}")
                        },
                        onCreatePlaylist = { name, description ->
                            libraryViewModel.createPlaylist(name, description)
                        },
                        onDeletePlaylist = { playlistId ->
                            libraryViewModel.deletePlaylist(playlistId)
                        },
                        onRenamePlaylist = { playlistId, newName ->
                            libraryViewModel.renamePlaylist(playlistId, newName)
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYLIST DETAIL SCREEN ====================
                composable(
                    route = "playlist_detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    val playlist = playlists.find { it.id == playlistId } ?: return@composable

                    PlaylistDetailScreen(
                        playlist = playlist,
                        allSongs = songs,
                        currentMode = appMode,
                        streamingSongs = streamingSongs,
                        isSearching = isSearchingStreaming,
                        onSearchStreaming = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        onBackClick = { navController.navigateUp() },
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playMixedPlaylist(
                                song = song,
                                playlist = playlist.songs,
                                streamingSongs = streamingSongs,
                                playlistName = playlist.name
                            )
                            navController.navigate("player")
                        },
                        onRemoveSong = { songId ->
                            libraryViewModel.removeSongFromPlaylist(playlistId, songId)
                        },
                        onAddSong = { song ->
                            libraryViewModel.addSongToPlaylist(playlistId, song)
                        },
                        onAddStreamingSong = { streamingSong ->
                            viewModel.cacheStreamingSong(streamingSong)

                            val song = Song(
                                id = streamingSong.id.hashCode().toLong(),
                                title = streamingSong.title,
                                artist = streamingSong.artist,
                                album = playlist.name,
                                duration = streamingSong.duration,
                                path = "streaming://${streamingSong.provider.name.lowercase()}/${streamingSong.id}",
                                albumArtUri = streamingSong.thumbnailUrl,
                                isStreaming = true,
                                streamingId = streamingSong.id,
                                streamingProvider = streamingSong.provider.name
                            )
                            libraryViewModel.addSongToPlaylist(playlistId, song)
                        },
                        onReorderSongs = { reorderedSongs ->
                            libraryViewModel.reorderPlaylistSongs(playlistId, reorderedSongs)
                        },
                        onPlayAll = {
                            if (playlist.songs.isNotEmpty()) {
                                libraryViewModel.addToRecentlyPlayed(playlist.songs.first())
                                viewModel.playMixedPlaylist(
                                    song = playlist.songs.first(),
                                    playlist = playlist.songs,
                                    streamingSongs = streamingSongs,
                                    playlistName = playlist.name
                                )
                                navController.navigate("player")
                            }
                        },
                        onShufflePlay = {
                            if (playlist.songs.isNotEmpty()) {
                                val shuffled = playlist.songs.shuffled()
                                libraryViewModel.addToRecentlyPlayed(shuffled.first())
                                viewModel.playMixedPlaylist(
                                    song = shuffled.first(),
                                    playlist = shuffled,
                                    streamingSongs = streamingSongs,
                                    playlistName = playlist.name
                                )
                                navController.navigate("player")
                            }
                        }
                    )
                }

                // ==================== SONGS (ALL SONGS) SCREEN ====================
                composable("songs") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    AllSongsScreen(
                        songs = filteredSongs,
                        onSongClick = { song, songList ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, songList)
                            navController.navigate("player")
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ALBUMS SCREEN ====================
                composable("albums") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    val albumsList = remember(filteredSongs) {
                        filteredSongs.groupBy { it.album }.map { (albumName, albumSongs) ->
                            Album(
                                id = albumName,
                                title = albumName,
                                artist = albumSongs.firstOrNull()?.artist ?: "Unknown",
                                coverUri = albumSongs.firstOrNull()?.albumArtUri,
                                year = null,
                                songs = albumSongs
                            )
                        }
                    }

                    AlbumsScreen(
                        albums = albumsList,
                        onAlbumClick = { album -> },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ARTISTS SCREEN ====================
                composable("artists") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    val artistsList = remember(filteredSongs) {
                        filteredSongs.groupBy { it.artist }.map { (artistName, artistSongs) ->
                            val albumsForArtist = artistSongs.groupBy { it.album }.map { (albumName, albumSongs) ->
                                Album(
                                    id = albumName,
                                    title = albumName,
                                    artist = artistName,
                                    coverUri = albumSongs.firstOrNull()?.albumArtUri,
                                    songs = albumSongs
                                )
                            }

                            Artist(
                                id = artistName,
                                name = artistName,
                                imageUri = null,
                                albums = albumsForArtist,
                                songs = artistSongs
                            )
                        }
                    }

                    ArtistsScreen(
                        artists = artistsList,
                        onArtistClick = { artist -> },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== USER SCREEN ====================
                composable("user") {
                    val context = LocalContext.current

                    val userPreferencesViewModel: UserPreferencesViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return UserPreferencesViewModel(context) as T
                            }
                        }
                    )
                    val userPreferences by userPreferencesViewModel.userPreferences.collectAsStateWithLifecycle()
                    val accountType by userPreferencesViewModel.accountType.collectAsStateWithLifecycle()

                    SettingsScreen(
                        appMode = appMode,
                        userPreferences = userPreferences,
                        accountType = accountType,
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onAccountClick = {
                            navController.navigate("account")
                        },
                        onPlaybackClick = {
                            navController.navigate("playback_settings")
                        },
                        onStorageClick = {
                            Toast.makeText(context, "Storage screen coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onNotificationsClick = {
                            userPreferencesViewModel.toggleNotifications()
                            Toast.makeText(context, "Notifications setting updated", Toast.LENGTH_SHORT).show()
                        },
                        onAboutClick = {
                            Toast.makeText(context, "About screen coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onToggleDarkTheme = {
                            userPreferencesViewModel.toggleDarkTheme()
                            Toast.makeText(context, "Theme updated", Toast.LENGTH_SHORT).show()
                        },
                        onToggleDownload = {
                            userPreferencesViewModel.toggleDownloadOnlyOnWifi()
                            Toast.makeText(context, "Download settings updated", Toast.LENGTH_SHORT).show()
                        },
                        onClearCache = {
                            viewModel.clearCache()
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ACCOUNT SCREEN ====================
                composable("account") {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    val userPreferencesViewModel: UserPreferencesViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return UserPreferencesViewModel(context) as T
                            }
                        }
                    )

                    val userPreferences by userPreferencesViewModel.userPreferences.collectAsStateWithLifecycle()
                    val accountType by userPreferencesViewModel.accountType.collectAsStateWithLifecycle()

                    // ✅ Get current user from LibraryViewModel
                    val currentUser by libraryViewModel.currentUser.collectAsStateWithLifecycle()

                    var isLoading by remember { mutableStateOf(false) }
                    var isSyncing by remember { mutableStateOf(false) }

                    AccountScreen(
                        userPreferences = userPreferences,
                        accountType = accountType,
                        isLoading = isLoading,
                        isSyncing = isSyncing,
                        onLogin = { email, password ->
                            isLoading = true
                            scope.launch {
                                try {
                                    if (email.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                        return@launch
                                    }

                                    // ✅ LOGIN using LibraryViewModel
                                    val result = libraryViewModel.loginUser(email, password)

                                    if (result.isSuccess) {
                                        val user = result.getOrNull()

                                        if (user != null) {
                                            userPreferencesViewModel.updateLoginState(
                                                isLoggedIn = true,
                                                isAdmin = user.isAdmin,
                                                email = user.email,
                                                userName = user.userName
                                            )

                                            Toast.makeText(context, "Logged in successfully as ${user.userName}", Toast.LENGTH_SHORT).show()
                                            navController.navigateUp()
                                        } else {
                                            Toast.makeText(context, "Login failed: No user data", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                        Toast.makeText(context, "Login failed: $errorMessage", Toast.LENGTH_SHORT).show()
                                    }
                                    isLoading = false
                                } catch (e: Exception) {
                                    isLoading = false
                                    Toast.makeText(context, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onRegister = { email, password, name ->
                            isLoading = true
                            scope.launch {
                                try {
                                    if (email.isBlank() || password.isBlank() || name.isBlank()) {
                                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                        return@launch
                                    }

                                    // ✅ REGISTER using LibraryViewModel
                                    val result = libraryViewModel.registerUser(email, password, name)

                                    if (result.isSuccess) {
                                        val user = result.getOrNull()

                                        if (user != null) {
                                            userPreferencesViewModel.updateLoginState(
                                                isLoggedIn = true,
                                                isAdmin = user.isAdmin,
                                                email = user.email,
                                                userName = user.userName
                                            )

                                            Toast.makeText(context, "Account created successfully! Welcome ${user.userName}", Toast.LENGTH_SHORT).show()
                                            navController.navigateUp()
                                        } else {
                                            Toast.makeText(context, "Registration failed: No user data", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                        Toast.makeText(context, "Registration failed: $errorMessage", Toast.LENGTH_SHORT).show()
                                    }
                                    isLoading = false
                                } catch (e: Exception) {
                                    isLoading = false
                                    Toast.makeText(context, "Registration error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onLogout = {
                            scope.launch {
                                // ✅ LOGOUT using both ViewModels
                                libraryViewModel.logoutUser()
                                userPreferencesViewModel.logoutUser()
                                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSync = {
                            isSyncing = true
                            scope.launch {
                                try {
                                    // ✅ SYNC using LibraryViewModel
                                    libraryViewModel.syncUserData()
                                    isSyncing = false
                                    Toast.makeText(context, "Data synchronized successfully ✅", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    isSyncing = false
                                    Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYBACK SETTINGS SCREEN ====================
                composable("playback_settings") {
                    val context = LocalContext.current

                    val userPreferencesViewModel: UserPreferencesViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return UserPreferencesViewModel(context) as T
                            }
                        }
                    )
                    val userPreferences by userPreferencesViewModel.userPreferences.collectAsStateWithLifecycle()

                    PlaybackSettingsScreen(
                        userPreferences = userPreferences,
                        onAudioQualityChange = { newQuality ->
                            userPreferencesViewModel.setAudioQuality(newQuality)
                            Toast.makeText(context, "Audio quality set to ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onStreamingQualityChange = { newQuality ->
                            userPreferencesViewModel.setStreamingQuality(newQuality)
                            Toast.makeText(context, "Streaming quality set to ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onDownloadQualityChange = { newQuality ->
                            userPreferencesViewModel.setDownloadQuality(newQuality)
                            Toast.makeText(context, "Download quality set to ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onCrossfadeChange = { seconds ->
                            userPreferencesViewModel.setCrossfadeDuration(seconds)
                            Toast.makeText(context, "Crossfade set to ${seconds}s", Toast.LENGTH_SHORT).show()
                        },
                        onToggleGapless = {
                            userPreferencesViewModel.toggleGaplessPlayback()
                            Toast.makeText(context, "Gapless playback toggled", Toast.LENGTH_SHORT).show()
                        },
                        onToggleNormalize = {
                            userPreferencesViewModel.toggleNormalizeVolume()
                            Toast.makeText(context, "Volume normalization toggled", Toast.LENGTH_SHORT).show()
                        },
                        onEqualizerChange = { preset ->
                            userPreferencesViewModel.setEqualizerPreset(preset)
                            Toast.makeText(context, "Equalizer preset set to ${preset.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYER SCREEN ====================
                composable("player") {
                    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
                    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
                    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
                    val duration by viewModel.duration.collectAsStateWithLifecycle()
                    val isLoadingStream by viewModel.isLoadingStream.collectAsStateWithLifecycle()

                    // ✅ DETECTAR SI ES FAVORITO (LOCAL O STREAMING)
                    val isLiked = remember(currentSong, favoriteSongIds, favoriteStreamingSongIds) {
                        currentSong?.let { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }
                                streamingId?.let { favoriteStreamingSongIds.contains(it) } ?: false
                            } else {
                                favoriteSongIds.contains(song.id)
                            }
                        } ?: false
                    }

                    MusicPlayerScreen(
                        playerState = PlayerState(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            isShuffleEnabled = shuffleEnabled,
                            repeatMode = repeatMode,
                            isLiked = isLiked,
                            isLoadingStream = isLoadingStream
                        ),
                        appMode = appMode,
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipToNext() },
                        onSkipPrevious = { viewModel.skipToPrevious() },
                        onSeek = { position -> viewModel.seekTo(position) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.cycleRepeatMode() },
                        onToggleLike = {
                            currentSong?.let { song ->
                                if (song.isStreaming) {
                                    val streamingId = song.streamingId ?: run {
                                        if (song.path.startsWith("streaming://")) {
                                            val parts = song.path.removePrefix("streaming://").split("/")
                                            if (parts.size >= 2) parts[1] else null
                                        } else null
                                    }

                                    streamingId?.let { id ->
                                        val provider = song.streamingProvider?.let { providerName ->
                                            when (providerName.uppercase()) {
                                                "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                                "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                                "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                                else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            }
                                        } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                        val streamingSong = com.example.music.data.model.StreamingSong(
                                            id = id,
                                            title = song.title,
                                            artist = song.artist,
                                            album = song.album,
                                            duration = song.duration,
                                            thumbnailUrl = song.albumArtUri,
                                            provider = provider
                                        )
                                        libraryViewModel.toggleStreamingFavorite(streamingSong)
                                    }
                                } else {
                                    libraryViewModel.toggleFavorite(song)
                                }
                            }
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}