package dev.abbasian.exoboost.presentation.ui.screen

import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import dev.abbasian.exoboost.data.manager.ExoPlayerManager
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.presentation.ui.component.AudioVisualization
import dev.abbasian.exoboost.presentation.ui.component.GlassyAudioControls
import dev.abbasian.exoboost.presentation.viewmodel.VideoPlayerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    val uiState by viewModel.uiState.collectAsState()
    var audioVisualizationData by remember { mutableStateOf(floatArrayOf()) }

    LaunchedEffect(uiState.videoInfo.isPlaying) {
        if (uiState.videoInfo.isPlaying) {
            // TODO: collect
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (config.audioVisualization.enableVisualization) {
            AudioVisualization(
                audioData = audioVisualizationData,
                visualizationType = config.audioVisualization.visualizationType,
                colorScheme = config.audioVisualization.colorScheme,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.7f)
            )
        }

        // blurred background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        GlassyAudioControls(
            isPlaying = uiState.videoInfo.isPlaying,
            onPlayPause = { viewModel.playPause() },
            onSeek = { progress ->
                val position = (progress * uiState.videoInfo.duration).toLong()
                viewModel.seekTo(position)
            },
            currentPosition = uiState.videoInfo.currentPosition,
            duration = uiState.videoInfo.duration,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f),
            config = config.glassyUI
        )

        // back button
        onBack?.let {
            IconButton(
                onClick = it,
                modifier = Modifier
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.3f),
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
    }
}