package dev.abbasian.exoboost.data.manager

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.domain.model.MediaInfo
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.model.MediaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException

@UnstableApi
class ExoPlayerManager(
    private val context: Context,
    private val dataSourceFactory: DataSource.Factory,
    private val networkManager: NetworkManager
) {
    private val _mediaState = MutableStateFlow<MediaState>(MediaState.Idle)
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private val _mediaInfo = MutableStateFlow(MediaInfo())
    val mediaInfo: StateFlow<MediaInfo> = _mediaInfo.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var currentConfig: MediaPlayerConfig = MediaPlayerConfig()
    private var currentUrl: String = ""
    private var retryCount = 0
    private val isInitialized = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)
    private var isPrepared = AtomicBoolean(false)
    private var isMediaReady = AtomicBoolean(false)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isReleased.get() && exoPlayer != null) {
                updateMediaInfo()
                mainHandler.postDelayed(this, 500)
            }
        }
    }

    fun initializePlayer(config: MediaPlayerConfig) {
        if (isInitialized.get() && !isReleased.get()) {
            Log.d("ExoPlayerManager", "Player already initialized")
            return
        }

        try {
            isReleased.set(false)
            isPrepared.set(false)
            isMediaReady.set(false)
            currentConfig = config
            Log.d("ExoPlayerManager", "Initializing ExoPlayer...")

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    config.bufferDurations.minBufferMs,
                    config.bufferDurations.maxBufferMs,
                    config.bufferDurations.bufferForPlaybackMs,
                    config.bufferDurations.bufferForPlaybackAfterRebufferMs
                )
                .build()

            val trackSelector = setupTrackSelector()

            exoPlayer?.let { player ->
                player.removeListener(playerListener)
                player.removeAnalyticsListener(analyticsListener)
                player.release()
            }

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    addListener(playerListener)
                    addAnalyticsListener(analyticsListener)
                    repeatMode = Player.REPEAT_MODE_OFF
                    setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                    playWhenReady = false
                }

            isInitialized.set(true)
            startPositionUpdates()

            Log.d("ExoPlayerManager", "ExoPlayer initialized successfully")

        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Failed to initialize player", e)
            _mediaState.value = MediaState.Error(
                PlayerError.UnknownError(
                    context.getString(R.string.error_init_player) + e.message,
                    e
                )
            )
            isInitialized.set(false)
        }
    }

    fun loadMedia(url: String) {
        if (isReleased.get()) {
            Log.w("ExoPlayerManager", "Cannot load video: player is released")
            return
        }

        if (!isInitialized.get()) {
            Log.w("ExoPlayerManager", "Cannot load video: player not initialized")
            return
        }

        if (currentUrl == url && isPrepared.get()) {
            Log.d("ExoPlayerManager", "Video already loaded: $url")
            return
        }

        currentUrl = url
        retryCount = 0
        isPrepared.set(false)
        isMediaReady.set(false)

        if (!networkManager.isNetworkAvailable()) {
            _mediaState.value = MediaState.Error(
                PlayerError.NetworkError("اتصال اینترنت موجود نیست")
            )
            return
        }

        try {
            _mediaState.value = MediaState.Loading
            Log.d("ExoPlayerManager", "Loading video: $url")

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .build()

            exoPlayer?.apply {
                stop()
                clearMediaItems()
                setMediaItem(mediaItem)
                prepare()
            }

            Log.d("ExoPlayerManager", "Video preparation started")

        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error loading video", e)
            handleLoadError(e)
        }
    }

    fun isReadyForSurface(): Boolean {
        return isInitialized.get() && isPrepared.get() && !isReleased.get()
    }

    fun onSurfaceAvailable() {
        Log.d("ExoPlayerManager", "Surface available")
        if (isReadyForSurface() && currentConfig.autoPlay) {
            mainHandler.postDelayed({
                if (!isReleased.get() && exoPlayer != null) {
                    exoPlayer?.playWhenReady = true
                }
            }, 100)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (isReleased.get()) return

            val stateString = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }

            Log.d("ExoPlayerManager", "Playback state changed: $stateString")

            when (playbackState) {
                Player.STATE_IDLE -> {
                    isPrepared.set(false)
                    isMediaReady.set(false)
                    _mediaState.value = MediaState.Idle
                }
                Player.STATE_BUFFERING -> {
                    _mediaState.value = MediaState.Loading
                }
                Player.STATE_READY -> {
                    retryCount = 0
                    isPrepared.set(true)

                    val hasVideo = exoPlayer?.videoFormat != null
                    if (hasVideo && !isMediaReady.get()) {
                        _mediaState.value = MediaState.Loading
                    } else {
                        isMediaReady.set(true)
                        _mediaState.value = MediaState.Ready
                    }
                }
                Player.STATE_ENDED -> {
                    _mediaState.value = MediaState.Ended
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isReleased.get()) return

            Log.d("ExoPlayerManager", "Is playing changed: $isPlaying")

            _mediaState.value = if (isPlaying) {
                MediaState.Playing
            } else {
                when (exoPlayer?.playbackState) {
                    Player.STATE_ENDED -> MediaState.Ended
                    Player.STATE_READY -> MediaState.Paused
                    else -> MediaState.Paused
                }
            }
            updateMediaInfo()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (isReleased.get()) return
            Log.d("ExoPlayerManager", "Video size changed: ${videoSize.width}x${videoSize.height}")
        }

        override fun onRenderedFirstFrame() {
            if (isReleased.get()) return
            Log.d("ExoPlayerManager", "First frame rendered")

            isMediaReady.set(true)

            if (_mediaState.value is MediaState.Loading && isPrepared.get()) {
                _mediaState.value = MediaState.Ready
            }

            if (currentConfig.autoPlay && exoPlayer?.playWhenReady == false && isPrepared.get()) {
                mainHandler.postDelayed({
                    if (!isReleased.get()) {
                        exoPlayer?.playWhenReady = true
                    }
                }, 50)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (isReleased.get()) return

            var logMessage = "Player error: ${error.errorCodeName} (${error.errorCode}) - ${error.message}"

            val cause = error.cause
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                logMessage += "\n HTTP Status Code: ${cause.responseCode}"

            } else if (cause is IOException) {
                logMessage += "\n IO Cause: ${cause.javaClass.simpleName} - ${cause.message}"
            } else if (cause != null) {
                logMessage += "\n Cause: ${cause.javaClass.simpleName} - ${cause.message}"
            }

            Log.e(
                "ExoPlayerManager",
                logMessage,
                error
            )

            isPrepared.set(false)
            isMediaReady.set(false)

            val playerError = mapPlaybackException(error)
            _mediaState.value = MediaState.Error(playerError)

            if (currentConfig.retryOnError && isRetryableError(playerError) &&
                retryCount < currentConfig.maxRetryCount
            ) {
                mainHandler.postDelayed({
                    if (!isReleased.get()) {
                        retry()
                    }
                }, 2000)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isReleased.get()) return
            Log.d("ExoPlayerManager", "Media item transition")
            updateMediaInfo()
        }
    }

    fun play() {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            exoPlayer?.playWhenReady = true
            Log.d("ExoPlayerManager", "Play requested")
        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error playing", e)
        }
    }

    fun pause() {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            exoPlayer?.playWhenReady = false
            exoPlayer?.pause()
            Log.d("ExoPlayerManager", "Pause requested")
        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error pausing", e)
        }
    }

    fun seekTo(position: Long) {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            exoPlayer?.seekTo(position.coerceAtLeast(0L))
            Log.d("ExoPlayerManager", "Seek to: $position")
        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error seeking", e)
        }
    }

    fun setVolume(volume: Float) {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            val clampedVolume = volume.coerceIn(0f, 1f)
            exoPlayer?.volume = clampedVolume
            updateMediaInfo()
            Log.d("ExoPlayerManager", "Volume set to: $clampedVolume")
        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error setting volume", e)
        }
    }

    fun retry() {
        if (isReleased.get() || !isInitialized.get()) {
            Log.w("ExoPlayerManager", "Cannot retry: player not available")
            return
        }

        if (retryCount >= currentConfig.maxRetryCount) {
            _mediaState.value = MediaState.Error(
                PlayerError.UnknownError(context.getString(R.string.error_retry_limit))
            )
            return
        }

        retryCount++
        isPrepared.set(false)
        isMediaReady.set(false)

        if (!networkManager.isNetworkAvailable()) {
            _mediaState.value = MediaState.Error(
                PlayerError.NetworkError(context.getString(R.string.help_check_network))
            )
            return
        }

        try {
            _mediaState.value = MediaState.Loading
            Log.d("ExoPlayerManager", "Retrying... attempt $retryCount")

            exoPlayer?.let { player ->
                player.stop()
                player.prepare()
                player.playWhenReady = false
            }
        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error during retry", e)
            handleLoadError(e)
        }
    }

    fun release() {
        if (isReleased.getAndSet(true)) {
            Log.d("ExoPlayerManager", "Player already released")
            return
        }

        Log.d("ExoPlayerManager", "Releasing ExoPlayer")

        try {
            stopPositionUpdates()

            exoPlayer?.let { player ->
                player.stop()
                player.removeListener(playerListener)
                player.removeAnalyticsListener(analyticsListener)
                player.release()
            }

            exoPlayer = null
            isInitialized.set(false)
            isPrepared.set(false)
            isMediaReady.set(false)
            currentUrl = ""
            retryCount = 0
            _mediaState.value = MediaState.Idle

            Log.d("ExoPlayerManager", "ExoPlayer released successfully")

        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error during release", e)
        }
    }

    fun getPlayer(): ExoPlayer? = if (isReleased.get()) null else exoPlayer

    private fun startPositionUpdates() {
        if (!isReleased.get()) {
            mainHandler.post(positionUpdateRunnable)
        }
    }

    private fun stopPositionUpdates() {
        mainHandler.removeCallbacks(positionUpdateRunnable)
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onLoadError(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData,
            error: IOException,
            wasCanceled: Boolean
        ) {
            if (!wasCanceled) {
                handleLoadError(error)
            }
        }
    }

    private fun handleLoadError(error: Throwable) {
        val playerError = when (error) {
            is SSLException -> PlayerError.SSLError(
                context.getString(R.string.error_security_certificate_with_message) + error.message,
                error.message,
                error
            )

            is SocketTimeoutException -> PlayerError.NetworkError(
                context.getString(R.string.error_timeout),
                true,
                error
            )

            is IOException -> PlayerError.NetworkError(
                context.getString(R.string.error_network) + error.message,
                true,
                error
            )

            else -> PlayerError.UnknownError(
                context.getString(R.string.error_network) + error.message,
                error
            )
        }

        _mediaState.value = MediaState.Error(playerError)
        Log.e("ExoPlayerManager", "Load error: ${playerError.message}", error)
    }

    private fun mapPlaybackException(error: PlaybackException): PlayerError {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                PlayerError.NetworkError(
                    context.getString(R.string.error_network),
                    true,
                    error
                )

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                PlayerError.NetworkError(
                    context.getString(R.string.error_timeout),
                    true,
                    error
                )

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                PlayerError.LiveStreamError(
                    context.getString(R.string.error_http) + extractHttpCode(error),
                    extractHttpCode(error),
                    error
                )

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                PlayerError.SourceError(
                    context.getString(R.string.error_format_not_supported),
                    currentUrl,
                    error
                )

            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                PlayerError.CodecError(
                    context.getString(R.string.error_decoding),
                    null,
                    error
                )

            else -> {
                if (error.cause is SSLException) {
                    PlayerError.SSLError(
                        context.getString(R.string.error_ssl_certificate_with_message) + error.message,
                        error.cause?.message,
                        error
                    )
                } else {
                    PlayerError.UnknownError(
                        context.getString(R.string.error_unknown) + error.message,
                        error
                    )
                }
            }
        }
    }

    private fun extractHttpCode(error: PlaybackException): Int? {
        return error.message?.let { message ->
            Regex("HTTP (\\d{3})").find(message)?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    private fun isRetryableError(error: PlayerError): Boolean {
        return when (error) {
            is PlayerError.NetworkError -> error.isRetryable
            is PlayerError.LiveStreamError -> error.httpCode in listOf(403, 404, 500, 502, 503, 504)
            is PlayerError.SSLError -> false
            else -> false
        }
    }

    private fun updateMediaInfo() {
        exoPlayer?.let { player ->
            val availableQualities = getAvailableQualities()
            val currentQuality = getCurrentSelectedQuality(availableQualities)

            _mediaInfo.value = MediaInfo(
                currentPosition = player.currentPosition.coerceAtLeast(0L),
                duration = if (player.duration != C.TIME_UNSET) player.duration else 0L,
                volume = player.volume,
                isPlaying = player.isPlaying,
                bufferedPosition = player.bufferedPosition.coerceAtLeast(0L),
                playbackSpeed = player.playbackParameters.speed,
                availableQualities = availableQualities,
                currentQuality = currentQuality
            )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            val clampedSpeed = speed.coerceIn(0.25f, 3.0f)
            exoPlayer?.setPlaybackSpeed(clampedSpeed)
            Log.d("ExoPlayerManager", "Playback speed set to: $clampedSpeed")
            updateMediaInfo()
        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error setting playback speed", e)
        }
    }

    fun getAvailableQualities(): List<VideoQuality> {
        if (isReleased.get() || !isInitialized.get()) return emptyList()

        return try {
            val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector
            val trackSelectionParameters = trackSelector?.parameters
            val mappedTrackInfo = trackSelector?.currentMappedTrackInfo

            mappedTrackInfo?.let { trackInfo ->
                val qualities = mutableListOf<VideoQuality>()

                qualities.add(VideoQuality(
                    trackGroup = -1,
                    trackIndex = -1,
                    width = 0,
                    height = 0,
                    bitrate = 0,
                    label = "Auto",
                    isSelected = trackSelectionParameters?.maxVideoWidth == Int.MAX_VALUE
                ))

                for (rendererIndex in 0 until trackInfo.rendererCount) {
                    if (trackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_VIDEO) {
                        val trackGroups = trackInfo.getTrackGroups(rendererIndex)

                        for (groupIndex in 0 until trackGroups.length) {
                            val trackGroup = trackGroups[groupIndex]

                            for (trackIndex in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(trackIndex)

                                if (format.width > 0 && format.height > 0) {
                                    qualities.add(VideoQuality(
                                        trackGroup = groupIndex,
                                        trackIndex = trackIndex,
                                        width = format.width,
                                        height = format.height,
                                        bitrate = format.bitrate,
                                        label = "${format.height}p",
                                        isSelected = false
                                    ))
                                }
                            }
                        }
                    }
                }

                qualities.distinctBy { it.height }.sortedByDescending { it.height }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error getting available qualities", e)
            emptyList()
        }
    }

    fun selectQuality(quality: VideoQuality) {
        if (isReleased.get() || !isInitialized.get()) return

        try {
            val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector ?: return

            val parametersBuilder = trackSelector.parameters.buildUpon()

            if (quality.trackGroup == -1) {
                parametersBuilder
                    .setMaxVideoSizeSd()
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .clearVideoSizeConstraints()
            } else {
                parametersBuilder
                    .setMaxVideoSize(quality.width, quality.height)
                    .setMinVideoSize(quality.width, quality.height)
                    .setMaxVideoBitrate(quality.bitrate + 100000) // Add buffer
            }

            trackSelector.setParameters(parametersBuilder.build())
            Log.d("ExoPlayerManager", "Quality selected: ${quality.getQualityLabel()}")

            mainHandler.postDelayed({
                updateMediaInfo()
            }, 1000)

        } catch (e: Exception) {
            Log.e("ExoPlayerManager", "Error selecting quality", e)
        }
    }

    private fun getCurrentSelectedQuality(availableQualities: List<VideoQuality>): VideoQuality? {
        return try {
            val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector
            val params = trackSelector?.parameters

            if (params?.maxVideoWidth == Int.MAX_VALUE) {
                availableQualities.find { it.label == "Auto" }
            } else {
                val currentFormat = exoPlayer?.videoFormat
                availableQualities.find {
                    it.height == currentFormat?.height && it.width == currentFormat?.width
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun setupTrackSelector(): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(false)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setTunnelingEnabled(false)
                    .setPreferredAudioLanguages("en", "fa", "ar")
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
            )
        }
    }

}
