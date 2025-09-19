package dev.abbasian.exoboostplayer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer
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

        if (uiState.showPlayer && uiState.selectedVideo != null) {
            // Full-screen video player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                ExoBoostPlayer(
                    videoUrl = uiState.selectedVideo!!.url,
                    config = VideoPlayerConfig(
                        enableCache = true,
                        autoPlay = true,
                        showControls = true,
                        enableGestures = true,
                        retryOnError = true,
                        maxRetryCount = 3
                    ),
                    onPlayerReady = {
                        // Player is ready
                    },
                    onError = { error ->
                        mainViewModel.showError(error)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Back button
                FilledTonalButton(
                    onClick = { mainViewModel.goBack() },
                    modifier = Modifier
                        .align(Alignment.Companion.TopStart)
                        .padding(16.dp)
                ) {
                    Text("بازگشت")
                }

                // Error display
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
            // Home screen with video selection
            HomeScreen(
                onVideoSelected = { video ->
                    mainViewModel.selectVideo(video)
                }
            )
        }
    }
}