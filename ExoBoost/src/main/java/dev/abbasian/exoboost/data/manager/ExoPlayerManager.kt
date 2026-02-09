package dev.abbasian.exoboost.data.manager

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException

@UnstableApi
class ExoPlayerManager(
    private val context: Context,
    private val dataSourceFactory: DataSource.Factory,
    private val networkManager: NetworkManager,
    private val autoRecoveryManager: AutoRecoveryManager,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "ExoPlayerManager"
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var hasTriedSoftwareDecoder = false
    private var qualityDowngradeAttempts = 0
    private val maxQualityDowngrades = 3

    private val _equalizerValues = MutableStateFlow(List(8) { 0.5f })
    val equalizerValues: StateFlow<List<Float>> = _equalizerValues.asStateFlow()

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
    private val positionUpdateRunnable =
        object : Runnable {
            override fun run() {
                if (!isReleased.get() && exoPlayer != null) {
                    updateMediaInfo()
                    mainHandler.postDelayed(this, 500)
                }
            }
        }

    fun initializePlayer(config: MediaPlayerConfig) {
        if (isInitialized.get() && !isReleased.get()) {
            logger.debug(TAG, "Player already initialized")
            return
        }

        try {
            isReleased.set(false)
            isPrepared.set(false)
            isMediaReady.set(false)
            currentConfig = config
            logger.debug(TAG, "Initializing ExoPlayer...")

            val loadControl =
                DefaultLoadControl
                    .Builder()
                    .setBufferDurationsMs(
                        config.bufferDurations.minBufferMs,
                        config.bufferDurations.maxBufferMs,
                        config.bufferDurations.bufferForPlaybackMs,
                        config.bufferDurations.bufferForPlaybackAfterRebufferMs,
                    ).build()

            val trackSelector = setupTrackSelector()

            exoPlayer?.let { player ->
                player.removeListener(playerListener)
                player.removeAnalyticsListener(analyticsListener)
                player.release()
            }

            exoPlayer =
                ExoPlayer
                    .Builder(context)
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

                        initializeAudioEffects()
                    }

            isInitialized.set(true)
            startPositionUpdates()

            logger.debug(TAG, "ExoPlayer initialized successfully")
        } catch (e: Exception) {
            logger.error(TAG, "Failed to initialize player", e)
            _mediaState.value =
                MediaState.Error(
                    PlayerError.UnknownError(
                        context.getString(R.string.error_init_player) + e.message,
                        e,
                    ),
                )
            isInitialized.set(false)
        }
    }

    private fun initializeAudioEffects() {
        try {
            exoPlayer?.let { player ->
                val audioSessionId = player.audioSessionId
                logger.debug(TAG, "Initializing audio effects with session ID: $audioSessionId")

                if (audioSessionId != 0) {
                    releaseAudioEffects()

                    equalizer =
                        Equalizer(0, audioSessionId).apply {
                            enabled = true
                            logger.debug(TAG, "Equalizer initialized with $numberOfBands bands")
                        }

                    bassBoost =
                        BassBoost(0, audioSessionId).apply {
                            enabled = true
                        }

                    virtualizer =
                        Virtualizer(0, audioSessionId).apply {
                            enabled = true
                        }
                } else {
                    logger.warning(TAG, "Invalid audio session ID, cannot initialize effects")
                }
            }
        } catch (e: Exception) {
            logger.error(TAG, "Failed to initialize audio effects", e)
        }
    }

    fun loadMedia(
        url: String,
        subtitleConfigurations: List<MediaItem.SubtitleConfiguration> = emptyList(),
    ) {
        if (isReleased.get()) {
            logger.warning(TAG, "Cannot load video: player is released")
            return
        }

        if (!isInitialized.get()) {
            logger.warning(TAG, "Cannot load video: player not initialized")
            return
        }

        if (currentUrl == url && isPrepared.get() && subtitleConfigurations.isEmpty()) {
            logger.debug(TAG, "Video already loaded: $url")
            return
        }

        currentUrl = url
        retryCount = 0
        isPrepared.set(false)
        isMediaReady.set(false)

        if (!networkManager.isNetworkAvailable()) {
            _mediaState.value =
                MediaState.Error(
                    PlayerError.NetworkError("اتصال اینترنت موجود نیست"),
                )
            return
        }

        try {
            _mediaState.value = MediaState.Loading
            logger.debug(TAG, "Loading video: $url with ${subtitleConfigurations.size} subtitles")

            val mediaItemBuilder =
                MediaItem
                    .Builder()
                    .setUri(Uri.parse(url))

            if (subtitleConfigurations.isNotEmpty()) {
                mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)
            }

            val mediaItem = mediaItemBuilder.build()

            exoPlayer?.apply {
                stop()
                clearMediaItems()
                setMediaItem(mediaItem)
                prepare()
            }

            logger.debug(TAG, "Video preparation started")
        } catch (e: Exception) {
            logger.error(TAG, "Error loading video", e)
            handleLoadError(e)
        }
    }

    fun setEqualizerBand(
        bandIndex: Int,
        value: Float,
    ) {
        try {
            equalizer?.let { eq ->
                if (bandIndex >= 0 && bandIndex < eq.numberOfBands) {
                    // Convert 0-1 range to -12dB to +12dB
                    val dbValue = (value - 0.5f) * 24f
                    val millibels = (dbValue * 100).toInt()

                    val bandLevelRange = eq.bandLevelRange
                    val clampedLevel =
                        millibels.coerceIn(
                            bandLevelRange[0].toInt(),
                            bandLevelRange[1].toInt(),
                        )

                    eq.setBandLevel(bandIndex.toShort(), clampedLevel.toShort())

                    logger.debug(
                        TAG,
                        "Set band $bandIndex to ${dbValue}dB ($clampedLevel millibels)",
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error setting equalizer band $bandIndex", e)
        }
    }

    fun applyEqualizerValues(values: List<Float>) {
        logger.debug(TAG, "Applying equalizer values: $values")
        values.forEachIndexed { index, value ->
            setEqualizerBand(index, value)
        }
        _equalizerValues.value = values
    }

    fun getEqualizerBandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 8

    fun getEqualizerFrequencies(): List<String> =
        try {
            equalizer?.let { eq ->
                (0 until eq.numberOfBands).map { band ->
                    val centerFreq = eq.getCenterFreq(band.toShort()) / 1000
                    when {
                        centerFreq >= 1000 -> "${centerFreq / 1000}kHz"
                        else -> "${centerFreq}Hz"
                    }
                }
            } ?: listOf("60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz")
        } catch (e: Exception) {
            logger.error(TAG, "Error getting frequencies", e)
            listOf("60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz")
        }

    fun isReadyForSurface(): Boolean = isInitialized.get() && isPrepared.get() && !isReleased.get()

    fun onSurfaceAvailable() {
        logger.debug(TAG, "Surface available")
        if (isReadyForSurface() && currentConfig.autoPlay) {
            mainHandler.postDelayed({
                if (!isReleased.get() && exoPlayer != null) {
                    exoPlayer?.playWhenReady = true
                }
            }, 100)
        }
    }

    private val playerListener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (isReleased.get()) return

                val stateString =
                    when (playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN"
                    }

                logger.debug(TAG, "Playback state changed: $stateString")

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
                        resetRecoveryState()

                        isPrepared.set(true)

                        val hasVideo = exoPlayer?.videoFormat != null
                        if (hasVideo && !isMediaReady.get()) {
                            _mediaState.value = MediaState.Loading
                        } else {
                            isMediaReady.set(true)
                            _mediaState.value = MediaState.Ready
                        }

                        if (equalizer == null) {
                            initializeAudioEffects()
                        }
                    }

                    Player.STATE_ENDED -> {
                        _mediaState.value = MediaState.Ended
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isReleased.get()) return

                logger.debug(TAG, "Is playing changed: $isPlaying")

                _mediaState.value =
                    if (isPlaying) {
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
                logger.debug(TAG, "Video size changed: ${videoSize.width}x${videoSize.height}")
            }

            override fun onRenderedFirstFrame() {
                if (isReleased.get()) return
                logger.debug(TAG, "First frame rendered")

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

                var logMessage =
                    "Player error: ${error.errorCodeName} (${error.errorCode}) - ${error.message}"

                val cause = error.cause
                if (cause is HttpDataSource.InvalidResponseCodeException) {
                    logMessage += "\n HTTP Status Code: ${cause.responseCode}"
                } else if (cause is IOException) {
                    logMessage += "\n IO Cause: ${cause.javaClass.simpleName} - ${cause.message}"
                } else if (cause != null) {
                    logMessage += "\n Cause: ${cause.javaClass.simpleName} - ${cause.message}"
                }

                logger.error(
                    TAG,
                    logMessage,
                    error,
                )

                isPrepared.set(false)
                isMediaReady.set(false)

                val playerError = mapPlaybackException(error)
                _mediaState.value = MediaState.Error(playerError)

                if (error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED &&
                    !hasTriedSoftwareDecoder
                ) {
                    logger.warning(
                        TAG,
                        "Hardware decoder failed, attempting software decoder fallback",
                    )
                    hasTriedSoftwareDecoder = true

                    mainHandler.postDelayed({
                        if (!isReleased.get()) {
                            try {
                                val currentPosition = exoPlayer?.currentPosition ?: 0L
                                val wasPlaying = exoPlayer?.playWhenReady ?: false

                                initializePlayer(
                                    currentConfig.copy(
                                        preferSoftwareDecoder = true,
                                    ),
                                )

                                loadMedia(currentUrl)

                                mainHandler.postDelayed({
                                    seekTo(currentPosition)
                                    if (wasPlaying) play()
                                }, 1000)

                                logger.info(TAG, "Switched to software decoder")
                            } catch (e: Exception) {
                                logger.error(TAG, "Software decoder fallback failed", e)
                            }
                        }
                    }, 1000)
                    return
                }

                if (currentConfig.autoQualityOnError &&
                    playerError is PlayerError.NetworkError &&
                    qualityDowngradeAttempts < maxQualityDowngrades
                ) {
                    logger.warning(TAG, "Network issue detected, attempting quality downgrade")

                    mainHandler.postDelayed({
                        if (!isReleased.get()) {
                            try {
                                val availableQualities =
                                    getAvailableQualities()
                                        .filter { it.label != "Auto" }
                                        .sortedBy { it.height }

                                val currentQuality = getCurrentSelectedQuality(availableQualities)
                                val currentIndex =
                                    availableQualities.indexOfFirst {
                                        it.height == currentQuality?.height
                                    }

                                if (currentIndex > 0) {
                                    val lowerQuality = availableQualities[currentIndex - 1]
                                    logger.info(
                                        TAG,
                                        "Auto-downgrading: ${currentQuality?.label} -> ${lowerQuality.label}",
                                    )

                                    selectQuality(lowerQuality)
                                    qualityDowngradeAttempts++

                                    mainHandler.postDelayed({
                                        if (!isReleased.get()) {
                                            retry()
                                        }
                                    }, 1500)
                                } else {
                                    logger.warning(
                                        TAG,
                                        "Already at lowest quality, cannot downgrade further",
                                    )
                                    scheduleNormalRetry(playerError)
                                }
                            } catch (e: Exception) {
                                logger.error(TAG, "Quality downgrade failed", e)
                                scheduleNormalRetry(playerError)
                            }
                        }
                    }, 1000)
                    return
                }

                scheduleNormalRetry(playerError)
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                if (isReleased.get()) return
                logger.debug(TAG, "Media item transition")
                updateMediaInfo()
            }
        }

    private fun scheduleNormalRetry(playerError: PlayerError) {
        if (!currentConfig.retryOnError || !isRetryableError(playerError)) {
            logger.warning(TAG, "Error is not retryable")
            resetRecoveryState()
            return
        }

        if (retryCount >= currentConfig.maxRetryCount) {
            logger.warning(TAG, "Max retry attempts reached")
            resetRecoveryState()
            return
        }

        mainHandler.post {
            GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                try {
                    if (autoRecoveryManager.shouldRetry(playerError, currentConfig.maxRetryCount)) {
                        retry()
                    } else {
                        logger.warning(TAG, "AutoRecoveryManager decided not to retry")
                        resetRecoveryState()
                    }
                } catch (e: Exception) {
                    logger.error(TAG, "Error in auto recovery", e)
                    resetRecoveryState()
                }
            }
        }
    }

    private fun resetRecoveryState() {
        hasTriedSoftwareDecoder = false
        qualityDowngradeAttempts = 0
        autoRecoveryManager.reset()
    }

    fun play() {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            exoPlayer?.playWhenReady = true
            logger.debug(TAG, "Play requested")
        } catch (e: Exception) {
            logger.error(TAG, "Error playing", e)
        }
    }

    fun pause() {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            exoPlayer?.playWhenReady = false
            exoPlayer?.pause()
            logger.debug(TAG, "Pause requested")
        } catch (e: Exception) {
            logger.error(TAG, "Error pausing", e)
        }
    }

    fun seekTo(position: Long) {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            exoPlayer?.seekTo(position.coerceAtLeast(0L))
            logger.debug(TAG, "Seek to: $position")
        } catch (e: Exception) {
            logger.error(TAG, "Error seeking", e)
        }
    }

    fun setVolume(volume: Float) {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            val clampedVolume = volume.coerceIn(0f, 1f)
            exoPlayer?.volume = clampedVolume
            updateMediaInfo()
            logger.debug(TAG, "Volume set to: $clampedVolume")
        } catch (e: Exception) {
            logger.error(TAG, "Error setting volume", e)
        }
    }

    fun retry() {
        if (isReleased.get() || !isInitialized.get()) {
            logger.warning(TAG, "Cannot retry: player not available")
            return
        }

        if (retryCount >= currentConfig.maxRetryCount) {
            _mediaState.value =
                MediaState.Error(
                    PlayerError.UnknownError(context.getString(R.string.error_retry_limit)),
                )

            hasTriedSoftwareDecoder = false
            qualityDowngradeAttempts = 0
            return
        }

        retryCount++
        isPrepared.set(false)
        isMediaReady.set(false)

        if (!networkManager.isNetworkAvailable()) {
            _mediaState.value =
                MediaState.Error(
                    PlayerError.NetworkError(context.getString(R.string.help_check_network)),
                )
            return
        }

        try {
            _mediaState.value = MediaState.Loading
            logger.debug(TAG, "Retrying... attempt $retryCount")

            exoPlayer?.let { player ->
                player.stop()
                player.prepare()
                player.playWhenReady = false
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error during retry", e)
            handleLoadError(e)
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            equalizer = null
            bassBoost = null
            virtualizer = null
        } catch (e: Exception) {
            logger.error(TAG, "Error releasing audio effects", e)
        }
    }

    fun release() {
        if (isReleased.getAndSet(true)) {
            logger.debug(TAG, "Player already released")
            return
        }

        logger.debug(TAG, "Releasing ExoPlayer")

        try {
            stopPositionUpdates()
            releaseAudioEffects()

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

            logger.debug(TAG, "ExoPlayer released successfully")
        } catch (e: Exception) {
            logger.error(TAG, "Error during release", e)
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

    private val analyticsListener =
        object : AnalyticsListener {
            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
                error: IOException,
                wasCanceled: Boolean,
            ) {
                if (!wasCanceled) {
                    handleLoadError(error)
                }
            }
        }

    private fun handleLoadError(error: Throwable) {
        val playerError =
            when (error) {
                is SSLException -> {
                    PlayerError.SSLError(
                        context.getString(R.string.error_security_certificate_with_message) + error.message,
                        error.message,
                        error,
                    )
                }

                is SocketTimeoutException -> {
                    PlayerError.NetworkError(
                        context.getString(R.string.error_timeout),
                        error,
                        retryable = true,
                    )
                }

                is IOException -> {
                    PlayerError.NetworkError(
                        context.getString(R.string.error_network) + error.message,
                        error,
                        retryable = true,
                    )
                }

                else -> {
                    PlayerError.UnknownError(
                        context.getString(R.string.error_network) + error.message,
                        error,
                    )
                }
            }

        _mediaState.value = MediaState.Error(playerError)
        logger.error(TAG, "Load error: ${playerError.message}", error)
    }

    private fun mapPlaybackException(error: PlaybackException): PlayerError =
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                PlayerError.NetworkError(
                    context.getString(R.string.error_network),
                    error,
                    retryable = true,
                )
            }

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                PlayerError.NetworkError(
                    context.getString(R.string.error_timeout),
                    error,
                    retryable = true,
                )
            }

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                PlayerError.LiveStreamError(
                    context.getString(R.string.error_http) + extractHttpCode(error),
                    extractHttpCode(error),
                    error,
                )
            }

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            -> {
                PlayerError.SourceError(
                    context.getString(R.string.error_format_not_supported),
                    currentUrl,
                    error,
                )
            }

            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            -> {
                PlayerError.CodecError(
                    context.getString(R.string.error_decoding),
                    cause = error,
                )
            }

            else -> {
                if (error.cause is SSLException) {
                    PlayerError.SSLError(
                        context.getString(R.string.error_ssl_certificate_with_message) + error.message,
                        error.cause?.message,
                        error,
                    )
                } else {
                    PlayerError.UnknownError(
                        context.getString(R.string.error_unknown) + error.message,
                        error,
                    )
                }
            }
        }

    private fun extractHttpCode(error: PlaybackException): Int? =
        error.message?.let { message ->
            Regex("HTTP (\\d{3})")
                .find(message)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }

    private fun isRetryableError(error: PlayerError): Boolean =
        when (error) {
            is PlayerError.NetworkError -> error.isRetryable
            is PlayerError.LiveStreamError -> error.httpCode in listOf(403, 404, 500, 502, 503, 504)
            is PlayerError.SSLError -> false
            else -> false
        }

    private fun updateMediaInfo() {
        exoPlayer?.let { player ->
            val availableQualities = getAvailableQualities()
            val currentQuality = getCurrentSelectedQuality(availableQualities)

            _mediaInfo.value =
                MediaInfo(
                    currentPosition = player.currentPosition.coerceAtLeast(0L),
                    duration = if (player.duration != C.TIME_UNSET) player.duration else 0L,
                    volume = player.volume,
                    isPlaying = player.isPlaying,
                    bufferedPosition = player.bufferedPosition.coerceAtLeast(0L),
                    playbackSpeed = player.playbackParameters.speed,
                    availableQualities = availableQualities,
                    currentQuality = currentQuality,
                )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (isReleased.get() || !isInitialized.get()) return
        try {
            val clampedSpeed = speed.coerceIn(0.25f, 3.0f)
            exoPlayer?.setPlaybackSpeed(clampedSpeed)
            logger.debug(TAG, "Playback speed set to: $clampedSpeed")
            updateMediaInfo()
        } catch (e: Exception) {
            logger.error(TAG, "Error setting playback speed", e)
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

                qualities.add(
                    VideoQuality(
                        trackGroup = -1,
                        trackIndex = -1,
                        width = 0,
                        height = 0,
                        bitrate = 0,
                        label = "Auto",
                        isSelected = trackSelectionParameters?.maxVideoWidth == Int.MAX_VALUE,
                    ),
                )

                for (rendererIndex in 0 until trackInfo.rendererCount) {
                    if (trackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_VIDEO) {
                        val trackGroups = trackInfo.getTrackGroups(rendererIndex)

                        for (groupIndex in 0 until trackGroups.length) {
                            val trackGroup = trackGroups[groupIndex]

                            for (trackIndex in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(trackIndex)

                                if (format.width > 0 && format.height > 0) {
                                    qualities.add(
                                        VideoQuality(
                                            trackGroup = groupIndex,
                                            trackIndex = trackIndex,
                                            width = format.width,
                                            height = format.height,
                                            bitrate = format.bitrate,
                                            label = "${format.height}p",
                                            isSelected = false,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                qualities.distinctBy { it.height }.sortedByDescending { it.height }
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error(TAG, "Error getting available qualities", e)
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
            logger.debug(TAG, "Quality selected: ${quality.getQualityLabel()}")

            mainHandler.postDelayed({
                updateMediaInfo()
            }, 1000)
        } catch (e: Exception) {
            logger.error(TAG, "Error selecting quality", e)
        }
    }

    private fun getCurrentSelectedQuality(availableQualities: List<VideoQuality>): VideoQuality? =
        try {
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

    private fun setupTrackSelector(): DefaultTrackSelector =
        DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(false)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setTunnelingEnabled(false)
                    .setPreferredAudioLanguages("en", "fa", "ar")
                    .setPreferredTextLanguages("en", "fa", "ar")
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false),
            )
        }

    fun getAvailableSubtitleTracks(): List<androidx.media3.common.Tracks.Group> {
        if (isReleased.get() || !isInitialized.get()) return emptyList()

        return try {
            exoPlayer?.currentTracks?.groups?.filter { group ->
                group.type == C.TRACK_TYPE_TEXT
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error(TAG, "Error getting subtitle tracks", e)
            emptyList()
        }
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        if (isReleased.get() || !isInitialized.get()) return

        try {
            val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector ?: return

            trackSelector.setParameters(
                trackSelector.parameters
                    .buildUpon()
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, !enabled)
                    .build(),
            )

            logger.debug(TAG, "Subtitles ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            logger.error(TAG, "Error setting subtitle state", e)
        }
    }

    fun selectSubtitleTrack(languageCode: String) {
        if (isReleased.get() || !isInitialized.get()) return

        try {
            val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector ?: return

            trackSelector.setParameters(
                trackSelector.parameters
                    .buildUpon()
                    .setPreferredTextLanguage(languageCode)
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                    .build(),
            )

            logger.debug(TAG, "Selected subtitle track: $languageCode")
        } catch (e: Exception) {
            logger.error(TAG, "Error selecting subtitle track", e)
        }
    }

    fun addSubtitleToCurrentMedia(subtitleConfiguration: MediaItem.SubtitleConfiguration) {
        if (isReleased.get() || !isInitialized.get()) return

        try {
            exoPlayer?.let { player ->
                val currentPosition = player.currentPosition
                val wasPlaying = player.isPlaying

                val currentMediaItem = player.currentMediaItem ?: return

                val existingSubtitles =
                    currentMediaItem.localConfiguration?.subtitleConfigurations ?: emptyList()

                val existingUris = existingSubtitles.map { it.uri.toString() }.toSet()
                val allSubtitleConfigurations =
                    if (subtitleConfiguration.uri.toString() in existingUris) {
                        existingSubtitles.filter { it.uri != subtitleConfiguration.uri } +
                            listOf(
                                subtitleConfiguration,
                            )
                    } else {
                        existingSubtitles + listOf(subtitleConfiguration)
                    }

                player.stop()

                val videoUri =
                    currentMediaItem.localConfiguration?.uri ?: Uri.parse(currentMediaItem.mediaId)
                logger.debug(TAG, "Video URI to use: $videoUri")

                val newMediaItem =
                    MediaItem
                        .Builder()
                        .setUri(videoUri)
                        .setSubtitleConfigurations(allSubtitleConfigurations)
                        .build()

                player.clearMediaItems()
                player.setMediaItem(newMediaItem, currentPosition)
                player.prepare()

                mainHandler.postDelayed({
                    if (!isReleased.get() && player.playbackState == Player.STATE_READY) {
                        if (wasPlaying) {
                            player.playWhenReady = true
                            player.play()
                        }
                    }
                }, 500)

                mainHandler.postDelayed({
                    if (!isReleased.get()) {
                        setSubtitleEnabled(true)
                    }
                }, 600)
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error adding subtitle to media", e)
        }
    }

    private suspend fun handleNetworkDegradation() {
        if (qualityDowngradeAttempts >= maxQualityDowngrades) {
            logger.warning(TAG, "Max quality downgrades reached")
            return
        }

        val availableQualities =
            getAvailableQualities()
                .filter { it.label != "Auto" }
                .sortedBy { it.height }

        val currentQuality = getCurrentSelectedQuality(availableQualities)
        val currentIndex = availableQualities.indexOf(currentQuality)

        if (currentIndex > 0) {
            val lowerQuality = availableQualities[currentIndex - 1]
            logger.info(
                TAG,
                "Auto-downgrading quality: ${currentQuality?.label} -> ${lowerQuality.label}",
            )
            selectQuality(lowerQuality)
            qualityDowngradeAttempts++
            delay(2000)
        } else {
            logger.warning(TAG, "Already at lowest quality")
        }
    }
}
