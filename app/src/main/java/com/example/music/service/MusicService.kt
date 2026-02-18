package com.example.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.music.MainActivity
import com.example.music.R
import com.example.music.data.model.RepeatMode
import com.example.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class MusicService : Service() {

    private val TAG = "MusicService"
    private var exoPlayer: ExoPlayer? = null
    private val binder = MusicBinder()

    private lateinit var simpleCache: SimpleCache

    // ==================== STATE ====================

    private val _currentSong    = MutableStateFlow<Song?>(null)
    private val _isPlaying      = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration       = MutableStateFlow(0L)
    private val _repeatMode     = MutableStateFlow(RepeatMode.OFF)
    private val _shuffleEnabled = MutableStateFlow(false)
    private val _isBuffering    = MutableStateFlow(false)

    // Queue state is owned here so auto-completion works correctly
    private val _queue    = MutableStateFlow<List<Song>>(emptyList())
    private var currentIndex  = 0
    private var originalQueue = listOf<Song>()   // kept for reference if needed

    private val handler = Handler(Looper.getMainLooper())
    private var lastNotificationUpdate = 0L

    private val updatePositionRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition
                        _duration.value        = player.duration.coerceAtLeast(0)

                        val now = System.currentTimeMillis()
                        if (now - lastNotificationUpdate > 1000) {
                            _currentSong.value?.let { updateNotification(it, true) }
                            lastNotificationUpdate = now
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error actualizando posición: ${e.message}")
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    // ==================== BINDER ====================

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun getCurrentSong(): StateFlow<Song?>    = _currentSong.asStateFlow()
    fun getIsPlaying(): StateFlow<Boolean>    = _isPlaying.asStateFlow()
    fun getCurrentPosition(): StateFlow<Long> = _currentPosition.asStateFlow()
    fun getDuration(): StateFlow<Long>        = _duration.asStateFlow()
    fun getRepeatMode(): StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    fun getShuffleEnabled(): StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    fun getIsBuffering(): StateFlow<Boolean>  = _isBuffering.asStateFlow()

    // ==================== LIFECYCLE ====================

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeExoPlayer()
        Log.d(TAG, "✅ MusicService creado")
    }

    @OptIn(UnstableApi::class)
    private fun initializeExoPlayer() {
        val cacheDir = File(cacheDir, "exoplayer-cache")
        simpleCache  = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(100L * 1024 * 1024),
            androidx.media3.database.StandaloneDatabaseProvider(this)
        )

        val upstreamFactory = DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(3000)
                .setReadTimeoutMs(3000)
                .setAllowCrossProtocolRedirects(true)
        )

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(500, 5000, 200, 500)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setDataSourceFactory(cacheDataSourceFactory)
            )
            .build()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> { _isBuffering.value = true;  Log.d(TAG, "⏳ Buffering") }
                    Player.STATE_READY     -> { _isBuffering.value = false; _duration.value = exoPlayer?.duration ?: 0L; Log.d(TAG, "✅ Ready") }
                    Player.STATE_ENDED     -> { Log.d(TAG, "🎵 Ended"); handleAutoCompletion() }
                    Player.STATE_IDLE      -> Log.d(TAG, "💤 Idle")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) handler.post(updatePositionRunnable)
                else           handler.removeCallbacks(updatePositionRunnable)
                _currentSong.value?.let { updateNotification(it, isPlaying) }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "❌ ExoPlayer error: ${error.message}", error)
                _isPlaying.value  = false
                _isBuffering.value = false
                _currentSong.value?.let { updateNotification(it, false) }
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                playPause()
                handler.postDelayed({
                    _currentSong.value?.let { updateNotification(it, _isPlaying.value) }
                }, 100)
            }
            ACTION_NEXT     -> sendBroadcast(Intent("com.example.music.ACTION_SKIP_NEXT"))
            ACTION_PREVIOUS -> sendBroadcast(Intent("com.example.music.ACTION_SKIP_PREVIOUS"))
            ACTION_SHUFFLE  -> sendBroadcast(Intent("com.example.music.ACTION_TOGGLE_SHUFFLE"))
            ACTION_REPEAT   -> sendBroadcast(Intent("com.example.music.ACTION_CYCLE_REPEAT"))
            ACTION_DISMISS  -> dismissNotificationAndStop()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Controls for music playback"; setShowBadge(false) }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    // ==================== PLAYBACK ====================

    fun playSongWithQueue(song: Song, queue: List<Song>) {
        Log.d(TAG, "🎵 playSongWithQueue: ${song.title}, queue: ${queue.size}")
        originalQueue = queue
        _queue.value  = queue
        currentIndex  = queue.indexOf(song).coerceAtLeast(0)
        playSongInternal(song)
    }

    fun play() {
        exoPlayer?.play()
        _isPlaying.value = true
        handler.post(updatePositionRunnable)
        _currentSong.value?.let { updateNotification(it, true) }
    }

    fun pause() {
        exoPlayer?.pause()
        _isPlaying.value = false
        handler.removeCallbacks(updatePositionRunnable)
        stopForeground(STOP_FOREGROUND_DETACH)
        _currentSong.value?.let { updateNotification(it, false) }
    }

    fun playPause() { if (_isPlaying.value) pause() else play() }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    private fun playSongInternal(song: Song) {
        try {
            Log.d(TAG, "▶️ playSongInternal: ${song.title} | path: ${song.path}")
            _currentSong.value = song

            val uri = when {
                song.path.startsWith("http")       -> Uri.parse(song.path)
                song.path.startsWith("content://") -> Uri.parse(song.path)
                song.path.startsWith("/")          -> Uri.parse("file://${song.path}")
                else                               -> Uri.parse(song.path)
            }

            exoPlayer?.apply {
                setMediaItem(MediaItem.Builder().setUri(uri).build())
                prepare()
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing song", e)
            _isPlaying.value  = false
            _isBuffering.value = false
        }
    }

    // ==================== AUTO-COMPLETION ====================

    /**
     * Called by ExoPlayer when STATE_ENDED fires.
     *
     * | Repeat | Shuffle | Result                                  |
     * |--------|---------|-----------------------------------------|
     * | ONE    | any     | Restart current song                    |
     * | ALL    | off/on  | Advance index (wraps), play next        |
     * | OFF    | off/on  | Advance index, stop if at end           |
     */
    private fun handleAutoCompletion() {
        val queue = _queue.value
        if (queue.isEmpty()) return

        Log.d(TAG, "🔚 handleAutoCompletion: repeat=${_repeatMode.value}, idx=$currentIndex/${queue.size}")

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                val song = queue[currentIndex]
                if (song.isStreaming && song.path.startsWith("streaming://")) {
                    // Delegate to ViewModel — it knows how to resolve streaming URLs
                    sendBroadcast(Intent("com.example.music.ACTION_SKIP_NEXT"))
                } else {
                    playSongInternal(song)
                }
                Log.d(TAG, "🔂 Repeat ONE → ${song.title}")
            }
            RepeatMode.ALL -> {
                val nextIndex = (currentIndex + 1) % queue.size
                val song = queue[nextIndex]
                if (song.isStreaming && song.path.startsWith("streaming://")) {
                    // Tell ViewModel to handle the skip — it will resolve the URL
                    sendBroadcast(Intent("com.example.music.ACTION_SKIP_NEXT"))
                } else {
                    currentIndex = nextIndex
                    playSongInternal(song)
                }
                Log.d(TAG, "🔁 Repeat ALL → [$nextIndex] ${song.title}")
            }
            RepeatMode.OFF -> {
                if (currentIndex < queue.size - 1) {
                    val nextIndex = currentIndex + 1
                    val song = queue[nextIndex]
                    if (song.isStreaming && song.path.startsWith("streaming://")) {
                        sendBroadcast(Intent("com.example.music.ACTION_SKIP_NEXT"))
                    } else {
                        currentIndex = nextIndex
                        playSongInternal(song)
                    }
                    Log.d(TAG, "▶️ Next → [$nextIndex] ${song.title}")
                } else {
                    Log.d(TAG, "🛑 End of queue")
                    pause()
                    seekTo(0L)
                }
            }
        }
    }

    // ==================== SHUFFLE & REPEAT ====================

    fun setShuffle(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        Log.d(TAG, "🔀 setShuffle: $enabled")
        // Refresh notification icons only — do NOT touch playback state
        _currentSong.value?.let { updateNotificationIconsOnly(it) }
    }

    /**
     * Called by ViewModel when the active queue changes (toggle shuffle, cycle repeat, etc.)
     */
    fun updateQueue(newQueue: List<Song>, newIndex: Int) {
        _queue.value = newQueue
        currentIndex = newIndex
        Log.d(TAG, "📝 Queue updated: ${newQueue.size} songs, index: $newIndex")
        // No notification update needed here — icons already refreshed by setShuffle/setRepeat
    }

    /**
     * Refreshes ONLY the shuffle/repeat icons in the notification.
     * Unlike updateNotification(), this NEVER calls stopForeground() or changes
     * the ongoing/autoCancel flags — so it cannot accidentally pause or dismiss
     * the player while a stream is loading or playing.
     */
    private fun updateNotificationIconsOnly(song: Song) {
        val isPlaying = _isPlaying.value || _isBuffering.value
        updateNotification(song, isPlaying)
    }

    fun setRepeat(mode: RepeatMode) {
        _repeatMode.value = mode
        Log.d(TAG, "🔁 setRepeat: $mode")
        // Refresh notification icons only — do NOT touch playback state
        _currentSong.value?.let { updateNotificationIconsOnly(it) }
    }

    // ==================== NOTIFICATION ====================

    private fun dismissNotificationAndStop() {
        try { if (exoPlayer?.isPlaying == true) exoPlayer?.pause() } catch (_: Exception) {}
        _isPlaying.value = false
        handler.removeCallbacks(updatePositionRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private suspend fun loadAlbumArtBitmap(albumArtUri: String?): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (albumArtUri.isNullOrEmpty()) return@withContext null
                if (albumArtUri.startsWith("http")) {
                    val conn = URL(albumArtUri).openConnection()
                        .apply { connectTimeout = 5000; readTimeout = 5000; connect() }
                    BitmapFactory.decodeStream(conn.getInputStream())
                } else {
                    contentResolver.openInputStream(Uri.parse(albumArtUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            } catch (_: Exception) { null }
        }
    }

    private fun createRoundedBitmapWithBorder(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint  = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE }
        val rect   = Rect(0, 0, width, height)
        val rectF  = RectF(rect)

        canvas.drawRoundRect(rectF, 24f, 24f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, width, height, true), rect, rect, paint)

        canvas.drawRoundRect(rectF, 24f, 24f, Paint().apply {
            isAntiAlias = true
            style       = Paint.Style.STROKE
            strokeWidth = 4f
            color       = android.graphics.Color.parseColor("#4DFFFFFF")
        })
        return output
    }

    private fun formatTime(millis: Long): String {
        val s = millis / 1000
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    private fun updateNotification(song: Song, isPlaying: Boolean) {
        // ── PendingIntents ────────────────────────────────────────────────
        fun servicePI(requestCode: Int, action: String) = PendingIntent.getService(
            this, requestCode,
            Intent(this, MusicService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPausePI = servicePI(100, ACTION_PLAY_PAUSE)
        val nextPI      = servicePI(101, ACTION_NEXT)
        val previousPI  = servicePI(102, ACTION_PREVIOUS)
        val shufflePI   = servicePI(103, ACTION_SHUFFLE)
        val repeatPI    = servicePI(104, ACTION_REPEAT)

        // ── Icons ─────────────────────────────────────────────────────────
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause_dark else R.drawable.ic_play_dark
        val playPauseIconBig = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val shuffleIcon = if (_shuffleEnabled.value) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        val repeatIcon  = when (_repeatMode.value) {
            RepeatMode.OFF -> R.drawable.ic_repeat
            RepeatMode.ALL -> R.drawable.ic_repeat_all
            RepeatMode.ONE -> R.drawable.ic_repeat_one
        }

        // ── Collapsed view ────────────────────────────────────────────────
        val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed).apply {
            setTextViewText(R.id.tv_song_title, song.title)
            setImageViewResource(R.id.btn_play_pause, playPauseIcon)
            setOnClickPendingIntent(R.id.btn_play_pause, playPausePI)
            setOnClickPendingIntent(R.id.btn_next_big,     nextPI)
            setOnClickPendingIntent(R.id.btn_previous_big, previousPI)
        }

        // ── Expanded view ─────────────────────────────────────────────────
        val progress = if (_duration.value > 0)
            (_currentPosition.value * 100 / _duration.value).toInt() else 0

        val expandedView = RemoteViews(packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.tv_song_title_big, song.title)
            setTextViewText(R.id.tv_artist_big, song.artist)
            setProgressBar(R.id.progress_bar, 100, progress, false)
            setTextViewText(R.id.tv_current_time, formatTime(_currentPosition.value))
            setTextViewText(R.id.tv_total_time,   formatTime(_duration.value))
            setImageViewResource(R.id.btn_play_pause_big, playPauseIconBig)
            setImageViewResource(R.id.btn_shuffle, shuffleIcon)
            setImageViewResource(R.id.btn_repeat,  repeatIcon)
            setOnClickPendingIntent(R.id.btn_previous_big,  previousPI)
            setOnClickPendingIntent(R.id.btn_play_pause_big, playPausePI)
            setOnClickPendingIntent(R.id.btn_next_big,      nextPI)
            setOnClickPendingIntent(R.id.btn_shuffle,       shufflePI)
            setOnClickPendingIntent(R.id.btn_repeat,        repeatPI)
            setOnClickPendingIntent(R.id.btn_collapse,      openPendingIntent)
        }

        // ── Build & post notification with album art ──────────────────────
        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = loadAlbumArtBitmap(song.albumArtUri)
            withContext(Dispatchers.Main) {
                val rounded = bitmap?.let { createRoundedBitmapWithBorder(it, 200, 200) }
                if (bitmap != null && rounded != null) {
                    expandedView.setImageViewBitmap(R.id.iv_background,    bitmap)
                    expandedView.setImageViewBitmap(R.id.iv_album_art_big, rounded)
                    collapsedView.setImageViewBitmap(R.id.iv_album_art,    rounded)
                } else {
                    expandedView.setImageViewResource(R.id.iv_background,    R.drawable.ic_music_note)
                    expandedView.setImageViewResource(R.id.iv_album_art_big, R.drawable.ic_music_note)
                    collapsedView.setImageViewResource(R.id.iv_album_art,    R.drawable.ic_music_note)
                }

                val builder = NotificationCompat.Builder(this@MusicService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle(song.title)
                    .setContentText(song.artist)
                    .setSubText("Music Player")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(openPendingIntent)
                    .setSilent(true)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(expandedView)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                if (isPlaying) { // isPlaying = _isPlaying.value || _isBuffering.value
                    // Playing OR buffering a new stream → keep foreground, keep ongoing
                    // This prevents any button press during stream loading from killing audio
                    builder.setOngoing(true).setAutoCancel(false)
                    startForeground(NOTIFICATION_ID, builder.build())
                } else {
                    // Genuinely paused by user → detach foreground, allow swipe-dismiss
                    builder.setOngoing(false).setAutoCancel(true)
                    builder.setDeleteIntent(servicePI(105, ACTION_DISMISS))
                    stopForeground(STOP_FOREGROUND_DETACH)
                    getSystemService(NotificationManager::class.java)
                        ?.notify(NOTIFICATION_ID, builder.build())
                }
            }
        }
    }

    // ==================== DESTROY ====================

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        handler.removeCallbacks(updatePositionRunnable)
        exoPlayer?.release(); exoPlayer = null
        try { simpleCache.release() } catch (_: Exception) {}
        super.onDestroy()
        Log.d(TAG, "🧹 MusicService destruido")
    }

    // ==================== CONSTANTS ====================

    companion object {
        const val CHANNEL_ID       = "music_playback_channel"
        const val NOTIFICATION_ID  = 1
        const val ACTION_PLAY_PAUSE = "com.example.music.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT       = "com.example.music.ACTION_NEXT"
        const val ACTION_PREVIOUS   = "com.example.music.ACTION_PREVIOUS"
        const val ACTION_SHUFFLE    = "com.example.music.ACTION_SHUFFLE"
        const val ACTION_REPEAT     = "com.example.music.ACTION_REPEAT"
        const val ACTION_DISMISS    = "com.example.music.ACTION_DISMISS"
    }
}