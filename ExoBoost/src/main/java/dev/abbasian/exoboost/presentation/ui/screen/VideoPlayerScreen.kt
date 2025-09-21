package dev.abbasian.exoboost.presentation.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.presentation.ui.component.EnhancedPlayerControls
import dev.abbasian.exoboost.presentation.ui.component.GestureHandler
import dev.abbasian.exoboost.presentation.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun ExoBoostPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    config: VideoPlayerConfig = VideoPlayerConfig(),
    onPlayerReady: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSpeedChanged: ((Float) -> Unit)? = null,
    onQualityChanged: ((VideoQuality) -> Unit)? = null,
    viewModel: VideoPlayerViewModel = koinViewModel(),
    playerManager: ExoPlayerManager = koinInject()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var isPlayerInitialized by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            Log.d("ExoBoostPlayer", "Initializing player...")
            playerManager.initializePlayer(config)
            isPlayerInitialized = true
            Log.d("ExoBoostPlayer", "Player initialized")

            launch {
                playerManager.videoState.collect { state ->
                    Log.d("ExoBoostPlayer", "Video state: $state")
                    viewModel.updateVideoState(state)
                }
            }

            launch {
                playerManager.videoInfo.collect { info ->
                    viewModel.updateVideoInfo(info)
                }
            }
        } catch (e: Exception) {
            Log.e("ExoBoostPlayer", "Error initializing player", e)
            onError?.invoke("Failed to initialize player: ${e.message}")
        }
    }

    LaunchedEffect(videoUrl, isPlayerInitialized) {
        if (isPlayerInitialized && videoUrl.isNotEmpty() && videoUrl.isNotBlank()) {
            try {
                Log.d("ExoBoostPlayer", "Loading video: $videoUrl")
                viewModel.loadVideo(videoUrl, config)
            } catch (e: Exception) {
                Log.e("ExoBoostPlayer", "Error loading video", e)
                onError?.invoke("Failed to load video: ${e.message}")
            }
        }
    }

    LaunchedEffect(uiState.videoState) {
        when (val state = uiState.videoState) {
            is VideoState.Ready -> {
                Log.d("ExoBoostPlayer", "Player ready")
                onPlayerReady?.invoke()
            }
            is VideoState.Error -> {
                Log.e("ExoBoostPlayer", "Player error: ${state.error.message}")
                onError?.invoke(state.error.message)
            }
            else -> {}
        }
    }

    LaunchedEffect(controlsVisible, uiState.videoInfo.isPlaying) {
        if (controlsVisible && uiState.videoInfo.isPlaying) {
            delay(4.seconds)
            if (uiState.videoInfo.isPlaying) {
                controlsVisible = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d("ExoBoostPlayer", "Lifecycle event: $event")
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (activity?.isChangingConfigurations != true) {
                        if (uiState.videoInfo.isPlaying) {
                            viewModel.playPause()
                        }
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    if (activity?.isChangingConfigurations != true) {
                        try {
                            playerManager.pause()
                        } catch (e: Exception) {
                            Log.e("ExoBoostPlayer", "Error pausing player", e)
                        }
                    }
                }

                Lifecycle.Event.ON_DESTROY -> {
                    if (activity?.isChangingConfigurations != true) {
                        try {
                            playerManager.release()
                            isPlayerInitialized = false
                            playerViewReady = false
                        } catch (e: Exception) {
                            Log.e("ExoBoostPlayer", "Error releasing player", e)
                        }
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            try {
                lifecycleOwner.lifecycle.removeObserver(observer)

                if (activity?.isChangingConfigurations != true) {
                    playerManager.pause()
                    if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        playerManager.release()
                        isPlayerInitialized = false
                        playerViewReady = false
                    }
                }
            } catch (e: Exception) {
                Log.e("ExoBoostPlayer", "Error in dispose", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                controlsVisible = !controlsVisible
            }
    ) {
        if (isPlayerInitialized) {
            AndroidView(
                factory = { context ->
                    Log.d("ExoBoostPlayer", "Creating PlayerView")
                    PlayerView(context).apply {
                        try {
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setKeepContentOnPlayerReset(true)
                            setUseArtwork(false)
                            setDefaultArtwork(null)
                            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)

                            val currentPlayer = playerManager.getPlayer()
                            if (currentPlayer != null) {
                                player = currentPlayer
                                Log.d("ExoBoostPlayer", "PlayerView connected to player")
                                playerViewReady = true
                                playerManager.onSurfaceAvailable()
                            } else {
                                Log.w("ExoBoostPlayer", "No player available for PlayerView")
                            }
                        } catch (e: Exception) {
                            Log.e("ExoBoostPlayer", "Error setting up PlayerView", e)
                        }
                    }
                },
                update = { view ->
                    try {
                        val currentPlayer = playerManager.getPlayer()
                        if (currentPlayer != null && view.player !== currentPlayer) {
                            view.player = currentPlayer
                            Log.d("ExoBoostPlayer", "PlayerView updated with new player")

                            view.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                            view.requestLayout()

                            if (!playerViewReady) {
                                playerViewReady = true
                                playerManager.onSurfaceAvailable()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error updating PlayerView", e)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (config.enableGestures && isPlayerInitialized && playerViewReady) {
            GestureHandler(
                volume = uiState.volume,
                brightness = uiState.brightness,
                onVolumeChange = { volume ->
                    try {
                        viewModel.setVolume(volume)
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error setting volume", e)
                    }
                },
                onBrightnessChange = { brightness ->
                    try {
                        viewModel.setBrightness(brightness)
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error setting brightness", e)
                    }
                },
                onSeek = { position ->
                    try {
                        viewModel.seekTo(position)
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error seeking", e)
                    }
                },
                currentPosition = uiState.videoInfo.currentPosition,
                duration = uiState.videoInfo.duration,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (config.showControls && isPlayerInitialized && playerViewReady) {
            EnhancedPlayerControls(
                videoState = uiState.videoState,
                videoInfo = uiState.videoInfo,
                showControls = controlsVisible,
                config = config,
                onPlayPause = {
                    try {
                        viewModel.playPause()
                        controlsVisible = true
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error in playPause", e)
                    }
                },
                onSeek = { position ->
                    try {
                        viewModel.seekTo(position)
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error seeking from controls", e)
                    }
                },
                onRetry = {
                    try {
                        viewModel.retry()
                        controlsVisible = true
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error retrying", e)
                    }
                },
                onSpeedSelected = { speed ->
                    try {
                        viewModel.setPlaybackSpeed(speed)
                        onSpeedChanged?.invoke(speed)
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error setting speed", e)
                    }
                },
                onQualitySelected = { quality ->
                    try {
                        viewModel.selectQuality(quality)
                        onQualityChanged?.invoke(quality)
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error selecting quality", e)
                    }
                },
                onFullscreen = {
                    try {
                        val newOrientation = if (isFullscreen) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                        activity?.requestedOrientation = newOrientation
                        isFullscreen = !isFullscreen
                        Log.d("ExoBoostPlayer", "Orientation changing to: $newOrientation")
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error changing orientation", e)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isFullscreen || onBack != null) {
            IconButton(
                onClick = {
                    try {
                        if (isFullscreen) {
                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            isFullscreen = false
                        } else {
                            onBack?.invoke()
                        }
                    } catch (e: Exception) {
                        Log.e("ExoBoostPlayer", "Error handling back button", e)
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = context.getString(R.string.cd_back),
                    tint = Color.White
                )
            }
        }
    }
}