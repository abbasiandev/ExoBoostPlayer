package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.ExoBoostPlayer

@Composable
fun ErrorRecoveryDemo(url: String, onBack: () -> Unit) {

    ExoBoostPlayer(
        videoUrl = url,
        mediaConfig = MediaPlayerConfig(
            autoPlay = true,
            showControls = true,
            retryOnError = true,
            maxRetryCount = 5,
            autoQualityOnError = true,
            preferSoftwareDecoder = false,
            bufferDurations = MediaPlayerConfig.BufferDurations(
                minBufferMs = 20000,
                maxBufferMs = 60000,
                bufferForPlaybackMs = 3000,
                bufferForPlaybackAfterRebufferMs = 6000
            )
        ),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}