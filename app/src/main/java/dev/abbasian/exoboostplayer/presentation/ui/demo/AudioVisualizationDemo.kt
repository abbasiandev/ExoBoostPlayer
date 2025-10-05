package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.VisualizationColorScheme
import dev.abbasian.exoboost.domain.model.VisualizationType
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostAudioPlayer

@Composable
fun AudioVisualizationDemo(url: String, title: String, artist: String, onBack: () -> Unit) {
    var visualizationType by remember { mutableStateOf(VisualizationType.SPECTRUM) }

    ExoBoostAudioPlayer(
        audioUrl = url,
        trackTitle = title,
        artistName = artist,
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            audioVisualization = MediaPlayerConfig.AudioVisualizationConfig(
                enableVisualization = true,
                colorScheme = VisualizationColorScheme.DYNAMIC,
                sensitivity = 1.5f,
                smoothingFactor = 0.85f
            ),
            glassyUI = MediaPlayerConfig.GlassyUIConfig(
                blurRadius = 35f,
                borderOpacity = 0.45f
            )
        ),
        currentVisualizationType = visualizationType,
        onVisualizationTypeChange = { visualizationType = it },
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}