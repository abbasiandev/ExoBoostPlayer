package dev.abbasian.exoboost.presentation.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.media3.ui.PlayerView
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.presentation.ui.component.EnhancedPlayerControls
import dev.abbasian.exoboost.presentation.ui.component.GestureHandler
import dev.abbasian.exoboost.presentation.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.delay
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
    viewModel: VideoPlayerViewModel = koinViewModel(),
    playerManager: ExoPlayerManager = koinInject()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        playerManager.videoState.collect { state ->
            viewModel.updateVideoState(state)
        }
    }

    LaunchedEffect(Unit) {
        playerManager.videoInfo.collect { info ->
            viewModel.updateVideoInfo(info)
        }
    }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            viewModel.loadVideo(videoUrl, config)
        }
    }

    LaunchedEffect(uiState.videoState) {
        if (uiState.videoState is VideoState.Ready) {
            onPlayerReady?.invoke()
        }
    }

    LaunchedEffect(uiState.videoState) {
        if (uiState.videoState is VideoState.Error) {
            onError?.invoke((uiState.videoState as VideoState.Error).error.message)
        }
    }

    LaunchedEffect(controlsVisible, uiState.videoInfo.isPlaying) {
        if (controlsVisible && uiState.videoInfo.isPlaying) {
            delay(4.seconds)
            controlsVisible = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (uiState.videoInfo.isPlaying) {
                        viewModel.playPause()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    playerManager.release()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerManager.release()
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
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = playerManager.getPlayer()
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (config.enableGestures) {
            GestureHandler(
                volume = uiState.volume,
                brightness = uiState.brightness,
                onVolumeChange = { volume -> viewModel.setVolume(volume) },
                onBrightnessChange = { brightness -> viewModel.setBrightness(brightness) },
                onSeek = { position -> viewModel.seekTo(position) },
                currentPosition = uiState.videoInfo.currentPosition,
                duration = uiState.videoInfo.duration,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (config.showControls) {
            EnhancedPlayerControls(
                videoState = uiState.videoState,
                videoInfo = uiState.videoInfo,
                showControls = controlsVisible,
                onPlayPause = {
                    viewModel.playPause()
                    controlsVisible = true
                },
                onSeek = { position ->
                    viewModel.seekTo(position)
                },
                onRetry = {
                    viewModel.retry()
                    controlsVisible = true
                },
                onFullscreen = {
                    activity?.requestedOrientation = if (isFullscreen) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    isFullscreen = !isFullscreen
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isFullscreen || onBack != null) {
            IconButton(
                onClick = {
                    if (isFullscreen) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        isFullscreen = false
                    } else {
                        onBack?.invoke()
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
                    contentDescription = "بازگشت",
                    tint = Color.White
                )
            }
        }
    }
}