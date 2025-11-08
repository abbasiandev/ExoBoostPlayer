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
        private const val SAMPLE_SIZE = 24
        private const val LOW_RES_SAMPLE_SIZE = 16
        private const val MAX_RETRIES = 3
        private const val PIXEL_SAMPLE_STEP = 6
        private const val LOW_RES_PIXEL_STEP = 8
        private const val MOTION_SMOOTHING_FACTOR = 0.8f
    }

    private var cachedPixels: IntArray? = null
    private var previousMotionScore: Float = 0f
    private var currentSampleSize: Int = SAMPLE_SIZE

    fun calculateMotion(
        previousFrame: Bitmap?,
        currentFrame: Bitmap,
        timestampMs: Long,
        quickMode: Boolean = false,
    ): MotionScore {
        currentSampleSize = if (quickMode) LOW_RES_SAMPLE_SIZE else SAMPLE_SIZE
        val pixelStep = if (quickMode) LOW_RES_PIXEL_STEP else PIXEL_SAMPLE_STEP

        if (previousFrame == null) {
            cacheCurrentFrame(currentFrame, quickMode)
            previousMotionScore = 0f
            return MotionScore(timestampMs, 0f, MotionDirection.NONE)
        }

        for (attempt in 0 until MAX_RETRIES) {
            try {
                val prevPixels =
                    cachedPixels ?: run {
                        val prev = downscaleBitmap(previousFrame, quickMode)
                        val pixels = IntArray(currentSampleSize * currentSampleSize)
                        prev.getPixels(
                            pixels,
                            0,
                            currentSampleSize,
                            0,
                            0,
                            currentSampleSize,
                            currentSampleSize,
                        )
                        prev.recycle()
                        pixels
                    }

                val curr = downscaleBitmap(currentFrame, quickMode)
                val currPixels = IntArray(currentSampleSize * currentSampleSize)
                curr.getPixels(
                    currPixels,
                    0,
                    currentSampleSize,
                    0,
                    0,
                    currentSampleSize,
                    currentSampleSize,
                )
                curr.recycle()

                var totalDifference = 0.0
                var sampleCount = 0

                for (i in prevPixels.indices step pixelStep) {
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

    private fun cacheCurrentFrame(
        currentFrame: Bitmap,
        quickMode: Boolean,
    ) {
        try {
            val downscaled = downscaleBitmap(currentFrame, quickMode)
            val pixels = IntArray(currentSampleSize * currentSampleSize)
            downscaled.getPixels(
                pixels,
                0,
                currentSampleSize,
                0,
                0,
                currentSampleSize,
                currentSampleSize,
            )
            downscaled.recycle()
            cachedPixels = pixels
        } catch (e: Exception) {
            cachedPixels = null
        }
    }

    private fun downscaleBitmap(
        bitmap: Bitmap,
        quickMode: Boolean,
    ): Bitmap {
        val size = if (quickMode) LOW_RES_SAMPLE_SIZE else SAMPLE_SIZE
        return Bitmap.createScaledBitmap(bitmap, size, size, false)
    }

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
