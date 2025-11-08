package dev.abbasian.exoboost.data.ai

import android.graphics.Bitmap
import android.graphics.Color
import dev.abbasian.exoboost.domain.model.MotionDirection
import dev.abbasian.exoboost.domain.model.MotionScore
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlin.math.abs

class MotionAnalysisEngine(
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "MotionAnalysisEngine"
        private const val SAMPLE_SIZE = 32
        private const val MAX_RETRIES = 3
        private const val PIXEL_SAMPLE_STEP = 4
        private const val MOTION_SMOOTHING_FACTOR = 0.8f
    }

    private var cachedPixels: IntArray? = null
    private var previousMotionScore: Float = 0f

    fun calculateMotion(
        previousFrame: Bitmap?,
        currentFrame: Bitmap,
        timestampMs: Long,
    ): MotionScore {
        if (previousFrame == null) {
            cacheCurrentFrame(currentFrame)
            previousMotionScore = 0f
            return MotionScore(timestampMs, 0f, MotionDirection.NONE)
        }

        for (attempt in 0 until MAX_RETRIES) {
            try {
                val prevPixels =
                    cachedPixels ?: run {
                        val prev = downscaleBitmap(previousFrame)
                        val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
                        prev.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
                        prev.recycle()
                        pixels
                    }

                val curr = downscaleBitmap(currentFrame)
                val currPixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
                curr.getPixels(currPixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
                curr.recycle()

                var totalDifference = 0.0
                var sampleCount = 0

                for (i in prevPixels.indices step PIXEL_SAMPLE_STEP) {
                    try {
                        val prevGray = toGrayscale(prevPixels[i])
                        val currGray = toGrayscale(currPixels[i])
                        totalDifference += abs(prevGray - currGray)
                        sampleCount++
                    } catch (e: Exception) {
                        continue
                    }
                }

                cachedPixels = currPixels

                if (sampleCount == 0) {
                    return MotionScore(timestampMs, 0f, MotionDirection.NONE)
                }

                val rawMotion = (totalDifference / (sampleCount * 255.0)).toFloat()
                val normalizedMotion = rawMotion.coerceIn(0f, 1f)
                val smoothedMotion = (
                    normalizedMotion * (1 - MOTION_SMOOTHING_FACTOR) +
                        previousMotionScore * MOTION_SMOOTHING_FACTOR
                )
                previousMotionScore = smoothedMotion

                val clampedMotion = smoothedMotion.coerceIn(0f, 1f)
                val direction = determineDirection(clampedMotion)

                return MotionScore(timestampMs, clampedMotion, direction)
            } catch (e: OutOfMemoryError) {
                logger.error(TAG, "Out of memory", e)
                clearCache()
                return MotionScore(timestampMs, 0f, MotionDirection.NONE)
            } catch (e: Exception) {
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(50)
                } else {
                    return MotionScore(
                        timestampMs,
                        previousMotionScore * 0.5f,
                        MotionDirection.MINIMAL,
                    )
                }
            }
        }

        return MotionScore(timestampMs, 0f, MotionDirection.NONE)
    }

    private fun cacheCurrentFrame(currentFrame: Bitmap) {
        try {
            val downscaled = downscaleBitmap(currentFrame)
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            downscaled.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
            downscaled.recycle()
            cachedPixels = pixels
        } catch (e: Exception) {
            cachedPixels = null
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap): Bitmap = Bitmap.createScaledBitmap(bitmap, SAMPLE_SIZE, SAMPLE_SIZE, false)

    private fun toGrayscale(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    private fun determineDirection(motion: Float): MotionDirection =
        when {
            motion < 0.1f -> MotionDirection.NONE
            motion < 0.3f -> MotionDirection.MINIMAL
            motion < 0.6f -> MotionDirection.MODERATE
            motion < 0.8f -> MotionDirection.HIGH
            else -> MotionDirection.EXTREME
        }

    fun clearCache() {
        cachedPixels = null
        previousMotionScore = 0f
    }
}
