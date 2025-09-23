package dev.abbasian.exoboost.presentation.ui.screen

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import dev.abbasian.exoboost.R
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.domain.model.VisualizationType
import dev.abbasian.exoboost.presentation.ui.component.AudioVisualization
import dev.abbasian.exoboost.presentation.ui.component.GlassyAudioControls
import dev.abbasian.exoboost.presentation.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun ExoBoostAudioPlayer(
    audioUrl: String,
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

    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var isPlayerInitialized by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }
    var audioVisualizationData by remember { mutableStateOf(floatArrayOf()) }

    var visualizationTime by remember { mutableLongStateOf(0L) }
    var bassIntensity by remember { mutableFloatStateOf(0f) }
    var midIntensity by remember { mutableFloatStateOf(0f) }
    var trebleIntensity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        try {
            Log.d("ExoBoostAudioPlayer", "Initializing audio player...")
            playerManager.initializePlayer(config)
            isPlayerInitialized = true
            Log.d("ExoBoostAudioPlayer", "Audio player initialized")

            launch {
                playerManager.videoState.collect { state ->
                    Log.d("ExoBoostAudioPlayer", "Audio state: $state")
                    viewModel.updateVideoState(state)
                }
            }

            launch {
                playerManager.videoInfo.collect { info ->
                    viewModel.updateVideoInfo(info)
                }
            }
        } catch (e: Exception) {
            Log.e("ExoBoostAudioPlayer", "Error initializing audio player", e)
            onError?.invoke("Failed to initialize audio player: ${e.message}")
        }
    }

    LaunchedEffect(audioUrl, isPlayerInitialized) {
        if (isPlayerInitialized && audioUrl.isNotEmpty() && audioUrl.isNotBlank()) {
            try {
                Log.d("ExoBoostAudioPlayer", "Loading audio: $audioUrl")
                viewModel.loadVideo(audioUrl, config)
            } catch (e: Exception) {
                Log.e("ExoBoostAudioPlayer", "Error loading audio", e)
                onError?.invoke("Failed to load audio: ${e.message}")
            }
        }
    }

    LaunchedEffect(uiState.videoState) {
        when (val state = uiState.videoState) {
            is VideoState.Ready -> {
                Log.d("ExoBoostAudioPlayer", "Audio player ready")
                onPlayerReady?.invoke()
            }
            is VideoState.Error -> {
                Log.e("ExoBoostAudioPlayer", "Audio player error: ${state.error.message}")
                onError?.invoke(state.error.message)
            }
            else -> {}
        }
    }

    LaunchedEffect(controlsVisible, uiState.videoInfo.isPlaying) {
        if (controlsVisible && uiState.videoInfo.isPlaying) {
            delay(6.seconds) // Longer delay for audio
            if (uiState.videoInfo.isPlaying) {
                controlsVisible = false
            }
        }
    }

    LaunchedEffect(uiState.videoInfo.isPlaying) {
        if (config.audioVisualization.enableVisualization) {
            while (uiState.videoInfo.isPlaying) {
                val currentTime = System.currentTimeMillis()
                visualizationTime = currentTime

                val newData = generateSmoothAudioVisualizationData(
                    currentTime,
                    uiState.videoInfo.currentPosition,
                    config.audioVisualization.visualizationType,
                    bassIntensity,
                    midIntensity,
                    trebleIntensity
                )

                audioVisualizationData = newData

                updateFrequencyIntensities(
                    currentTime,
                    onBassUpdate = { bassIntensity = it },
                    onMidUpdate = { midIntensity = it },
                    onTrebleUpdate = { trebleIntensity = it }
                )

                delay(16.milliseconds)
            }
        } else {
            audioVisualizationData = FloatArray(32) { 0f }
            bassIntensity = 0f
            midIntensity = 0f
            trebleIntensity = 0f
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d("ExoBoostAudioPlayer", "Lifecycle event: $event")
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
                            Log.e("ExoBoostAudioPlayer", "Error pausing audio player", e)
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
                            Log.e("ExoBoostAudioPlayer", "Error releasing audio player", e)
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
                Log.e("ExoBoostAudioPlayer", "Error in dispose", e)
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
                    Log.d("ExoBoostAudioPlayer", "Creating hidden PlayerView for audio")
                    PlayerView(context).apply {
                        try {
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            layoutParams = ViewGroup.LayoutParams(0, 0) // Hidden
                            setKeepContentOnPlayerReset(true)
                            setUseArtwork(false)
                            setDefaultArtwork(null)

                            val currentPlayer = playerManager.getPlayer()
                            if (currentPlayer != null) {
                                player = currentPlayer
                                Log.d("ExoBoostAudioPlayer", "Hidden PlayerView connected to audio player")
                                playerViewReady = true
                                playerManager.onSurfaceAvailable()
                            } else {
                                Log.w("ExoBoostAudioPlayer", "No player available for hidden PlayerView")
                            }
                        } catch (e: Exception) {
                            Log.e("ExoBoostAudioPlayer", "Error setting up hidden PlayerView", e)
                        }
                    }
                },
                update = { view ->
                    try {
                        val currentPlayer = playerManager.getPlayer()
                        if (currentPlayer != null && view.player !== currentPlayer) {
                            view.player = currentPlayer
                            Log.d("ExoBoostAudioPlayer", "Hidden PlayerView updated with new player")

                            if (!playerViewReady) {
                                playerViewReady = true
                                playerManager.onSurfaceAvailable()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ExoBoostAudioPlayer", "Error updating hidden PlayerView", e)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.8f + bassIntensity * 0.2f),
                            Color.Blue.copy(alpha = midIntensity * 0.1f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        if (config.audioVisualization.enableVisualization &&
            audioVisualizationData.isNotEmpty()) {
            AudioVisualization(
                audioData = audioVisualizationData,
                visualizationType = config.audioVisualization.visualizationType,
                colorScheme = config.audioVisualization.colorScheme,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (uiState.videoInfo.isPlaying) 0.9f else 0.3f)
            )
        }

        if (controlsVisible && isPlayerInitialized && playerViewReady) {
            GlassyAudioControls(
                isPlaying = uiState.videoInfo.isPlaying,
                onPlayPause = {
                    try {
                        viewModel.playPause()
                        controlsVisible = true
                    } catch (e: Exception) {
                        Log.e("ExoBoostAudioPlayer", "Error in playPause", e)
                    }
                },
                onSeek = { position ->
                    try {
                        viewModel.seekTo(position)
                    } catch (e: Exception) {
                        Log.e("ExoBoostAudioPlayer", "Error seeking", e)
                    }
                },
                currentPosition = uiState.videoInfo.currentPosition,
                duration = uiState.videoInfo.duration,
                bufferedPosition = uiState.videoInfo.bufferedPosition,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f),
                config = config.glassyUI,
                onSkipPrevious = null,
                onSkipNext = null,
                onVolumeChange = { volume ->
                    try {
                        viewModel.setVolume(volume)
                    } catch (e: Exception) {
                        Log.e("ExoBoostAudioPlayer", "Error setting volume", e)
                    }
                },
                volume = uiState.volume,
                trackTitle = "Audio Track",
                artistName = null
            )
        }

        onBack?.let {
            IconButton(
                onClick = {
                    try {
                        it.invoke()
                    } catch (e: Exception) {
                        Log.e("ExoBoostAudioPlayer", "Error handling back button", e)
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
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

private fun generateSmoothAudioVisualizationData(
    currentTime: Long,
    playbackPosition: Long,
    visualizationType: VisualizationType,
    bassIntensity: Float,
    midIntensity: Float,
    trebleIntensity: Float
): FloatArray {
    val dataSize = when (visualizationType) {
        VisualizationType.SPECTRUM -> 64
        VisualizationType.WAVEFORM -> 128
        VisualizationType.CIRCULAR -> 48
        VisualizationType.BARS -> 32
        VisualizationType.PARTICLE_SYSTEM -> 24
    }

    val timeSeconds = currentTime / 1000.0f
    val musicTime = playbackPosition / 1000.0f

    return FloatArray(dataSize) { index ->
        val frequency = index.toFloat() / dataSize.toFloat()

        val bassResponse = when {
            frequency < 0.25f -> bassIntensity * (1f - frequency * 2f)
            else -> 0f
        }

        val midResponse = when {
            frequency in 0.25f..0.75f -> midIntensity * (1f - abs(frequency - 0.5f) * 2f) // Mid range
            else -> 0f
        }

        val trebleResponse = when {
            frequency > 0.75f -> trebleIntensity * (frequency - 0.75f) * 4f
            else -> 0f
        }

        val dynamicComponent = sin(musicTime * (1f + frequency * 3f)) *
                cos(timeSeconds * (2f + frequency * 1.5f)) *
                (0.3f + bassIntensity * 0.4f + midIntensity * 0.2f + trebleIntensity * 0.1f)

        val amplitude =
            (bassResponse + midResponse + trebleResponse + dynamicComponent * 0.3f).coerceIn(0f, 1f)

        val noise = Random.nextFloat() * 0.05f

        (amplitude + noise).coerceIn(0f, 1f)
    }
}

private fun updateFrequencyIntensities(
    currentTime: Long,
    onBassUpdate: (Float) -> Unit,
    onMidUpdate: (Float) -> Unit,
    onTrebleUpdate: (Float) -> Unit
) {
    val timeSeconds = currentTime / 1000.0f

    val bassPattern = abs(sin(timeSeconds * 0.8f) * cos(timeSeconds * 0.3f)) *
            (0.7f + sin(timeSeconds * 1.2f) * 0.3f)

    val midPattern = abs(sin(timeSeconds * 1.5f) * cos(timeSeconds * 0.7f)) *
            (0.5f + cos(timeSeconds * 0.9f) * 0.3f)

    val treblePattern = abs(sin(timeSeconds * 2.2f) * cos(timeSeconds * 1.1f)) *
            (0.4f + sin(timeSeconds * 1.8f) * 0.2f)

    onBassUpdate(bassPattern.coerceIn(0f, 1f))
    onMidUpdate(midPattern.coerceIn(0f, 1f))
    onTrebleUpdate(treblePattern.coerceIn(0f, 1f))
}