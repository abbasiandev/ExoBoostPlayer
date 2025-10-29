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
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostAudioPlayer
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostPlayer
import dev.abbasian.exoboost.util.MediaType
import dev.abbasian.exoboost.util.MediaUtil

@Composable
fun exoBoostUniversalPlayer(
    mediaUrl: String,
    mimeType: String? = null,
    modifier: Modifier = Modifier,
    mediaConfig: MediaPlayerConfig = MediaPlayerConfig(),
    onPlayerReady: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    trackTitle: String = "Audio Track",
    artistName: String = "Unknown Artist",
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    currentTrackIndex: Int = 0,
    totalTracks: Int = 1,
) {
    val mediaType =
        remember(mediaUrl, mimeType) {
            MediaUtil.getMediaType(mediaUrl, mimeType)
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.9f),
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.9f),
                                ),
                        ),
                ),
    ) {
        when (mediaType) {
            MediaType.VIDEO -> {
                exoBoostPlayer(
                    videoUrl = mediaUrl,
                    mediaConfig = mediaConfig,
                    onPlayerReady = onPlayerReady,
                    onError = onError,
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            MediaType.AUDIO -> {
                exoBoostAudioPlayer(
                    audioUrl = mediaUrl,
                    mediaConfig = mediaConfig,
                    onPlayerReady = onPlayerReady,
                    onError = onError,
                    onBack = onBack,
                    trackTitle = trackTitle,
                    artistName = artistName,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    currentTrackIndex = currentTrackIndex,
                    totalTracks = totalTracks,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            MediaType.UNKNOWN -> {
                glassyContainer(
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text(
                        "Unsupported media format",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
