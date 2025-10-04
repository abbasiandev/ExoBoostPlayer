package dev.abbasian.exoboostplayer.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.component.ExoBoostUniversalPlayer
import dev.abbasian.exoboostplayer.presentation.ui.screen.HomeScreen
import dev.abbasian.exoboostplayer.presentation.ui.theme.ExoBoostPlayerTheme
import dev.abbasian.exoboostplayer.presentation.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ExoBoostPlayerTheme {
                VideoPlayerApp()
            }
        }
    }

    @Composable
    private fun VideoPlayerApp() {
        val uiState by mainViewModel.uiState.collectAsState()

        if (uiState.showPlayer && uiState.selectedMedia != null) {
            val selectedMedia = uiState.selectedMedia!!

            val mediaType = remember(selectedMedia.url, selectedMedia.mimeType) {
                dev.abbasian.exoboost.util.MediaUtil.getMediaType(
                    selectedMedia.url,
                    selectedMedia.mimeType
                )
            }

            val isAudio = mediaType == dev.abbasian.exoboost.util.MediaType.AUDIO
            val hasPlaylist = uiState.playlist.isNotEmpty() && isAudio

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                ExoBoostUniversalPlayer(
                    mediaUrl = uiState.selectedMedia!!.url,
                    modifier = Modifier.fillMaxSize(),
                    mediaConfig = MediaPlayerConfig(
                        autoPlay = true,
                        showControls = true,
                        enableGestures = true,
                        enableSpeedControl = true,
                        // audio visualization settings
                        audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(
                            enableVisualization = true,
                            visualizationType = dev.abbasian.exoboost.domain.model.VisualizationType.SPECTRUM,
                            colorScheme = dev.abbasian.exoboost.domain.model.VisualizationColorScheme.DYNAMIC,
                            sensitivity = 0.8f,
                            smoothingFactor = 0.7f
                        ),
                        // glassy UI for modern look
                        glassyUI = MediaPlayerConfig.GlassyUIConfig(
                            blurRadius = 20f,
                            backgroundOpacity = 0.15f,
                            borderOpacity = 0.3f,
                            cornerRadius = 16f
                        ),
                        enableQualitySelection = dev.abbasian.exoboost.util.MediaUtil.isAdaptiveStream(uiState.selectedMedia!!.url),
                        playbackSpeedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f),
                        defaultPlaybackSpeed = 1.0f,
                        maxRetryCount = 5,
                        retryOnError = true
                    ),
                    trackTitle = uiState.selectedMedia!!.title ?: "Unknown Track",
                    artistName = selectedMedia.artist ?: "Unknown Artist",
                    currentTrackIndex = if (hasPlaylist) uiState.currentTrackIndex else 0,
                    totalTracks = if (hasPlaylist) uiState.playlist.size else 1,
                    onNext = if (hasPlaylist) {{ mainViewModel.playNext() }} else null,
                    onPrevious = if (hasPlaylist) {{ mainViewModel.playPrevious() }} else null,
                    onPlayerReady = {
                        Log.d("MainActivity", "Player is ready")
                    },
                    onError = { error ->
                        mainViewModel.showError(error)
                    },
                    onBack = {
                        mainViewModel.goBack()
                    }
                )

                uiState.errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.Companion.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { mainViewModel.hideError() }) {
                                Text("باشه")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }
            }
        } else {
            HomeScreen(
                onMediaSelected = { media ->
                    mainViewModel.selectMedia(media)
                }
            )
        }
    }
}