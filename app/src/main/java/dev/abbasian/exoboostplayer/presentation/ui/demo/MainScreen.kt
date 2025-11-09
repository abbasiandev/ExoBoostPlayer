package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.abbasian.exoboost.presentation.viewmodel.MediaPlayerViewModel
import dev.abbasian.exoboostplayer.presentation.Demo
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen() {
    val mediaPlayerViewModel: MediaPlayerViewModel = koinViewModel()

    var currentDemo by remember { mutableStateOf<Demo?>(null) }

    LaunchedEffect(currentDemo) {
        if (currentDemo == null) {
            mediaPlayerViewModel.resetPlayer()
        }
    }

    currentDemo?.let { demo ->
        when (demo) {
            is Demo.VideoBasic -> BasicVideoPlayerDemo(demo.url) { currentDemo = null }
            is Demo.VideoAdvanced -> AdvancedVideoPlayerDemo(demo.url) { currentDemo = null }
            is Demo.VideoErrorRecovery -> NetworkRecoveryDemo(demo.url) { currentDemo = null }
            is Demo.VideoQualityControl -> QualityControlDemo(demo.url) { currentDemo = null }
            is Demo.ErrorComparison -> ErrorComparisonDemo(demo.url) { currentDemo = null }
            is Demo.BufferVisualization -> BufferVisualizationDemo(demo.url) { currentDemo = null }
            is Demo.NetworkSimulation -> NetworkSimulationDemo(demo.url) { currentDemo = null }

            is Demo.AudioVisualization ->
                AdvancedAudioVisualizationDemo(
                    demo.url,
                    demo.title,
                    demo.artist,
                ) { currentDemo = null }

            is Demo.AudioEqualizer ->
                AudioEqualizerDemo(
                    demo.url,
                    demo.title,
                    demo.artist,
                ) { currentDemo = null }

            is Demo.AudioPlaylist -> PlaylistDemo { currentDemo = null }
        }
    } ?: DemoList(onDemoSelected = { currentDemo = it })
}
