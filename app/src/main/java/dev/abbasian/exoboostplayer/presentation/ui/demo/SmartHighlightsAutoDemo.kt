package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostPlayer

@Composable
fun SmartHighlightsAutoDemo(
    url: String,
    config: HighlightConfig,
    onBack: () -> Unit,
) {
    exoBoostPlayer(
        videoUrl = url,
        modifier = Modifier.fillMaxSize(),
        mediaConfig = MediaPlayerConfig(
            enableSmartHighlights = true,
            autoGenerateHighlights = true,
            highlightConfig = config,
            showControls = true,
            enableGestures = true,
            autoPlay = true,
            maxRetryCount = 3,
            glassyUI = MediaPlayerConfig.GlassyUIConfig(
                blurRadius = 30f,
                borderOpacity = 0.4f,
            ),
        ),
        onBack = onBack,
    )
}