package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.VisualizationColorScheme
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostAudioPlayer

@Composable
fun AudioEqualizerDemo(url: String, title: String, artist: String, onBack: () -> Unit) {

    ExoBoostAudioPlayer(
        audioUrl = url,
        trackTitle = title,
        artistName = artist,
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(
                enableVisualization = true,
                colorScheme = VisualizationColorScheme.DYNAMIC,
            )
        ),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}