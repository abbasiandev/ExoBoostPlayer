package dev.abbasian.exoboost.presentation.ui.screen

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.service.HighlightGenerationService
import dev.abbasian.exoboost.presentation.state.HighlightsState
import dev.abbasian.exoboost.presentation.ui.component.enhancedPlayerControls
import dev.abbasian.exoboost.presentation.ui.component.gestureHandler
import dev.abbasian.exoboost.presentation.ui.component.highlightsBottomSheet
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongMethod", "LongParameterList")
@OptIn(UnstableApi::class)
@Composable
fun exoBoostPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    mediaConfig: MediaPlayerConfig = MediaPlayerConfig(),
    onPlayerReady: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSpeedChanged: ((Float) -> Unit)? = null,
    onQualityChanged: ((VideoQuality) -> Unit)? = null,
    viewModel: MediaPlayerViewModel = koinViewModel(),
    playerManager: ExoPlayerManager = koinInject(),
) {
    @Suppress("ktlint:standard:property-naming")
    val TAG = "ExoBoostPlayer"
    val logger: ExoBoostLogger = koinInject()

    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val highlightsState by viewModel.highlightsState.collectAsState()

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var isModalOpen by remember { mutableStateOf(false) }
    var isPlayerInitialized by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }

    var highlightService: HighlightGenerationService? by remember { mutableStateOf(null) }
    var isBound by remember { mutableStateOf(false) }

    val showHighlightsBottomSheet by viewModel.showHighlightsBottomSheet.collectAsState()

    val serviceConnection =
        remember {
            object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName?,
                    service: IBinder?,
                ) {
                    val binder = service as HighlightGenerationService.LocalBinder
                    highlightService = binder.getService()
                    isBound = true
                    logger.debug(TAG, "Connected to highlight service")
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    highlightService = null
                    isBound = false
                    logger.debug(TAG, "Disconnected from highlight service")
                }
            }
        }

    DisposableEffect(Unit) {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
            }
        }

        onDispose {
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window.insetsController?.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    LaunchedEffect(isFullscreen) {
        activity?.window?.let { window ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    if (isFullscreen) {
                        controller.hide(
                            WindowInsetsCompat.Type.statusBars()
                                or WindowInsetsCompat.Type.navigationBars(),
                        )
                        controller.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                        controller.show(WindowInsetsCompat.Type.navigationBars())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    if (isFullscreen) {
                        (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        )
                    } else {
                        (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        )
                    }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            logger.debug(TAG, "Initializing player...")
            playerManager.initializePlayer(mediaConfig)
            isPlayerInitialized = true
            logger.debug(TAG, "Player initialized")

            launch {
                playerManager.mediaState.collect { state ->
                    logger.debug(TAG, "Video state: $state")
                    viewModel.updateMediaState(state)
                }
            }

            launch {
                playerManager.mediaInfo.collect { info ->
                    viewModel.updateMediaInfo(info)
                }
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error initializing player", e)
            onError?.invoke("Failed to initialize player: ${e.message}")
        }
    }

    LaunchedEffect(videoUrl, isPlayerInitialized) {
        if (isPlayerInitialized && videoUrl.isNotEmpty() && videoUrl.isNotBlank()) {
            try {
                logger.debug(TAG, "Loading video: $videoUrl")
                viewModel.loadMedia(videoUrl, mediaConfig)
            } catch (e: Exception) {
                logger.error(TAG, "Error loading video", e)
                onError?.invoke("Failed to load video: ${e.message}")
            }
        }
    }

    LaunchedEffect(uiState.mediaState) {
        when (val state = uiState.mediaState) {
            is MediaState.Ready -> {
                logger.debug(TAG, "Player ready")
                onPlayerReady?.invoke()
            }

            is MediaState.Error -> {
                logger.error(TAG, "Player error: ${state.error.message}")
                onError?.invoke(state.error.message)
            }

            else -> {}
        }
    }

    LaunchedEffect(controlsVisible, uiState.mediaInfo.isPlaying, isModalOpen) {
        if (controlsVisible && uiState.mediaInfo.isPlaying && !isModalOpen) {
            delay(4.seconds)
            if (uiState.mediaInfo.isPlaying && !isModalOpen) {
                controlsVisible = false
            }
        }
    }

    LaunchedEffect(highlightService) {
        highlightService?.highlightsState?.collect { state ->
            logger.debug(TAG, "Service state: ${state::class.simpleName}")

            when (state) {
                is HighlightsState.Analyzing -> {
                    viewModel.updateHighlightsState(state)
                }

                is HighlightsState.Success -> {
                    viewModel.updateHighlightsState(state)
                }

                is HighlightsState.Error -> {
                    viewModel.updateHighlightsState(state)
                }

                else -> {}
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                logger.debug(TAG, "Lifecycle event: $event")
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (activity?.isChangingConfigurations != true) {
                            if (uiState.mediaInfo.isPlaying) {
                                viewModel.playPause()
                            }
                        }
                    }

                    Lifecycle.Event.ON_STOP -> {
                        if (activity?.isChangingConfigurations != true) {
                            try {
                                playerManager.pause()
                            } catch (e: Exception) {
                                logger.error(TAG, "Error pausing player", e)
                            }
                        }
                    }

                    Lifecycle.Event.ON_DESTROY -> {
                        if (activity?.isChangingConfigurations != true) {
                            try {
                                if (isBound) {
                                    context.unbindService(serviceConnection)
                                    isBound = false
                                }
                                playerManager.release()
                                isPlayerInitialized = false
                                playerViewReady = false
                            } catch (e: Exception) {
                                logger.error(TAG, "Error releasing player", e)
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
                    if (isBound) {
                        context.unbindService(serviceConnection)
                        isBound = false
                    }
                    playerManager.pause()
                    if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        playerManager.release()
                        isPlayerInitialized = false
                        playerViewReady = false
                    }
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error in dispose", e)
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    enabled = !showHighlightsBottomSheet,
                ) {
                    controlsVisible = !controlsVisible
                },
    ) {
        if (isPlayerInitialized) {
            AndroidView(
                factory = { context ->
                    logger.debug(TAG, "Creating PlayerView")
                    PlayerView(context).apply {
                        try {
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            setKeepContentOnPlayerReset(true)
                            setUseArtwork(false)
                            setDefaultArtwork(null)
                            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)

                            val currentPlayer = playerManager.getPlayer()
                            if (currentPlayer != null) {
                                player = currentPlayer
                                logger.debug(TAG, "PlayerView connected to player")
                                playerViewReady = true
                                playerManager.onSurfaceAvailable()
                            } else {
                                logger.warning(TAG, "No player available for PlayerView")
                            }
                        } catch (e: Exception) {
                            logger.error(TAG, "Error setting up PlayerView", e)
                        }
                    }
                },
                update = { view ->
                    try {
                        val currentPlayer = playerManager.getPlayer()
                        if (currentPlayer != null && view.player !== currentPlayer) {
                            view.player = currentPlayer
                            logger.debug(TAG, "PlayerView updated with new player")

                            view.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
                            view.requestLayout()

                            if (!playerViewReady) {
                                playerViewReady = true
                                playerManager.onSurfaceAvailable()
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(TAG, "Error updating PlayerView", e)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (mediaConfig.enableGestures && isPlayerInitialized && playerViewReady) {
            gestureHandler(
                volume = uiState.volume,
                brightness = uiState.brightness,
                onVolumeChange = { volume ->
                    try {
                        viewModel.setVolume(volume)
                    } catch (e: Exception) {
                        logger.error(TAG, "Error setting volume", e)
                    }
                },
                onBrightnessChange = { brightness ->
                    try {
                        viewModel.setBrightness(brightness)
                    } catch (e: Exception) {
                        logger.error(TAG, "Error setting brightness", e)
                    }
                },
                onSeek = { position ->
                    try {
                        viewModel.seekTo(position)
                    } catch (e: Exception) {
                        logger.error(TAG, "Error seeking", e)
                    }
                },
                currentPosition = uiState.mediaInfo.currentPosition,
                duration = uiState.mediaInfo.duration,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (mediaConfig.showControls && isPlayerInitialized && playerViewReady) {
            enhancedPlayerControls(
                mediaState = uiState.mediaState,
                mediaInfo = uiState.mediaInfo,
                showControls = controlsVisible,
                mediaConfig = mediaConfig,
                onPlayPause = {
                    try {
                        viewModel.playPause()
                        controlsVisible = true
                    } catch (e: Exception) {
                        logger.error(TAG, "Error in playPause", e)
                    }
                },
                onSeek = { position ->
                    try {
                        viewModel.seekTo(position)
                    } catch (e: Exception) {
                        logger.error(TAG, "Error seeking from controls", e)
                    }
                },
                onRetry = {
                    try {
                        viewModel.retry()
                        controlsVisible = true
                    } catch (e: Exception) {
                        logger.error(TAG, "Error retrying", e)
                    }
                },
                onSpeedSelected = { speed ->
                    try {
                        viewModel.setPlaybackSpeed(speed)
                        onSpeedChanged?.invoke(speed)
                    } catch (e: Exception) {
                        logger.error(TAG, "Error setting speed", e)
                    }
                },
                onQualitySelected = { quality ->
                    try {
                        viewModel.selectQuality(quality)
                        onQualityChanged?.invoke(quality)
                    } catch (e: Exception) {
                        logger.error(TAG, "Error selecting quality", e)
                    }
                },
                onFullscreen = {
                    try {
                        val newOrientation =
                            if (isFullscreen) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        activity?.requestedOrientation = newOrientation
                        isFullscreen = !isFullscreen
                        logger.debug(TAG, "Orientation changing to: $newOrientation")
                    } catch (e: Exception) {
                        logger.error(TAG, "Error changing orientation", e)
                    }
                },
                onModalStateChanged = { isOpen ->
                    isModalOpen = isOpen
                },
                onGenerateHighlights =
                    if (mediaConfig.enableSmartHighlights) {
                        {
                            try {
                                viewModel.toggleHighlightsBottomSheet(true)

                                when (val currentState = highlightsState) {
                                    is HighlightsState.Idle -> {
                                        logger.info(TAG, "Starting new highlight generation")
                                        viewModel.generateHighlights(
                                            videoUrl = videoUrl,
                                            config = mediaConfig.highlightConfig,
                                            useCache = true,
                                        )
                                    }

                                    is HighlightsState.Success -> {
                                        logger.debug(
                                            TAG,
                                            "Showing cached results: ${currentState.highlights.highlights.size} highlights",
                                        )
                                    }

                                    is HighlightsState.Analyzing -> {
                                        logger.debug(
                                            TAG,
                                            "Showing analysis progress: ${currentState.progressPercent}%",
                                        )
                                    }

                                    is HighlightsState.Error -> {
                                        logger.info(
                                            TAG,
                                            "Retrying after error: ${currentState.message}",
                                        )
                                        viewModel.generateHighlights(
                                            videoUrl = videoUrl,
                                            config = mediaConfig.highlightConfig,
                                            useCache = false,
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error(TAG, "Error in onGenerateHighlights", e)
                            }
                        }
                    } else {
                        null
                    },
                highlightsState = highlightsState,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (mediaConfig.enableSmartHighlights) {
            highlightsBottomSheet(
                highlightsState = highlightsState,
                showBottomSheet = showHighlightsBottomSheet,
                onDismiss = {
                    logger.debug(
                        TAG,
                        "Bottom sheet dismiss requested - State: ${highlightsState::class.simpleName}",
                    )

                    when (highlightsState) {
                        is HighlightsState.Analyzing -> {
                            logger.info(TAG, "Cancelling analysis")
                            viewModel.cancelHighlightGeneration()
                        }

                        else -> {
                            // Just close the sheet, keep the state
                            logger.debug(TAG, "Closing bottom sheet, keeping state")
                            viewModel.dismissHighlightsBottomSheet()
                        }
                    }
                },
                onPlayHighlights = {
                    try {
                        logger.debug(TAG, "Playing highlights")
                        viewModel.playHighlights()
                        controlsVisible = false
                    } catch (e: Exception) {
                        logger.error(TAG, "Error playing highlights", e)
                    }
                },
                onJumpToHighlight = { index ->
                    try {
                        logger.debug(TAG, "Jumping to highlight: $index")
                        viewModel.jumpToHighlight(index)
                        controlsVisible = false
                    } catch (e: Exception) {
                        logger.error(TAG, "Error jumping to highlight", e)
                    }
                },
                onJumpToChapter = { chapter ->
                    try {
                        logger.debug(TAG, "Jumping to chapter: ${chapter.title}")
                        viewModel.jumpToChapter(chapter)
                        controlsVisible = false
                    } catch (e: Exception) {
                        logger.error(TAG, "Error jumping to chapter", e)
                    }
                },
            )
        }
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
                    logger.error(TAG, "Error handling back button", e)
                }
            },
            modifier =
                Modifier
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = context.getString(R.string.cd_back),
                tint = Color.White,
            )
        }
    }
}
