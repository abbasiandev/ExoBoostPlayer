package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun BufferIndicator(
    currentPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    if (duration <= 0) return

    val currentProgress = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    val bufferedProgress = (bufferedPosition.toFloat() / duration).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        drawBufferIndicator(
            currentProgress = currentProgress,
            bufferedProgress = bufferedProgress,
            color = Color.White,
            backgroundColor = Color.White.copy(alpha = 0.3f)
        )
    }
}

private fun DrawScope.drawBufferIndicator(
    currentProgress: Float,
    bufferedProgress: Float,
    color: Color,
    backgroundColor: Color
) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    // Background
    drawRect(
        color = backgroundColor,
        topLeft = Offset.Zero,
        size = Size(canvasWidth, canvasHeight)
    )

    // Buffered portion
    drawRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset.Zero,
        size = Size(canvasWidth * bufferedProgress, canvasHeight)
    )

    // Current position
    drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = Size(canvasWidth * currentProgress, canvasHeight)
    )
}
