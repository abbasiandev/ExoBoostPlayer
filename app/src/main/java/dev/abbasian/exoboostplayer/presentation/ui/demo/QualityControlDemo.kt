package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer

@Composable
fun QualityControlDemo(url: String, onBack: () -> Unit) {
    ExoBoostPlayer(
        videoUrl = url,
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            showControls = true,
            enableQualitySelection = true,
            enableSpeedControl = true
        ),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}