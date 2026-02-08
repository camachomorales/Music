package com.example.music.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.cache.SongCacheManager
import com.example.music.data.cache.StreamUrlCache
import com.example.music.data.model.AppMode
import com.example.music.data.model.RepeatMode
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.MusicRepository
import com.example.music.data.repository.StreamingMusicRepository
import com.example.music.service.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.music.data.api.MusicProviderType

class MusicPlayerViewModel(
    application: Application,
    private val musicRepository: MusicRepository
) : AndroidViewModel(application) {

    private val TAG = "MusicPlayerVM"
    private val streamingRepository = StreamingMusicRepository()
    private val streamUrlCache = StreamUrlCache(streamingRepository)
    private var musicService: MusicService? = null
    private var serviceBound = false
    private var searchJob: Job? = null

    private val songCacheManager = SongCacheManager(application)

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                "com.example.music.ACTION_SKIP_NEXT" -> skipToNext()
                "com.example.music.ACTION_SKIP_PREVIOUS" -> skipToPrevious()
                "com.example.music.ACTION_TOGGLE_SHUFFLE" -> toggleShuffle()
                "com.example.music.ACTION_CYCLE_REPEAT" -> cycleRepeatMode()
            }
        }
    }

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _streamingSongs = MutableStateFlow<List<StreamingSong>>(emptyList())
    val streamingSongs: StateFlow<List<StreamingSong>> = _streamingSongs.asStateFlow()

    private val _localSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val localSearchResults: StateFlow<List<Song>> = _localSearchResults.asStateFlow()

    // ✅ Estado persistente de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoadingStream = MutableStateFlow(false)
    val isLoadingStream: StateFlow<Boolean> = _isLoadingStream.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _isOnlineMode = MutableStateFlow(false)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    private val _appMode = MutableStateFlow(AppMode.OFFLINE)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private val _originalQueue = mutableListOf<Song>()
    private val _currentIndex = MutableStateFlow(0)

    private val streamingSongCache = mutableMapOf<String, StreamingSong>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            Log.d(TAG, "✅ Servicio conectado")
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
            Log.d(TAG, "❌ Servicio desconectado")
        }
    }

    init {
        Log.d(TAG, "🎵 Inicializando ViewModel")
        bindMusicService()
        loadLocalSongs()
        registerNotificationReceiver()
    }

    private fun registerNotificationReceiver() {
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.music.ACTION_SKIP_NEXT")
            addAction("com.example.music.ACTION_SKIP_PREVIOUS")
            addAction("com.example.music.ACTION_TOGGLE_SHUFFLE")
            addAction("com.example.music.ACTION_CYCLE_REPEAT")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                notificationReceiver,
                filter,
                android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().registerReceiver(
                notificationReceiver,
                filter,
                android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            getApplication<Application>().registerReceiver(notificationReceiver, filter)
        }

        Log.d(TAG, "📻 BroadcastReceiver registrado")
    }

    private fun bindMusicService() {
        try {
            val intent = Intent(getApplication(), MusicService::class.java)
            getApplication<Application>().startService(intent)
            getApplication<Application>().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            Log.d(TAG, "🔗 Binding servicio")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error binding servicio", e)
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            musicService?.let { service ->
                launch { service.getCurrentSong().collect { _currentSong.value = it } }
                launch { service.getIsPlaying().collect { _isPlaying.value = it } }
                launch { service.getCurrentPosition().collect { _currentPosition.value = it } }
                launch { service.getDuration().collect { _duration.value = it } }
                launch { service.getRepeatMode().collect { _repeatMode.value = it } }
                launch { service.getShuffleEnabled().collect { _shuffleEnabled.value = it } }
                launch { service.getIsBuffering().collect { _isBuffering.value = it } }
            }
        }
    }

    // ==================== LOAD SONGS ====================

    fun loadLocalSongs() {
        viewModelScope.launch {
            try {
                val localSongs = musicRepository.getSongsFromDevice()
                _songs.value = localSongs
                _originalQueue.clear()
                _originalQueue.addAll(localSongs)
                _queue.value = localSongs
                Log.d(TAG, "📱 ${localSongs.size} canciones locales cargadas")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando canciones: ${e.message}", e)
            }
        }
    }

    // ==================== PLAYBACK ====================

    fun playSong(song: Song, playlist: List<Song> = _songs.value) {
        Log.d(TAG, "▶️ Reproduciendo: ${song.title}")

        if (song.isStreaming && song.path.startsWith("streaming://")) {
            playStreamingSongFromPath(song, playlist)
            return
        }

        _originalQueue.clear()
        _originalQueue.addAll(playlist)

        val queue = if (_shuffleEnabled.value && _repeatMode.value != RepeatMode.ONE) {
            val shuffled = playlist.toMutableList()
            shuffled.shuffle()
            shuffled.remove(song)
            listOf(song) + shuffled
        } else {
            playlist
        }

        _queue.value = queue
        _currentIndex.value = queue.indexOf(song)
        musicService?.playSongWithQueue(song, queue)
    }

    // MusicPlayerViewModel.kt
// Reemplazar el método playStreamingSongFromPath() completo con este:

    private fun playStreamingSongFromPath(song: Song, playlist: List<Song>) {
        viewModelScope.launch {
            try {
                _isLoadingStream.value = true
                Log.d(TAG, "🔍 Resolviendo URL de streaming: ${song.path}")

                val parts = song.path.removePrefix("streaming://").split("/")
                if (parts.size < 2) {
                    Log.e(TAG, "❌ Path inválido: ${song.path}")
                    _isLoadingStream.value = false
                    Toast.makeText(getApplication(), "Invalid streaming path", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val providerName = parts[0]
                val songId = parts[1]

                val provider = when (providerName.lowercase()) {
                    "innertube" -> MusicProviderType.INNERTUBE
                    "jiosaavn" -> MusicProviderType.JIOSAAVN
                    "youtube_music" -> MusicProviderType.YOUTUBE_MUSIC
                    else -> {
                        Log.e(TAG, "❌ Provider desconocido: $providerName")
                        _isLoadingStream.value = false
                        Toast.makeText(getApplication(), "Unknown provider: $providerName", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                Log.d(TAG, "🎵 Provider: ${provider.displayName}, ID: $songId")

                val streamingSong = StreamingSong(
                    id = songId,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration,
                    thumbnailUrl = song.albumArtUri,
                    provider = provider
                )

                val resolvedUrl = streamUrlCache.getStreamUrl(streamingSong)

                if (resolvedUrl != null) {
                    // ✅ Preservar información de streaming
                    val resolvedSong = song.copy(
                        path = resolvedUrl,
                        isStreaming = true,
                        streamingId = songId,  // ✅ NUEVO
                        streamingProvider = provider.name  // ✅ NUEVO
                    )
                    Log.d(TAG, "✅ URL resuelta: ${resolvedUrl.take(100)}...")

                    _originalQueue.clear()
                    _originalQueue.addAll(playlist)

                    val queue = if (_shuffleEnabled.value && _repeatMode.value != RepeatMode.ONE) {
                        val shuffled = playlist.toMutableList()
                        shuffled.shuffle()
                        shuffled.remove(song)
                        listOf(resolvedSong) + shuffled
                    } else {
                        playlist.map {
                            if (it.id == song.id) resolvedSong else it
                        }
                    }

                    _queue.value = queue
                    _currentIndex.value = queue.indexOf(resolvedSong)
                    musicService?.playSongWithQueue(resolvedSong, queue)
                    _isLoadingStream.value = false

                    launch(Dispatchers.IO) {
                        try {
                            songCacheManager.downloadToCache(
                                url = resolvedUrl,
                                songId = songId,
                                onProgress = { progress ->
                                    if (progress % 25 == 0) {
                                        Log.d(TAG, "📥 Progreso: ${song.title} - $progress%")
                                    }
                                }
                            )
                            Log.d(TAG, "✅ Descargado a cache: ${song.title}")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error descargando: ${e.message}")
                        }
                    }
                } else {
                    _isLoadingStream.value = false
                    Log.e(TAG, "❌ No se pudo resolver URL")
                    Toast.makeText(getApplication(), "Could not get streaming URL", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo streaming: ${e.message}", e)
                _isLoadingStream.value = false
                Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // MusicPlayerViewModel.kt
// Reemplazar el método playStreamingSong() completo con este:

    fun playStreamingSong(streamingSong: StreamingSong) {
        viewModelScope.launch {
            try {
                _isLoadingStream.value = true
                val songId = streamingSong.id

                Log.d(TAG, "🎵 Reproduciendo: ${streamingSong.title}")

                // ✅ CASO 1: Canción en cache
                if (songCacheManager.isCached(songId)) {
                    val cachedPath = songCacheManager.getCachedPath(songId)

                    if (cachedPath != null) {
                        Log.d(TAG, "⚡ CACHE HIT - Reproducción instantánea: ${streamingSong.title}")

                        // ✅ SOLUCIÓN: Preservar información de streaming
                        val song = Song(
                            id = streamingSong.id.hashCode().toLong(),
                            title = streamingSong.title,
                            artist = streamingSong.artist,
                            album = streamingSong.album?.takeIf { it.isNotBlank() }
                                ?: streamingSong.artist,
                            duration = streamingSong.duration,
                            path = cachedPath,
                            albumArtUri = streamingSong.thumbnailUrl,
                            isStreaming = true,  // ✅ MANTENER true (era false antes - EL PROBLEMA)
                            streamingId = streamingSong.id,  // ✅ NUEVO: Preservar ID original
                            streamingProvider = streamingSong.provider.name  // ✅ NUEVO: Preservar provider
                        )

                        playSong(song, listOf(song))
                        _isLoadingStream.value = false

                        launch(Dispatchers.IO) {
                            preloadNextStreamingSongs(streamingSong)
                        }
                        return@launch
                    }
                }

                // ✅ CASO 2: Streaming directo (sin cache)
                Log.d(TAG, "🌐 Resolviendo URL: ${streamingSong.title}")

                val resolvedUrl = streamUrlCache.getStreamUrl(streamingSong)

                if (resolvedUrl != null) {
                    val song = Song(
                        id = streamingSong.id.hashCode().toLong(),
                        title = streamingSong.title,
                        artist = streamingSong.artist,
                        album = streamingSong.album?.takeIf { it.isNotBlank() }
                            ?: streamingSong.artist,
                        duration = streamingSong.duration,
                        path = resolvedUrl,
                        albumArtUri = streamingSong.thumbnailUrl,
                        isStreaming = true,  // ✅ true
                        streamingId = streamingSong.id,  // ✅ NUEVO
                        streamingProvider = streamingSong.provider.name  // ✅ NUEVO
                    )

                    playSong(song, listOf(song))
                    _isLoadingStream.value = false

                    Log.d(TAG, "▶️ Reproduciendo con ExoPlayer: ${streamingSong.title}")

                    launch(Dispatchers.IO) {
                        try {
                            songCacheManager.downloadToCache(
                                url = resolvedUrl,
                                songId = songId,
                                onProgress = { progress ->
                                    if (progress % 25 == 0) {
                                        Log.d(TAG, "📥 Progreso: ${streamingSong.title} - $progress%")
                                    }
                                }
                            )
                            Log.d(TAG, "✅ Descargado a cache: ${streamingSong.title}")

                            preloadNextStreamingSongs(streamingSong)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error descargando: ${e.message}")
                        }
                    }
                } else {
                    _isLoadingStream.value = false
                    Log.e(TAG, "❌ No se pudo resolver URL")
                    Toast.makeText(getApplication(), "Error al obtener URL", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo: ${e.message}", e)
                _isLoadingStream.value = false
                Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun preloadNextStreamingSongs(currentStreamingSong: StreamingSong) {
        val streamingSongs = _streamingSongs.value
        val currentIndex = streamingSongs.indexOf(currentStreamingSong)

        if (currentIndex == -1 || streamingSongs.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (i in 1..3) {
                    val nextIdx = (currentIndex + i) % streamingSongs.size
                    val nextSong = streamingSongs.getOrNull(nextIdx) ?: continue

                    if (songCacheManager.isCached(nextSong.id)) {
                        Log.d(TAG, "⚡ Ya en cache: ${nextSong.title}")
                        continue
                    }

                    val url = streamingRepository.getStreamUrl(nextSong)

                    if (url != null) {
                        songCacheManager.preloadChunk(url, nextSong.id, chunkSizeKB = 512)

                        if (i == 1) {
                            songCacheManager.downloadToCache(url, nextSong.id)
                        }
                    }
                }

                Log.d(TAG, "✅ Pre-carga completada")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error pre-cargando: ${e.message}")
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            songCacheManager.clearAll()
            Log.d(TAG, "🧹 Cache limpiado")
        }
    }

    fun getCacheInfo(): Pair<Int, Long> {
        val count = songCacheManager.getCachedSongsCount()
        val size = songCacheManager.getCacheSize()
        return Pair(count, size)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            musicService?.pause()
        } else {
            musicService?.play()
        }
    }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
    }

    fun seekTo(position: Float) {
        val seekPosition = (position * _duration.value).toLong()
        musicService?.seekTo(seekPosition)
    }

    // ==================== NAVIGATION ====================

    fun skipToNext() {
        val currentMode = _repeatMode.value
        val queue = _queue.value

        if (queue.isEmpty()) {
            Log.w(TAG, "⚠️ Cola vacía")
            return
        }

        when (currentMode) {
            RepeatMode.ONE -> playCurrentSong()
            RepeatMode.ALL -> {
                _currentIndex.value = (_currentIndex.value + 1) % queue.size
                playCurrentSong()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value < queue.size - 1) {
                    _currentIndex.value += 1
                    playCurrentSong()
                } else {
                    musicService?.pause()
                    _isPlaying.value = false
                }
            }
        }
    }

    fun skipToPrevious() {
        val currentMode = _repeatMode.value
        val queue = _queue.value

        if (queue.isEmpty()) {
            Log.w(TAG, "⚠️ Cola vacía")
            return
        }

        when (currentMode) {
            RepeatMode.ONE -> playCurrentSong()
            RepeatMode.ALL -> {
                _currentIndex.value = if (_currentIndex.value == 0) {
                    queue.size - 1
                } else {
                    _currentIndex.value - 1
                }
                playCurrentSong()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value > 0) {
                    _currentIndex.value -= 1
                    playCurrentSong()
                } else {
                    musicService?.seekTo(0)
                    _currentPosition.value = 0L
                }
            }
        }
    }

    fun toggleShuffle() {
        val newState = !_shuffleEnabled.value
        _shuffleEnabled.value = newState
        musicService?.setShuffle(newState)

        if (_repeatMode.value == RepeatMode.ONE) return

        val currentSong = _currentSong.value
        if (currentSong != null && _originalQueue.isNotEmpty()) {
            val newQueue = if (newState) {
                val shuffled = _originalQueue.toMutableList()
                shuffled.shuffle()
                shuffled.remove(currentSong)
                listOf(currentSong) + shuffled
            } else {
                _originalQueue.toList()
            }

            _queue.value = newQueue
            val newIndex = newQueue.indexOf(currentSong)
            _currentIndex.value = newIndex
            musicService?.updateQueue(newQueue, newIndex)
        }
    }

    fun cycleRepeatMode() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = newMode
        musicService?.setRepeat(newMode)

        if (newMode != RepeatMode.ONE && _shuffleEnabled.value && _originalQueue.isNotEmpty()) {
            val currentSong = _currentSong.value
            if (currentSong != null) {
                val shuffled = _originalQueue.toMutableList()
                shuffled.shuffle()
                shuffled.remove(currentSong)
                val newQueue = listOf(currentSong) + shuffled
                _queue.value = newQueue
                _currentIndex.value = 0
                musicService?.updateQueue(newQueue, 0)
            }
        }
    }

    private fun playCurrentSong() {
        val queue = _queue.value

        if (_currentIndex.value < 0 || _currentIndex.value >= queue.size) {
            _currentIndex.value = 0
            if (queue.isEmpty()) return
        }

        val song = queue[_currentIndex.value]
        _currentSong.value = song
        musicService?.playSongWithQueue(song, queue)
        _isPlaying.value = true
    }

    // ==================== STREAMING ====================

    fun toggleOnlineMode() {
        _isOnlineMode.value = !_isOnlineMode.value
        _appMode.value = if (_isOnlineMode.value) AppMode.STREAMING else AppMode.OFFLINE

        if (!_isOnlineMode.value) {
            loadLocalSongs()
            clearSearchCompletely()
            streamingSongCache.clear()
        } else {
            getTrending()
        }
    }

    fun toggleAppMode() = toggleOnlineMode()

    fun getTrending() {
        viewModelScope.launch {
            try {
                val trending = streamingRepository.getTrending(limit = 30)
                _streamingSongs.value = trending
                trending.forEach { streamingSongCache[it.id] = it }
                Log.d(TAG, "✅ ${trending.size} trending")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error trending: ${e.message}", e)
                _streamingSongs.value = emptyList()
            }
        }
    }

    // ==================== SEARCH ====================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            clearSearchResults()
        } else {
            if (_isOnlineMode.value) {
                searchStreamingSongs(query)
            } else {
                searchLocalSongs(query)
            }
        }
    }

    private fun clearSearchResults() {
        searchJob?.cancel()
        _streamingSongs.value = emptyList()
        _localSearchResults.value = emptyList()
        _isSearching.value = false
    }

    fun clearSearchCompletely() {
        _searchQuery.value = ""
        clearSearchResults()
    }

    fun searchLocalSongs(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _localSearchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)

            try {
                fun normalize(text: String): String {
                    return text.trim()
                        .lowercase()
                        .replace(Regex("\\s+"), " ")
                        .replace(Regex("[^a-z0-9\\s]"), "")
                }

                val normalizedQuery = normalize(query)
                val allSongs = _songs.value

                val results = allSongs.filter { song ->
                    val title = normalize(song.title)
                    val artist = normalize(song.artist)
                    val album = normalize(song.album)

                    title.contains(normalizedQuery) ||
                            artist.contains(normalizedQuery) ||
                            album.contains(normalizedQuery)
                }

                _localSearchResults.value = results
                Log.d(TAG, "🔍 Búsqueda local: '$query' → ${results.size} resultados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error búsqueda local: ${e.message}", e)
                _localSearchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchStreamingSongs(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _streamingSongs.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(800)

            try {
                Log.d(TAG, "🔍 Búsqueda streaming: '$query'")
                val results = streamingRepository.search(query, limit = 30)
                _streamingSongs.value = results
                results.forEach { streamingSongCache[it.id] = it }
                Log.d(TAG, "✅ ${results.size} resultados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error búsqueda streaming: ${e.message}", e)
                _streamingSongs.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        clearSearchCompletely()
    }

    // ==================== CACHE STREAMING SONG ====================

    fun cacheStreamingSong(streamingSong: StreamingSong) {
        streamingSongCache[streamingSong.id] = streamingSong
        Log.d(TAG, "💾 StreamingSong cacheado: ${streamingSong.id} - ${streamingSong.title}")
    }

    // ==================== MIXED PLAYLIST PLAYBACK ====================

    fun playMixedPlaylist(
        song: Song,
        playlist: List<Song>,
        streamingSongs: List<StreamingSong>,
        playlistName: String? = null
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎵 Reproduciendo playlist mixta: ${song.title}, playlist: $playlistName")

                // Cachear todas las StreamingSongs
                streamingSongs.forEach { streamingSongCache[it.id] = it }

                // Modificar álbum solo si hay nombre de playlist
                val playlistWithAlbum = if (!playlistName.isNullOrBlank()) {
                    playlist.map { it.copy(album = playlistName) }
                } else {
                    playlist
                }

                _originalQueue.clear()
                _originalQueue.addAll(playlistWithAlbum)
                _queue.value = playlistWithAlbum

                val startIndex = playlistWithAlbum.indexOfFirst { it.id == song.id }
                if (startIndex != -1) {
                    _currentIndex.value = startIndex
                }

                val songWithPlaylistName = if (!playlistName.isNullOrBlank()) {
                    song.copy(album = playlistName)
                } else {
                    song
                }

                if (songWithPlaylistName.isStreaming && songWithPlaylistName.path.startsWith("streaming://")) {
                    playStreamingSongFromPath(songWithPlaylistName, playlistWithAlbum)
                } else {
                    playSong(songWithPlaylistName, playlistWithAlbum)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo playlist mixta", e)
            }
        }
    }

    // ==================== FAVORITES ====================

    fun toggleFavoriteBySongId(songId: Long) {
        val songIdStr = songId.toString()
        val currentFavorites = _favorites.value.toMutableSet()
        if (currentFavorites.contains(songIdStr)) {
            currentFavorites.remove(songIdStr)
        } else {
            currentFavorites.add(songIdStr)
        }
        _favorites.value = currentFavorites
    }

    fun toggleFavorite(songId: Long) {
        toggleFavoriteBySongId(songId)
    }

    // ==================

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        streamUrlCache.cleanup()

        try {
            getApplication<Application>().unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }

        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
        Log.d(TAG, "🧹 ViewModel limpiado")
    }
}