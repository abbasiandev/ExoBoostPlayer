package dev.abbasian.exoboost.presentation.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.abbasian.exoboost.domain.model.VisualizationColorScheme
import dev.abbasian.exoboost.domain.model.VisualizationType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun audioVisualization(
    audioData: FloatArray,
    visualizationType: VisualizationType,
    colorScheme: VisualizationColorScheme,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    bassIntensity: Float = 0f,
    midIntensity: Float = 0f,
    trebleIntensity: Float = 0f,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
    )

    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        when (visualizationType) {
            VisualizationType.SPECTRUM ->
                drawEnhancedSpectrum(
                    audioData,
                    colorScheme,
                    colorPhase,
                    isPlaying,
                    bassIntensity,
                    midIntensity,
                    trebleIntensity,
                )

            VisualizationType.WAVEFORM ->
                drawEnhancedWaveform(
                    audioData,
                    colorScheme,
                    colorPhase,
                    pulseAnimation,
                    isPlaying,
                )

            VisualizationType.CIRCULAR ->
                drawEnhancedCircular(
                    audioData,
                    colorScheme,
                    colorPhase,
                    bassIntensity,
                    midIntensity,
                    trebleIntensity,
                    isPlaying,
                )

            VisualizationType.BARS ->
                drawEnhancedBars(
                    audioData,
                    colorScheme,
                    colorPhase,
                    pulseAnimation,
                    isPlaying,
                )

            VisualizationType.PARTICLE_SYSTEM ->
                drawEnhancedParticleSystem(
                    audioData,
                    colorScheme,
                    colorPhase,
                    bassIntensity,
                    midIntensity,
                    trebleIntensity,
                    isPlaying,
                )
        }
    }
}

private fun DrawScope.drawEnhancedSpectrum(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    colorPhase: Float,
    isPlaying: Boolean,
    bassIntensity: Float,
    midIntensity: Float,
    trebleIntensity: Float,
) {
    if (!isPlaying || audioData.isEmpty()) return

    val barWidth = size.width / audioData.size
    val centerY = size.height / 2
    val maxBarHeight = size.height * 0.4f

    audioData.forEachIndexed { index, amplitude ->
        val normalizedIndex = index.toFloat() / audioData.size
        val barHeight = amplitude * maxBarHeight
        val x = index * barWidth

        val intensityMultiplier =
            when {
                normalizedIndex < 0.3f -> 0.7f + bassIntensity * 0.8f
                normalizedIndex < 0.7f -> 0.6f + midIntensity * 0.9f
                else -> 0.5f + trebleIntensity * 1.0f
            }

        val finalBarHeight = barHeight * intensityMultiplier

        val color =
            when (colorScheme) {
                VisualizationColorScheme.DYNAMIC -> {
                    val hue = (colorPhase + normalizedIndex * 60f) % 360f
                    Color.hsv(hue, 0.8f, 0.7f + amplitude * 0.3f)
                }

                VisualizationColorScheme.RAINBOW -> {
                    Color.hsv(
                        hue = (normalizedIndex * 300f + colorPhase * 0.5f) % 360f,
                        saturation = 0.9f,
                        value = 0.6f + amplitude * 0.4f,
                    )
                }

                VisualizationColorScheme.MONOCHROME -> {
                    Color.White.copy(alpha = 0.4f + amplitude * 0.6f)
                }

                else -> Color.hsv((colorPhase + normalizedIndex * 60f) % 360f, 0.8f, amplitude)
            }

        drawRoundRect(
            color = color,
            topLeft = Offset(x + barWidth * 0.1f, centerY - finalBarHeight / 2),
            size = Size(barWidth * 0.8f, finalBarHeight),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )

        if (amplitude > 0.7f) {
            drawRoundRect(
                color = color.copy(alpha = 0.3f),
                topLeft = Offset(x, centerY - finalBarHeight / 2 - 4.dp.toPx()),
                size = Size(barWidth, finalBarHeight + 8.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx()),
                blendMode = BlendMode.Screen,
            )
        }
    }
}

private fun DrawScope.drawEnhancedWaveform(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    colorPhase: Float,
    pulseAnimation: Float,
    isPlaying: Boolean,
) {
    if (!isPlaying || audioData.isEmpty()) return

    val path = Path()
    val centerY = size.height / 2
    val stepX = size.width / audioData.size
    val waveAmplitude = size.height * 0.3f * pulseAnimation

    path.moveTo(0f, centerY)

    audioData.forEachIndexed { index, amplitude ->
        val x = index * stepX
        val y = centerY + (amplitude * waveAmplitude * sin(index * 0.1f))

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            val prevX = (index - 1) * stepX
            val controlX = (prevX + x) / 2
            path.quadraticBezierTo(controlX, y, x, y)
        }
    }

    val gradient =
        when (colorScheme) {
            VisualizationColorScheme.RAINBOW ->
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color.hsv((colorPhase) % 360f, 0.8f, 0.9f),
                            Color.hsv((colorPhase + 60f) % 360f, 0.8f, 0.9f),
                            Color.hsv((colorPhase + 120f) % 360f, 0.8f, 0.9f),
                            Color.hsv((colorPhase + 180f) % 360f, 0.8f, 0.9f),
                        ),
                )

            VisualizationColorScheme.DYNAMIC ->
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color.hsv(colorPhase % 360f, 0.9f, 0.8f),
                            Color.hsv((colorPhase + 30f) % 360f, 0.9f, 1.0f),
                        ),
                )

            else ->
                Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0.7f)),
                )
        }

    drawPath(
        path = path,
        brush = gradient,
        style =
            Stroke(
                width = (3.dp.toPx() * pulseAnimation).coerceAtLeast(2.dp.toPx()),
                cap = StrokeCap.Round,
            ),
    )
}

private fun DrawScope.drawEnhancedCircular(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    colorPhase: Float,
    bassIntensity: Float,
    midIntensity: Float,
    trebleIntensity: Float,
    isPlaying: Boolean,
) {
    if (!isPlaying || audioData.isEmpty()) return

    val center = Offset(size.width / 2, size.height / 2)
    val baseRadius = minOf(size.width, size.height) * 0.15f
    val angleStep = 360f / audioData.size

    repeat(3) { ringIndex ->
        val ringMultiplier = 1f + ringIndex * 0.3f
        val ringAlpha = 1f - ringIndex * 0.3f

        audioData.forEachIndexed { index, amplitude ->
            val angle = (index * angleStep) * (PI / 180).toFloat()
            val normalizedIndex = index.toFloat() / audioData.size

            val intensityBoost =
                when {
                    normalizedIndex < 0.33f -> bassIntensity
                    normalizedIndex < 0.66f -> midIntensity
                    else -> trebleIntensity
                }

            val radius =
                baseRadius * ringMultiplier + (amplitude * baseRadius * 1.5f * (1f + intensityBoost))

            val startRadius = baseRadius * ringMultiplier
            val startX = center.x + (startRadius * cos(angle))
            val startY = center.y + (startRadius * sin(angle))
            val endX = center.x + (radius * cos(angle))
            val endY = center.y + (radius * sin(angle))

            val color =
                when (colorScheme) {
                    VisualizationColorScheme.DYNAMIC -> {
                        Color.hsv(
                            hue = (colorPhase + normalizedIndex * 90f + ringIndex * 30f) % 360f,
                            saturation = 0.8f,
                            value = amplitude * ringAlpha,
                        )
                    }

                    VisualizationColorScheme.RAINBOW -> {
                        Color.hsv(
                            hue = (normalizedIndex * 360f + colorPhase + ringIndex * 60f) % 360f,
                            saturation = 0.9f,
                            value = ringAlpha,
                        )
                    }

                    else -> Color.White.copy(alpha = amplitude * ringAlpha * 0.8f)
                }

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = (2.dp.toPx() + amplitude * 3.dp.toPx()) / (ringIndex + 1),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawEnhancedBars(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    colorPhase: Float,
    pulseAnimation: Float,
    isPlaying: Boolean,
) {
    if (!isPlaying || audioData.isEmpty()) return

    val barWidth = size.width / audioData.size * 0.7f
    val spacing = size.width / audioData.size * 0.3f
    val maxBarHeight = size.height * 0.8f

    audioData.forEachIndexed { index, amplitude ->
        val normalizedIndex = index.toFloat() / audioData.size
        val barHeight = amplitude * maxBarHeight * pulseAnimation
        val x = index * (barWidth + spacing) + spacing / 2
        val y = size.height - barHeight

        val color =
            when (colorScheme) {
                VisualizationColorScheme.DYNAMIC -> {
                    val baseHue = (colorPhase + normalizedIndex * 120f) % 360f
                    Color.hsv(baseHue, 0.8f, 0.7f + amplitude * 0.3f)
                }

                VisualizationColorScheme.RAINBOW -> {
                    Color.hsv(
                        hue = (normalizedIndex * 280f + colorPhase * 0.8f) % 360f,
                        saturation = 0.85f,
                        value = 0.8f + amplitude * 0.2f,
                    )
                }

                VisualizationColorScheme.MONOCHROME -> {
                    Color.White.copy(alpha = 0.5f + amplitude * 0.5f)
                }

                else -> Color.hsv((colorPhase + normalizedIndex * 120f) % 360f, 0.8f, amplitude)
            }

        drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )

        if (amplitude > 0.3f) {
            val reflectionHeight = barHeight * 0.3f * amplitude
            val reflectionGradient =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            color.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                    startY = size.height,
                    endY = size.height + reflectionHeight,
                )

            drawRoundRect(
                brush = reflectionGradient,
                topLeft = Offset(x, size.height),
                size = Size(barWidth, reflectionHeight),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}

private fun DrawScope.drawEnhancedParticleSystem(
    audioData: FloatArray,
    colorScheme: VisualizationColorScheme,
    colorPhase: Float,
    bassIntensity: Float,
    midIntensity: Float,
    trebleIntensity: Float,
    isPlaying: Boolean,
) {
    if (!isPlaying || audioData.isEmpty()) return

    val centerX = size.width / 2
    val centerY = size.height / 2
    val maxDistance = minOf(size.width, size.height) * 0.4f

    val totalIntensity = (bassIntensity + midIntensity + trebleIntensity) / 3f
    val particleCount =
        (audioData.average() * 150 * (1f + totalIntensity)).toInt().coerceIn(20, 300)

    repeat(particleCount) { index ->
        val amplitude = audioData.getOrElse(index % audioData.size) { 0f }
        val normalizedIndex = index.toFloat() / particleCount

        val baseAngle = (index * 360f / particleCount + colorPhase * 0.5f) * (PI / 180).toFloat()
        val spiralFactor = 1f + sin(normalizedIndex * PI.toFloat() * 4f) * 0.3f

        val distance = amplitude * maxDistance * spiralFactor * (1f + totalIntensity * 0.5f)
        val x = centerX + (distance * cos(baseAngle))
        val y = centerY + (distance * sin(baseAngle))

        val particleSize = (amplitude * 8f * (1f + totalIntensity * 0.5f)).coerceIn(1f, 12f)

        val color =
            when (colorScheme) {
                VisualizationColorScheme.DYNAMIC -> {
                    Color.hsv(
                        hue = (colorPhase + normalizedIndex * 180f) % 360f,
                        saturation = 0.8f + amplitude * 0.2f,
                        value = amplitude * (0.7f + totalIntensity * 0.3f),
                    )
                }

                VisualizationColorScheme.RAINBOW -> {
                    Color.hsv(
                        hue = (normalizedIndex * 360f + colorPhase) % 360f,
                        saturation = 1f,
                        value = amplitude * 0.9f,
                    )
                }

                VisualizationColorScheme.MONOCHROME -> {
                    Color.White.copy(alpha = amplitude * 0.8f * (0.5f + totalIntensity * 0.5f))
                }

                else -> Color.hsv((colorPhase + normalizedIndex * 180f) % 360f, 0.8f, amplitude)
            }

        drawCircle(
            color = color,
            radius = particleSize,
            center = Offset(x, y),
            blendMode = BlendMode.Screen,
        )

        if (amplitude > 0.6f && totalIntensity > 0.5f) {
            val trailDistance = distance * 0.8f
            val trailX = centerX + (trailDistance * cos(baseAngle))
            val trailY = centerY + (trailDistance * sin(baseAngle))

            drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = particleSize * 0.6f,
                center = Offset(trailX, trailY),
                blendMode = BlendMode.Screen,
            )
        }
    }
}
