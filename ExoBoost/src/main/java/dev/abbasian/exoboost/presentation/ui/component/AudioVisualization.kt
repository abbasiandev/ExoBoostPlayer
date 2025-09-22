package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.VisualizationColorScheme
import dev.abbasian.exoboost.domain.model.VisualizationType
import kotlin.math.*

@Composable
fun AudioVisualization(
    audioData: FloatArray,
    visualizationType: VisualizationType,
    colorScheme: VisualizationColorScheme,
    modifier: Modifier = Modifier
) {
    val animatedColors by rememberInfiniteTransition().animateColor(
        initialValue = Color.Blue,
        targetValue = Color.Magenta,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        when (visualizationType) {
            VisualizationType.SPECTRUM -> drawSpectrum(audioData, colorScheme, animatedColors)
            VisualizationType.WAVEFORM -> drawWaveform(audioData, colorScheme, animatedColors)
            VisualizationType.CIRCULAR -> drawCircularVisualization(audioData, colorScheme, animatedColors)
            VisualizationType.BARS -> drawBars(audioData, colorScheme, animatedColors)
            VisualizationType.PARTICLE_SYSTEM -> drawParticleSystem(audioData, colorScheme, animatedColors)
        }
    }
}

private fun DrawScope.drawSpectrum(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    animatedColor: Color
) {
    val barWidth = size.width / audioData.size
    val centerY = size.height / 2

    audioData.forEachIndexed { index, amplitude ->
        val barHeight = amplitude * size.height * 0.4f
        val x = index * barWidth

        val color = when (colorScheme) {
            VisualizationColorScheme.DYNAMIC -> animatedColor.copy(alpha = amplitude)
            VisualizationColorScheme.RAINBOW -> Color.hsv(
                hue = (index.toFloat() / audioData.size) * 360f,
                saturation = 1f,
                value = amplitude
            )
            else -> animatedColor
        }

        drawRect(
            color = color,
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth * 0.8f, barHeight),
            blendMode = BlendMode.Screen
        )
    }
}

private fun DrawScope.drawWaveform(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    animatedColor: Color
) {
    val path = Path()
    val centerY = size.height / 2
    val stepX = size.width / audioData.size

    path.moveTo(0f, centerY)

    audioData.forEachIndexed { index, amplitude ->
        val x = index * stepX
        val y = centerY + (amplitude * size.height * 0.3f * if (index % 2 == 0) 1 else -1)
        path.lineTo(x, y)
    }

    val color = when (colorScheme) {
        VisualizationColorScheme.DYNAMIC -> animatedColor
        VisualizationColorScheme.RAINBOW -> {
            val brush = Brush.horizontalGradient(
                colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta)
            )
            drawPath(path, brush = brush, style = Stroke(width = 3.dp.toPx()))
            return
        }
        else -> animatedColor
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCircularVisualization(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    animatedColor: Color
) {
    val center = Offset(size.width / 2, size.height / 2)
    val baseRadius = minOf(size.width, size.height) * 0.2f
    val angleStep = 360f / audioData.size

    audioData.forEachIndexed { index, amplitude ->
        val angle = (index * angleStep) * (PI / 180).toFloat()
        val radius = baseRadius + (amplitude * baseRadius * 2f)

        val startX = center.x + (baseRadius * cos(angle))
        val startY = center.y + (baseRadius * sin(angle))
        val endX = center.x + (radius * cos(angle))
        val endY = center.y + (radius * sin(angle))

        val color = when (colorScheme) {
            VisualizationColorScheme.DYNAMIC -> animatedColor.copy(alpha = amplitude)
            VisualizationColorScheme.RAINBOW -> Color.hsv(
                hue = (index.toFloat() / audioData.size) * 360f,
                saturation = 1f,
                value = 1f
            )
            else -> animatedColor
        }

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawBars(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    animatedColor: Color
) {
    val barWidth = size.width / audioData.size * 0.8f
    val spacing = size.width / audioData.size * 0.2f

    audioData.forEachIndexed { index, amplitude ->
        val barHeight = amplitude * size.height * 0.7f
        val x = index * (barWidth + spacing)
        val y = size.height - barHeight

        val color = when (colorScheme) {
            VisualizationColorScheme.DYNAMIC -> {
                val intensity = amplitude.coerceIn(0f, 1f)
                lerpColor(Color.Blue, Color.Red, intensity)
            }
            VisualizationColorScheme.RAINBOW -> Color.hsv(
                hue = (index.toFloat() / audioData.size) * 360f,
                saturation = 0.8f,
                value = 0.9f
            )
            VisualizationColorScheme.MONOCHROME -> Color.White.copy(alpha = amplitude)
            else -> animatedColor.copy(alpha = amplitude.coerceIn(0.3f, 1f))
        }

        drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

private fun DrawScope.drawParticleSystem(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    animatedColor: Color
) {
    val particleCount = (audioData.average() * 100).toInt().coerceIn(10, 200)
    val centerX = size.width / 2
    val centerY = size.height / 2

    repeat(particleCount) { index ->
        val amplitude = audioData.getOrElse(index % audioData.size) { 0f }
        val angle = (index * 360f / particleCount) * (PI / 180).toFloat()
        val distance = amplitude * minOf(size.width, size.height) * 0.4f

        val x = centerX + (distance * cos(angle))
        val y = centerY + (distance * sin(angle))
        val particleSize = (amplitude * 8f).coerceIn(2f, 12f)

        val color = when (colorScheme) {
            VisualizationColorScheme.DYNAMIC -> animatedColor.copy(alpha = amplitude)
            VisualizationColorScheme.RAINBOW -> Color.hsv(
                hue = (index.toFloat() / particleCount) * 360f,
                saturation = 1f,
                value = amplitude
            )
            VisualizationColorScheme.MONOCHROME -> Color.White.copy(alpha = amplitude * 0.8f)
            else -> animatedColor.copy(alpha = amplitude)
        }

        drawCircle(
            color = color,
            radius = particleSize,
            center = Offset(x, y),
            blendMode = BlendMode.Screen
        )
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clampedFraction,
        green = start.green + (end.green - start.green) * clampedFraction,
        blue = start.blue + (end.blue - start.blue) * clampedFraction,
        alpha = start.alpha + (end.alpha - start.alpha) * clampedFraction
    )
}