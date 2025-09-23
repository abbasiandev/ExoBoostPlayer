package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostAudioPlayer
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer
import dev.abbasian.exoboost.util.MediaType
import dev.abbasian.exoboost.util.MediaUtil

@Composable
fun ExoBoostUniversalPlayer(
    mediaUrl: String,
    mimeType: String? = null,
    modifier: Modifier = Modifier,
    config: VideoPlayerConfig = VideoPlayerConfig(),
    onPlayerReady: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val mediaType = remember(mediaUrl, mimeType) {
        MediaUtil.getMediaType(mediaUrl, mimeType)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        when (mediaType) {
            MediaType.VIDEO -> {
                ExoBoostPlayer(
                    videoUrl = mediaUrl,
                    config = config,
                    onPlayerReady = onPlayerReady,
                    onError = onError,
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaType.AUDIO -> {
                ExoBoostAudioPlayer(
                    audioUrl = mediaUrl,
                    config = config,
                    onPlayerReady = onPlayerReady,
                    onError = onError,
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaType.UNKNOWN -> {
                GlassyContainer(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        "Unsupported media format",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}