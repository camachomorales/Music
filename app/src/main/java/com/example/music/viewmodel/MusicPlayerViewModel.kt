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
                "com.example.music.ACTION_SKIP_NEXT"     -> skipToNext()
                "com.example.music.ACTION_SKIP_PREVIOUS" -> skipToPrevious()
                "com.example.music.ACTION_TOGGLE_SHUFFLE" -> toggleShuffle()
                "com.example.music.ACTION_CYCLE_REPEAT"  -> cycleRepeatMode()
            }
        }
    }

    // ==================== STATE FLOWS ====================

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _streamingSongs = MutableStateFlow<List<StreamingSong>>(emptyList())
    val streamingSongs: StateFlow<List<StreamingSong>> = _streamingSongs.asStateFlow()

    private val _localSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val localSearchResults: StateFlow<List<Song>> = _localSearchResults.asStateFlow()

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

    // ==================== QUEUE MANAGEMENT ====================
    // _originalQueue: always the unshuffled order set when a playlist starts
    // _queue:         the active playback order (may be shuffled)
    // _currentIndex:  index into _queue of the currently playing song

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private val _originalQueue = mutableListOf<Song>()
    private val _currentIndex = MutableStateFlow(0)

    private val streamingSongCache = mutableMapOf<String, StreamingSong>()

    // ==================== SERVICE CONNECTION ====================

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

    // ==================== INIT HELPERS ====================

    private fun registerNotificationReceiver() {
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.music.ACTION_SKIP_NEXT")
            addAction("com.example.music.ACTION_SKIP_PREVIOUS")
            addAction("com.example.music.ACTION_TOGGLE_SHUFFLE")
            addAction("com.example.music.ACTION_CYCLE_REPEAT")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                notificationReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().registerReceiver(
                notificationReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED
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
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "🔗 Binding servicio")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error binding servicio", e)
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            musicService?.let { service ->
                launch { service.getCurrentSong().collect    { _currentSong.value    = it } }
                launch { service.getIsPlaying().collect      { _isPlaying.value      = it } }
                launch { service.getCurrentPosition().collect{ _currentPosition.value = it } }
                launch { service.getDuration().collect       { _duration.value       = it } }
                launch { service.getRepeatMode().collect     { _repeatMode.value     = it } }
                launch { service.getShuffleEnabled().collect { _shuffleEnabled.value = it } }
                launch { service.getIsBuffering().collect    { _isBuffering.value    = it } }
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

    /**
     * Entry point for playing a song.
     * Builds the correct queue (shuffled or not) and delegates to the service.
     */
    fun playSong(song: Song, playlist: List<Song> = _songs.value) {
        Log.d(TAG, "▶️ Reproduciendo: ${song.title}")

        if (song.isStreaming && song.path.startsWith("streaming://")) {
            playStreamingSongFromPath(song, playlist)
            return
        }

        // Save the canonical unshuffled order
        _originalQueue.clear()
        _originalQueue.addAll(playlist)

        // Build the active queue depending on shuffle state
        // RepeatMode.ONE: shuffle has no effect on queue order (play the single song)
        val queue = buildQueue(song, playlist)

        _queue.value = queue
        _currentIndex.value = queue.indexOf(song).coerceAtLeast(0)
        musicService?.playSongWithQueue(song, queue)
        _isPlaying.value = true
    }

    /**
     * Builds the playback queue for a given song + playlist honoring shuffle state.
     * RepeatMode.ONE is intentionally ignored here — the queue still contains all songs
     * so that the user can navigate away; repeat-one is enforced during auto-completion.
     */
    private fun buildQueue(startSong: Song, playlist: List<Song>): List<Song> {
        return if (_shuffleEnabled.value) {
            val shuffled = playlist.toMutableList()
            shuffled.remove(startSong)
            shuffled.shuffle()
            listOf(startSong) + shuffled
        } else {
            playlist
        }
    }

    // ==================== STREAMING PLAYBACK ====================

    /**
     * Resolves a streaming:// path to a real URL and plays it.
     *
     * @param rebuildQueue true  → fresh play (user tapped a song): rebuild full queue.
     *                     false → navigation (skip next/prev): keep queue, only patch
     *                             the resolved URL at the current index slot.
     */
    private fun playStreamingSongFromPath(
        song: Song,
        playlist: List<Song>,
        rebuildQueue: Boolean = true
    ) {
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
                val songId       = parts[1]

                val provider = when (providerName.lowercase()) {
                    "innertube"     -> MusicProviderType.INNERTUBE
                    "jiosaavn"      -> MusicProviderType.JIOSAAVN
                    "youtube_music" -> MusicProviderType.YOUTUBE_MUSIC
                    else -> {
                        Log.e(TAG, "❌ Provider desconocido: $providerName")
                        _isLoadingStream.value = false
                        Toast.makeText(getApplication(), "Unknown provider: $providerName", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                // Check cache first for instant playback
                val cachedPath = if (songCacheManager.isCached(songId))
                    songCacheManager.getCachedPath(songId) else null

                val resolvedUrl = cachedPath ?: streamUrlCache.getStreamUrl(
                    StreamingSong(
                        id           = songId,
                        title        = song.title,
                        artist       = song.artist,
                        album        = song.album,
                        duration     = song.duration,
                        thumbnailUrl = song.albumArtUri,
                        provider     = provider
                    )
                )

                if (resolvedUrl != null) {
                    // If _isLoadingStream was cleared by a newer skip, abort silently
                    if (!_isLoadingStream.value) {
                        Log.d(TAG, "⚠️ Skip cancelado — ya hay una nueva navegación en curso")
                        return@launch
                    }

                    val resolvedSong = song.copy(
                        path              = resolvedUrl,
                        isStreaming       = true,
                        streamingId       = songId,
                        streamingProvider = provider.name
                    )
                    Log.d(TAG, "✅ URL resuelta: ${resolvedUrl.take(80)}...")

                    if (rebuildQueue) {
                        // Fresh play: build a new full queue
                        _originalQueue.clear()
                        _originalQueue.addAll(playlist)
                        val queue = buildQueue(resolvedSong, playlist.map {
                            if (it.id == song.id) resolvedSong else it
                        })
                        _queue.value        = queue
                        _currentIndex.value = queue.indexOf(resolvedSong).coerceAtLeast(0)
                        musicService?.playSongWithQueue(resolvedSong, queue)
                    } else {
                        // Navigation skip: patch only the current slot, keep queue intact
                        val currentQueue  = _queue.value.toMutableList()
                        val idx           = _currentIndex.value.coerceIn(currentQueue.indices)
                        currentQueue[idx] = resolvedSong
                        _queue.value      = currentQueue
                        // Also patch originalQueue so shuffle restore is correct
                        val origIdx = _originalQueue.indexOfFirst { it.id == song.id }
                        if (origIdx != -1) _originalQueue[origIdx] = resolvedSong
                        musicService?.playSongWithQueue(resolvedSong, currentQueue)
                    }

                    _isLoadingStream.value = false
                    _isPlaying.value       = true

                    // Background cache download (only if not already cached)
                    if (cachedPath == null) {
                        launch(Dispatchers.IO) {
                            try {
                                songCacheManager.downloadToCache(
                                    url        = resolvedUrl,
                                    songId     = songId,
                                    onProgress = { p ->
                                        if (p % 25 == 0) Log.d(TAG, "📥 $p% ${song.title}")
                                    }
                                )
                                Log.d(TAG, "✅ Cached: ${song.title}")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error caching: ${e.message}")
                            }
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

    fun playStreamingSong(streamingSong: StreamingSong) {
        viewModelScope.launch {
            try {
                _isLoadingStream.value = true
                val songId = streamingSong.id
                Log.d(TAG, "🎵 Reproduciendo: ${streamingSong.title}")

                // Build full queue from all streaming songs so next/prev navigation works.
                // Each song gets a lazy "streaming://provider/id" path — resolved on demand.
                val allStreaming = _streamingSongs.value.ifEmpty { listOf(streamingSong) }
                val fullQueue = allStreaming.map { s ->
                    Song(
                        id                = s.id.hashCode().toLong(),
                        title             = s.title,
                        artist            = s.artist,
                        album             = s.album?.takeIf { it.isNotBlank() } ?: s.artist,
                        duration          = s.duration,
                        path              = "streaming://${s.provider.name.lowercase()}/${s.id}",
                        albumArtUri       = s.thumbnailUrl,
                        isStreaming       = true,
                        streamingId       = s.id,
                        streamingProvider = s.provider.name
                    )
                }

                // Store canonical unshuffled queue
                _originalQueue.clear()
                _originalQueue.addAll(fullQueue)

                // Find the tapped song's position
                val startIdx = allStreaming.indexOfFirst { it.id == streamingSong.id }
                    .coerceAtLeast(0)
                val startSongPlaceholder = fullQueue[startIdx]

                // Apply shuffle if needed
                val activeQueue = buildQueue(startSongPlaceholder, fullQueue)
                _queue.value = activeQueue
                _currentIndex.value = activeQueue.indexOfFirst {
                    it.streamingId == streamingSong.id
                }.coerceAtLeast(0)

                Log.d(TAG, "📋 Queue construida: ${activeQueue.size} canciones, idx=${_currentIndex.value}")

                // Now resolve the actual URL for the tapped song
                val cachedPath = if (songCacheManager.isCached(songId))
                    songCacheManager.getCachedPath(songId) else null

                val resolvedUrl = cachedPath ?: streamUrlCache.getStreamUrl(streamingSong)

                if (resolvedUrl != null) {
                    // Patch the placeholder in queue with the real URL
                    val resolvedSong = startSongPlaceholder.copy(path = resolvedUrl)
                    val patchedQueue = activeQueue.toMutableList()
                    patchedQueue[_currentIndex.value] = resolvedSong
                    _queue.value = patchedQueue
                    // Also patch originalQueue
                    val origIdx = _originalQueue.indexOfFirst { it.streamingId == songId }
                    if (origIdx != -1) _originalQueue[origIdx] = resolvedSong

                    musicService?.playSongWithQueue(resolvedSong, patchedQueue)
                    _isLoadingStream.value = false
                    _isPlaying.value = true
                    Log.d(TAG, "▶️ Reproduciendo: ${streamingSong.title}")

                    // Background cache + preload next songs
                    if (cachedPath == null) {
                        launch(Dispatchers.IO) {
                            try {
                                songCacheManager.downloadToCache(
                                    url        = resolvedUrl,
                                    songId     = songId,
                                    onProgress = { p ->
                                        if (p % 25 == 0) Log.d(TAG, "📥 $p% ${streamingSong.title}")
                                    }
                                )
                                Log.d(TAG, "✅ Cached: ${streamingSong.title}")
                                preloadNextStreamingSongs(streamingSong)
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error caching: ${e.message}")
                            }
                        }
                    } else {
                        launch(Dispatchers.IO) { preloadNextStreamingSongs(streamingSong) }
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

    /** Converts a StreamingSong to a Song model with the given playback path. */
    private fun StreamingSong.toSong(path: String) = Song(
        id                = id.hashCode().toLong(),
        title             = title,
        artist            = artist,
        album             = album?.takeIf { it.isNotBlank() } ?: artist,
        duration          = duration,
        path              = path,
        albumArtUri       = thumbnailUrl,
        isStreaming       = true,
        streamingId       = id,
        streamingProvider = provider.name
    )

    private fun preloadNextStreamingSongs(currentStreamingSong: StreamingSong) {
        val streamingSongs = _streamingSongs.value
        val currentIndex   = streamingSongs.indexOf(currentStreamingSong)
        if (currentIndex == -1 || streamingSongs.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (i in 1..3) {
                    val nextIdx  = (currentIndex + i) % streamingSongs.size
                    val nextSong = streamingSongs.getOrNull(nextIdx) ?: continue
                    if (songCacheManager.isCached(nextSong.id)) { Log.d(TAG, "⚡ Ya en cache: ${nextSong.title}"); continue }
                    val url = streamingRepository.getStreamUrl(nextSong)
                    if (url != null) {
                        songCacheManager.preloadChunk(url, nextSong.id, chunkSizeKB = 512)
                        if (i == 1) songCacheManager.downloadToCache(url, nextSong.id)
                    }
                }
                Log.d(TAG, "✅ Pre-carga completada")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error pre-cargando: ${e.message}")
            }
        }
    }

    // ==================== TRANSPORT ====================

    fun togglePlayPause() {
        // Ignore play/pause while a stream URL is being resolved
        if (_isLoadingStream.value) return
        if (_isPlaying.value) musicService?.pause() else musicService?.play()
    }

    fun seekTo(position: Long)  { musicService?.seekTo(position) }
    fun seekTo(position: Float) { musicService?.seekTo((position * _duration.value).toLong()) }

    // ==================== NAVIGATION ====================

    /**
     * Skip to next track.
     *
     * | Repeat | Shuffle | Behaviour                                    |
     * |--------|---------|----------------------------------------------|
     * | ONE    | any     | Restart current song                         |
     * | ALL    | off     | Next in order, wrap around                   |
     * | ALL    | on      | Next in shuffled queue, wrap around          |
     * | OFF    | off     | Next in order, stop at end                   |
     * | OFF    | on      | Next in shuffled queue, stop at end          |
     */
    fun skipToNext() {
        // If a stream is currently loading, cancel it and proceed with the skip
        // (do NOT just return — the user or auto-completion needs to move forward)
        if (_isLoadingStream.value) {
            _isLoadingStream.value = false
        }
        val queue = _queue.value
        if (queue.isEmpty()) { Log.w(TAG, "⚠️ Cola vacía"); return }

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Repeat-one: restart current regardless of shuffle
                playCurrentSong()
            }
            RepeatMode.ALL -> {
                _currentIndex.value = (_currentIndex.value + 1) % queue.size
                playCurrentSong()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value < queue.size - 1) {
                    _currentIndex.value += 1
                    playCurrentSong()
                } else {
                    // End of queue — stop
                    musicService?.pause()
                    _isPlaying.value = false
                    Log.d(TAG, "🛑 Fin de cola (skipNext)")
                }
            }
        }
    }

    /**
     * Skip to previous track.
     * Same repeat/shuffle matrix as skipToNext but in reverse.
     * If position > 3 s, restart current song instead of going back.
     */
    fun skipToPrevious() {
        // If a stream is currently loading, cancel it and proceed with the skip
        if (_isLoadingStream.value) {
            _isLoadingStream.value = false
        }
        val queue = _queue.value
        if (queue.isEmpty()) { Log.w(TAG, "⚠️ Cola vacía"); return }

        // If we're more than 3 seconds in, restart current song
        if (_currentPosition.value > 3_000L && _repeatMode.value != RepeatMode.ONE) {
            musicService?.seekTo(0L)
            _currentPosition.value = 0L
            return
        }

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                playCurrentSong()
            }
            RepeatMode.ALL -> {
                _currentIndex.value = if (_currentIndex.value == 0) queue.size - 1
                else _currentIndex.value - 1
                playCurrentSong()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value > 0) {
                    _currentIndex.value -= 1
                    playCurrentSong()
                } else {
                    // Already at first song — just restart it
                    musicService?.seekTo(0L)
                    _currentPosition.value = 0L
                }
            }
        }
    }

    // ==================== SHUFFLE ====================

    /**
     * Toggle shuffle.
     * When turned ON:  re-order _queue keeping current song first.
     * When turned OFF: restore _originalQueue, find current song by id.
     * RepeatMode.ONE is visually acknowledged (button turns blue) but queue
     * order doesn't matter because only one song plays.
     */
    fun toggleShuffle() {
        val newState    = !_shuffleEnabled.value
        _shuffleEnabled.value = newState
        musicService?.setShuffle(newState)
        Log.d(TAG, "🔀 Shuffle: $newState")

        val currentSong = _currentSong.value ?: return
        if (_originalQueue.isEmpty()) return

        val newQueue = if (newState) {
            // Shuffle: current song stays first
            val rest = _originalQueue.toMutableList().also { it.remove(currentSong) }
            rest.shuffle()
            listOf(currentSong) + rest
        } else {
            // Restore original order
            _originalQueue.toList()
        }

        _queue.value     = newQueue
        val newIndex     = newQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
        _currentIndex.value = newIndex
        musicService?.updateQueue(newQueue, newIndex)
    }

    // ==================== REPEAT ====================

    /**
     * Cycle: OFF → ALL → ONE → OFF
     *
     * When switching to ALL with shuffle on, re-shuffle so the queue
     * is randomised from current position again.
     */
    fun cycleRepeatMode() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = newMode
        musicService?.setRepeat(newMode)
        Log.d(TAG, "🔁 Repeat: $newMode")

        // Re-apply shuffle to queue when leaving RepeatMode.ONE with shuffle on
        if (newMode != RepeatMode.ONE && _shuffleEnabled.value && _originalQueue.isNotEmpty()) {
            val currentSong = _currentSong.value ?: return
            val rest = _originalQueue.toMutableList().also { it.remove(currentSong) }
            rest.shuffle()
            val newQueue = listOf(currentSong) + rest
            _queue.value     = newQueue
            _currentIndex.value = 0
            musicService?.updateQueue(newQueue, 0)
        }
    }

    // ==================== INTERNAL PLAYBACK ====================

    /**
     * Play the song at _currentIndex in _queue.
     * Handles both local and streaming songs transparently.
     */
    private fun playCurrentSong() {
        val queue = _queue.value
        if (queue.isEmpty()) return

        // Clamp index
        if (_currentIndex.value !in queue.indices) _currentIndex.value = 0

        val song = queue[_currentIndex.value]
        Log.d(TAG, "🎵 playCurrentSong: [${ _currentIndex.value}/${queue.size}] ${song.title}")

        if (song.isStreaming && song.path.startsWith("streaming://")) {
            // Resolve stream URL before passing to service
            playStreamingSongFromPath(song, queue, rebuildQueue = false)
        } else {
            _currentSong.value = song
            musicService?.playSongWithQueue(song, queue)
            _isPlaying.value = true
        }
    }

    // ==================== STREAMING MODE ====================

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
        if (query.isBlank()) clearSearchResults()
        else if (_isOnlineMode.value) searchStreamingSongs(query)
        else searchLocalSongs(query)
    }

    private fun clearSearchResults() {
        searchJob?.cancel()
        _streamingSongs.value    = emptyList()
        _localSearchResults.value = emptyList()
        _isSearching.value       = false
    }

    fun clearSearchCompletely() {
        _searchQuery.value = ""
        clearSearchResults()
    }

    fun searchLocalSongs(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) { _localSearchResults.value = emptyList(); _isSearching.value = false; return }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)
            try {
                fun normalize(text: String) = text.trim().lowercase()
                    .replace(Regex("\\s+"), " ").replace(Regex("[^a-z0-9\\s]"), "")

                val q = normalize(query)
                _localSearchResults.value = _songs.value.filter { song ->
                    normalize(song.title).contains(q) ||
                            normalize(song.artist).contains(q) ||
                            normalize(song.album).contains(q)
                }
                Log.d(TAG, "🔍 Local '$query' → ${_localSearchResults.value.size} resultados")
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
        if (query.isBlank()) { _streamingSongs.value = emptyList(); _isSearching.value = false; return }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(800)
            try {
                val results = streamingRepository.search(query, limit = 30)
                _streamingSongs.value = results
                results.forEach { streamingSongCache[it.id] = it }
                Log.d(TAG, "✅ ${results.size} resultados streaming")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error búsqueda streaming: ${e.message}", e)
                _streamingSongs.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() = clearSearchCompletely()

    // ==================== CACHE ====================

    fun cacheStreamingSong(streamingSong: StreamingSong) {
        streamingSongCache[streamingSong.id] = streamingSong
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) { songCacheManager.clearAll() }
    }

    fun getCacheInfo(): Pair<Int, Long> =
        Pair(songCacheManager.getCachedSongsCount(), songCacheManager.getCacheSize())

    // ==================== MIXED PLAYLIST ====================

    fun playMixedPlaylist(
        song: Song,
        playlist: List<Song>,
        streamingSongs: List<StreamingSong>,
        playlistName: String? = null
    ) {
        viewModelScope.launch {
            try {
                streamingSongs.forEach { streamingSongCache[it.id] = it }

                val playlistWithAlbum = if (!playlistName.isNullOrBlank())
                    playlist.map { it.copy(album = playlistName) }
                else playlist

                _originalQueue.clear()
                _originalQueue.addAll(playlistWithAlbum)
                _queue.value = playlistWithAlbum

                val startIndex = playlistWithAlbum.indexOfFirst { it.id == song.id }
                if (startIndex != -1) _currentIndex.value = startIndex

                val songWithAlbum = if (!playlistName.isNullOrBlank()) song.copy(album = playlistName) else song

                if (songWithAlbum.isStreaming && songWithAlbum.path.startsWith("streaming://"))
                    playStreamingSongFromPath(songWithAlbum, playlistWithAlbum)
                else
                    playSong(songWithAlbum, playlistWithAlbum)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo playlist mixta", e)
            }
        }
    }

    // ==================== FAVORITES ====================

    fun toggleFavoriteBySongId(songId: Long) {
        val key      = songId.toString()
        val current  = _favorites.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _favorites.value = current
    }

    fun toggleFavorite(songId: Long) = toggleFavoriteBySongId(songId)

    // ==================== LIFECYCLE ====================

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        streamUrlCache.cleanup()
        try { getApplication<Application>().unregisterReceiver(notificationReceiver) }
        catch (e: Exception) { Log.e(TAG, "Error unregistering receiver", e) }
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
        Log.d(TAG, "🧹 ViewModel limpiado")
    }
}            