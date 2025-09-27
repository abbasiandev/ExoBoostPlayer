package dev.abbasian.exoboost.presentation.ui.screen

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.domain.model.VisualizationType
import dev.abbasian.exoboost.presentation.ui.component.AudioVisualization
import dev.abbasian.exoboost.presentation.ui.component.GlassyAudioControls
import dev.abbasian.exoboost.presentation.ui.component.GlassyContainer
import dev.abbasian.exoboost.presentation.viewmodel.VideoPlayerViewModel
import dev.abbasian.exoboost.util.EnhancedAudioVisualization
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun ExoBoostAudioPlayer(
    audioUrl: String,
    modifier: Modifier = Modifier,
    config: VideoPlayerConfig = VideoPlayerConfig(),
    onPlayerReady: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    trackTitle: String = "Audio Track",
    artistName: String = "Unknown Artist",
    viewModel: VideoPlayerViewModel = koinViewModel(),
    playerManager: ExoPlayerManager = koinInject(),
    onVisualizationTypeChange: ((VisualizationType) -> Unit)? = null,
    currentVisualizationType: VisualizationType = VisualizationType.SPECTRUM
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var isPlayerInitialized by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }
    var selectedVisualizationType by remember { mutableStateOf(currentVisualizationType) }

    // audio visualization state real time
    val audioVisualizer = remember { EnhancedAudioVisualization() }

    var showEqualizer by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.7f) }

    // player initialization
    LaunchedEffect(Unit) {
        try {
            Log.d("ExoBoostAudioPlayer", "Initializing audio player...")
            playerManager.initializePlayer(config)
            isPlayerInitialized = true
            Log.d("ExoBoostAudioPlayer", "Audio player initialized")

            launch {
                playerManager.videoState.collect { state ->
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

    LaunchedEffect(uiState.videoInfo.isPlaying, selectedVisualizationType) {
        if (config.audioVisualization.enableVisualization) {
            launch {
                while (isActive && uiState.videoInfo.isPlaying) {
                    val audioSessionId = try {
                        playerManager.getPlayer()?.audioSessionId ?: 0
                    } catch (e: Exception) {
                        0
                    }

                    audioVisualizer.updateVisualization(
                        isPlaying = true,
                        audioSessionId = audioSessionId,
                        visualizationType = selectedVisualizationType,
                        sensitivity = config.audioVisualization.sensitivity,
                        smoothingFactor = config.audioVisualization.smoothingFactor
                    )

                    delay(33.milliseconds)
                }

                if (!uiState.videoInfo.isPlaying) {
                    audioVisualizer.clearVisualization()
                }
            }
        } else {
            audioVisualizer.clearVisualization()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
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
                            audioVisualizer.release()
                        } catch (e: Exception) {
                            Log.e("ExoBoostAudioPlayer", "Error pausing", e)
                        }
                    }
                }

                Lifecycle.Event.ON_DESTROY -> {
                    if (activity?.isChangingConfigurations != true) {
                        try {
                            playerManager.release()
                            audioVisualizer.release()
                            isPlayerInitialized = false
                            playerViewReady = false
                        } catch (e: Exception) {
                            Log.e("ExoBoostAudioPlayer", "Error releasing", e)
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
                audioVisualizer.release()
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color(0xFF0A0A0A),
                        Color.Black
                    )
                )
            )
    ) {
        if (isPlayerInitialized) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        try {
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            layoutParams = ViewGroup.LayoutParams(0, 0)
                            setKeepContentOnPlayerReset(true)
                            setUseArtwork(false)
                            setDefaultArtwork(null)

                            val currentPlayer = playerManager.getPlayer()
                            if (currentPlayer != null) {
                                player = currentPlayer
                                playerViewReady = true
                                playerManager.onSurfaceAvailable()
                            }
                        } catch (e: Exception) {
                            Log.e("ExoBoostAudioPlayer", "Error setting up PlayerView", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0f)
            )
        }

        if (config.audioVisualization.enableVisualization && audioVisualizer.hasData()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 64.dp)
            ) {
                AudioVisualization(
                    audioData = audioVisualizer.getVisualizationData(),
                    visualizationType = selectedVisualizationType,
                    colorScheme = config.audioVisualization.colorScheme,
                    isPlaying = uiState.videoInfo.isPlaying,
                    bassIntensity = audioVisualizer.getBassIntensity(),
                    midIntensity = audioVisualizer.getMidIntensity(),
                    trebleIntensity = audioVisualizer.getTrebleIntensity(),
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (uiState.videoInfo.isPlaying) 0.9f else 0.2f)
                )
            }
        }

        onBack?.let {
            IconButton(
                onClick = it,
                modifier = Modifier
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // main ui layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // visualization type selector
            if (config.audioVisualization.enableVisualization) {
                VisualizationTypeSelector(
                    currentType = selectedVisualizationType,
                    onTypeSelected = { newType ->
                        selectedVisualizationType = newType
                        onVisualizationTypeChange?.invoke(newType)
                    },
                    modifier = Modifier.padding(top = 40.dp)
                )
            }

            // middle spacer for visualization area
            Box(modifier = Modifier.weight(1f))

            // bottom section
            GlassyAudioControls(
                isPlaying = uiState.videoInfo.isPlaying,
                onPlayPause = { viewModel.playPause() },
                onSeek = { position -> viewModel.seekTo(position) },
                currentPosition = uiState.videoInfo.currentPosition,
                duration = uiState.videoInfo.duration,
                bufferedPosition = uiState.videoInfo.bufferedPosition,
                volume = uiState.volume,
                onVolumeChange = { volume -> viewModel.setVolume(volume) },
                trackTitle = trackTitle,
                artistName = artistName,
                config = config.glassyUI,
                onNext = { /* handle next track */ },
                onPrevious = { /* handle previous track */ },
                showEqualizer = showEqualizer,
                onEqualizerToggle = { showEqualizer = !showEqualizer },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun VisualizationTypeSelector(
    currentType: VisualizationType,
    onTypeSelected: (VisualizationType) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassyContainer(
        config = VideoPlayerConfig.GlassyUIConfig(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Visualization Style",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(VisualizationType.values()) { type ->
                    VisualizationTypeButton(
                        type = type,
                        isSelected = currentType == type,
                        onClick = { onTypeSelected(type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualizationTypeButton(
    type: VisualizationType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val buttonText = when (type) {
        VisualizationType.SPECTRUM -> "Spectrum"
        VisualizationType.WAVEFORM -> "Waveform"
        VisualizationType.CIRCULAR -> "Circular"
        VisualizationType.BARS -> "Bars"
        VisualizationType.PARTICLE_SYSTEM -> "Particles"
    }

    Box(
        modifier = Modifier
            .background(
                brush = if (isSelected) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = if (isSelected) 0.4f else 0.2f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = buttonText,
            color = Color.White.copy(alpha = if (isSelected) 1f else 0.7f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
