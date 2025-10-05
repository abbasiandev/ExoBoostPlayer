package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer

@Composable
fun BasicVideoPlayerDemo(url: String, onBack: () -> Unit) {

    ExoBoostPlayer(
        videoUrl = url,
        mediaConfig = MediaPlayerConfig(autoPlay = true, showControls = true),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}