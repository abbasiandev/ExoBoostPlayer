package dev.abbasian.exoboost.data.manager

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.domain.model.VideoInfo
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

@UnstableApi
class ExoPlayerManager(
    private val context: Context,
    private val dataSourceFactory: DataSource.Factory,
    private val networkManager: NetworkManager
) {
    private var _videoState = MutableStateFlow<VideoState>(VideoState.Idle)
    val videoState: StateFlow<VideoState> = _videoState.asStateFlow()

    private var _videoInfo = MutableStateFlow(VideoInfo())
    val videoInfo: StateFlow<VideoInfo> = _videoInfo.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var currentConfig: VideoPlayerConfig = VideoPlayerConfig()
    private var currentUrl: String = ""
    private var retryCount = 0
    private var isInitialized = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            updateVideoInfo()
            mainHandler.postDelayed(this, 500) // Update every 500ms
        }
    }

    fun initializePlayer(config: VideoPlayerConfig) {
        if (isInitialized) return

        try {
            currentConfig = config

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    config.bufferDurations.minBufferMs,
                    config.bufferDurations.maxBufferMs,
                    config.bufferDurations.bufferForPlaybackMs,
                    config.bufferDurations.bufferForPlaybackAfterRebufferMs
                )
                .build()

            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setForceHighestSupportedBitrate(false)
                        .setAllowAudioMixedMimeTypeAdaptiveness(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
                )
            }

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    addListener(playerListener)
                    addAnalyticsListener(analyticsListener)
                }

            isInitialized = true
            startPositionUpdates()

        } catch (e: Exception) {
            _videoState.value = VideoState.Error(
                PlayerError.UnknownError("خطا در راه‌اندازی پلیر: ${e.message}", e)
            )
        }
    }

    fun loadVideo(url: String) {
        currentUrl = url
        retryCount = 0

        if (!networkManager.isNetworkAvailable()) {
            _videoState.value = VideoState.Error(
                PlayerError.NetworkError("اتصال اینترنت موجود نیست")
            )
            return
        }

        try {
            _videoState.value = VideoState.Loading

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .build()

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = currentConfig.autoPlay
            }

        } catch (e: Exception) {
            handleLoadError(e)
        }
    }

    private fun handleLoadError(error: Throwable) {
        val playerError = when (error) {
            is SSLException -> PlayerError.SSLError(
                "خطای گواهی امنیتی: ${error.message}",
                error.message,
                error
            )
            is SocketTimeoutException -> PlayerError.NetworkError(
                "زمان اتصال به پایان رسید",
                true,
                error
            )
            is IOException -> PlayerError.NetworkError(
                "خطای شبکه: ${error.message}",
                true,
                error
            )
            else -> PlayerError.UnknownError(
                "خطای نامشخص: ${error.message}",
                error
            )
        }

        _videoState.value = VideoState.Error(playerError)
    }

    fun play() {
        exoPlayer?.playWhenReady = true
    }

    fun pause() {
        exoPlayer?.playWhenReady = false
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
        updateVideoInfo()
    }

    fun retry() {
        if (retryCount >= currentConfig.maxRetryCount) {
            _videoState.value = VideoState.Error(
                PlayerError.UnknownError("حداکثر تعداد تلاش به پایان رسید")
            )
            return
        }

        retryCount++

        if (!networkManager.isNetworkAvailable()) {
            _videoState.value = VideoState.Error(
                PlayerError.NetworkError("اتصال اینترنت برقرار نیست")
            )
            return
        }

        try {
            _videoState.value = VideoState.Loading
            exoPlayer?.prepare()
        } catch (e: Exception) {
            handleLoadError(e)
        }
    }

    fun release() {
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
        isInitialized = false
        _videoState.value = VideoState.Idle
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    private fun startPositionUpdates() {
        mainHandler.post(positionUpdateRunnable)
    }

    private fun stopPositionUpdates() {
        mainHandler.removeCallbacks(positionUpdateRunnable)
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _videoState.value = when (playbackState) {
                Player.STATE_IDLE -> VideoState.Idle
                Player.STATE_BUFFERING -> VideoState.Loading
                Player.STATE_READY -> {
                    retryCount = 0 // Reset retry count on successful load
                    VideoState.Ready
                }
                Player.STATE_ENDED -> VideoState.Ended
                else -> VideoState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _videoState.value = if (isPlaying) VideoState.Playing else {
                when (exoPlayer?.playbackState) {
                    Player.STATE_ENDED -> VideoState.Ended
                    Player.STATE_READY -> VideoState.Paused
                    else -> VideoState.Paused
                }
            }
            updateVideoInfo()
        }

        override fun onPlayerError(error: PlaybackException) {
            val playerError = mapPlaybackException(error)
            _videoState.value = VideoState.Error(playerError)

            // Auto-retry on retryable errors
            if (currentConfig.retryOnError && isRetryableError(playerError) &&
                retryCount < currentConfig.maxRetryCount) {

                mainHandler.postDelayed({
                    retry()
                }, 2000) // Wait 2 seconds before retry
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateVideoInfo()
        }
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

    private fun mapPlaybackException(error: PlaybackException): PlayerError {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                PlayerError.NetworkError("خطای اتصال شبکه", true, error)

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                PlayerError.NetworkError("زمان اتصال به پایان رسید", true, error)

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                PlayerError.LiveStreamError(
                    "خطای HTTP: ${extractHttpCode(error)}",
                    extractHttpCode(error),
                    error
                )

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                PlayerError.SourceError("فرمت ویدیو پشتیبانی نمی‌شود", currentUrl, error)

            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                PlayerError.CodecError("خطا در رمزگشایی ویدیو", null, error)

            else -> {
                if (error.cause is SSLException) {
                    PlayerError.SSLError(
                        "خطای گواهی SSL: ${error.message}",
                        error.cause?.message,
                        error
                    )
                } else {
                    PlayerError.UnknownError("خطای نامشخص: ${error.message}", error)
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
            is PlayerError.SSLError -> false // SSL errors usually need manual intervention
            else -> false
        }
    }

    private fun updateVideoInfo() {
        exoPlayer?.let { player ->
            _videoInfo.value = VideoInfo(
                currentPosition = player.currentPosition.coerceAtLeast(0L),
                duration = if (player.duration != C.TIME_UNSET) player.duration else 0L,
                volume = player.volume,
                isPlaying = player.isPlaying,
                bufferedPosition = player.bufferedPosition.coerceAtLeast(0L),
                playbackSpeed = player.playbackParameters.speed
            )
        }
    }
}