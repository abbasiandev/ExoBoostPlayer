package dev.abbasian.exoboostplayer.presentation.ui.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.presentation.ui.screen.exoBoostPlayer

@Composable
fun BasicVideoPlayerDemo(
    url: String,
    onBack: () -> Unit,
) {
    exoBoostPlayer(
        videoUrl = url,
        mediaConfig =
            MediaPlayerConfig(
                autoPlay = true,
                showControls = true,
                highlightConfig = HighlightConfig.fast(),
            ),
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}
