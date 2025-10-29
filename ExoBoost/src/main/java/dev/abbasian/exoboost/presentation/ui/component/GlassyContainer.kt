package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig

@Composable
fun glassyContainer(
    modifier: Modifier = Modifier,
    config: MediaPlayerConfig.GlassyUIConfig = MediaPlayerConfig.GlassyUIConfig(),
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: Dp = 16.dp,
    alignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = config.backgroundOpacity),
                                    Color.White.copy(alpha = config.backgroundOpacity * 0.5f),
                                ),
                        ),
                ).border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = config.borderOpacity),
                    shape = shape,
                )
                // prevent back layer clickable
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }.padding(contentPadding),
        contentAlignment = alignment,
    ) {
        content()
    }
}
