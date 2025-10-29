package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostPlayer

@Composable
fun AdvancedVideoPlayerDemo(url: String, onBack: () -> Unit) {

    exoBoostPlayer(
        videoUrl = url,
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            showControls = true,
            enableGestures = true,
            enableSpeedControl = true,
            enableQualitySelection = true,
            playbackSpeedOptions = listOf(
                0.25f,
                0.5f,
                0.75f,
                1.0f,
                1.25f,
                1.5f,
                1.75f,
                2.0f,
                2.5f,
                3.0f
            ),
            glassyUI = MediaPlayerConfig.GlassyUIConfig(
                blurRadius = 30f,
                borderOpacity = 0.4f
            )
        ),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}