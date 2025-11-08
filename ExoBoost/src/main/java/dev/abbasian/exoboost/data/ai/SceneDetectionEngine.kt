package dev.abbasian.exoboost.data.ai

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.Scene
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class SceneDetectionEngine(
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "SceneDetectionEngine"
        private const val BASE_SAMPLE_INTERVAL_MS = 3000L
        private const val SCENE_THRESHOLD = 0.3f
        private const val FRAME_READ_DELAY_MS = 50L
        private const val BATCH_SIZE = 5
        private const val BATCH_DELAY_MS = 200L
        private const val MAX_CONSECUTIVE_FAILURES = 5
        private const val MIN_SCENE_DURATION = 3000L
        private const val HISTOGRAM_BINS = 128
        private const val LOW_RES_WIDTH = 160
        private const val LOW_RES_HEIGHT = 90
    }

    suspend fun detectScenes(
        videoUri: Uri,
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        config: HighlightConfig = HighlightConfig(),
        onProgress: ((Float) -> Unit)? = null,
    ): List<Scene> =
        withContext(Dispatchers.Default) {
            val scenes = mutableListOf<Scene>()
            var currentSceneStart = 0L
            var previousHistogram: FloatArray? = null
            var consecutiveFailures = 0
            var frameCount = 0

            try {
                val sampleInterval = calculateSampleInterval(durationMs, config)

                val analysisLimit =
                    config.maxAnalysisDurationMs?.let {
                        minOf(it, durationMs)
                    } ?: durationMs

                val timePoints =
                    if (config.quickMode) {
                        generateQuickModeTimePoints(durationMs, analysisLimit)
                    } else {
                        generateRegularTimePoints(analysisLimit, sampleInterval)
                    }

                timePoints.forEachIndexed { index, currentTime ->
                    yield()

                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        return@forEachIndexed
                    }

                    try {
                        if (frameCount > 0 && frameCount % BATCH_SIZE == 0) {
                            delay(BATCH_DELAY_MS)
                        }

                        onProgress?.invoke(index.toFloat() / timePoints.size)

                        delay(FRAME_READ_DELAY_MS)

                        val frame =
                            retriever.getFrameAtTime(
                                currentTime * 1000,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            )

                        frame?.let { bitmap ->
                            val histogram =
                                calculateColorHistogram(bitmap, config.lowResolutionMode)
                            val brightness = calculateBrightness(bitmap, config.lowResolutionMode)

                            previousHistogram?.let { prev ->
                                val similarity = calculateHistogramSimilarity(prev, histogram)

                                if (similarity < (1.0f - SCENE_THRESHOLD)) {
                                    val sceneDuration = currentTime - currentSceneStart

                                    if (sceneDuration >= MIN_SCENE_DURATION) {
                                        scenes.add(
                                            Scene(
                                                startMs = currentSceneStart,
                                                endMs = currentTime,
                                                averageBrightness = brightness,
                                                averageMotion = 0f,
                                                changeIntensity = 1.0f - similarity,
                                            ),
                                        )
                                        currentSceneStart = currentTime
                                    }
                                }
                            }

                            previousHistogram = histogram
                            bitmap.recycle()
                            consecutiveFailures = 0
                            frameCount++
                        } ?: run {
                            consecutiveFailures++
                            delay(500)
                        }
                    } catch (e: Exception) {
                        consecutiveFailures++
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            return@forEachIndexed
                        }
                        delay(500L * consecutiveFailures)
                    }
                }

                val finalSceneDuration = durationMs - currentSceneStart
                if (finalSceneDuration >= MIN_SCENE_DURATION) {
                    scenes.add(
                        Scene(
                            startMs = currentSceneStart,
                            endMs = durationMs,
                            averageBrightness = 0.5f,
                            averageMotion = 0f,
                            changeIntensity = 0f,
                        ),
                    )
                }

                if (scenes.isEmpty()) {
                    return@withContext listOf(
                        Scene(
                            startMs = 0L,
                            endMs = durationMs,
                            averageBrightness = 0.5f,
                            averageMotion = 0f,
                            changeIntensity = 0f,
                        ),
                    )
                }

                onProgress?.invoke(1f)
                logger.info(TAG, "Scene detection complete: ${scenes.size} scenes")
                scenes
            } catch (e: Exception) {
                logger.error(TAG, "Scene detection failed", e)
                listOf(
                    Scene(
                        startMs = 0L,
                        endMs = durationMs,
                        averageBrightness = 0.5f,
                        averageMotion = 0f,
                        changeIntensity = 0f,
                    ),
                )
            }
        }

    private fun calculateSampleInterval(
        durationMs: Long,
        config: HighlightConfig,
    ): Long {
        if (!config.adaptiveSampling) {
            return BASE_SAMPLE_INTERVAL_MS
        }

        return when {
            durationMs < 60_000 -> 2000L
            durationMs < 300_000 -> 3000L
            durationMs < 600_000 -> 4000L
            else -> 5000L
        }
    }

    private fun generateQuickModeTimePoints(
        durationMs: Long,
        analysisLimit: Long,
    ): List<Long> {
        val segmentDuration = (analysisLimit * 0.2).toLong()
        val step = 3000L

        val points = mutableListOf<Long>()

        var time = 0L
        while (time < segmentDuration) {
            points.add(time)
            time += step
        }

        time = (analysisLimit * 0.4).toLong()
        val middleEnd = (analysisLimit * 0.6).toLong()
        while (time < middleEnd) {
            points.add(time)
            time += step
        }

        time = (analysisLimit * 0.8).toLong()
        while (time < analysisLimit) {
            points.add(time)
            time += step
        }

        return points.sorted()
    }

    private fun generateRegularTimePoints(
        analysisLimit: Long,
        sampleInterval: Long,
    ): List<Long> {
        val points = mutableListOf<Long>()
        var time = 0L
        while (time < analysisLimit) {
            points.add(time)
            time += sampleInterval
        }
        return points
    }

    private fun calculateColorHistogram(
        bitmap: Bitmap,
        lowResMode: Boolean,
    ): FloatArray {
        val histogram = FloatArray(HISTOGRAM_BINS)

        val scaledBitmap =
            if (lowResMode) {
                Bitmap.createScaledBitmap(bitmap, LOW_RES_WIDTH, LOW_RES_HEIGHT, false)
            } else {
                Bitmap.createScaledBitmap(bitmap, 320, 180, false)
            }

        val width = scaledBitmap.width
        val height = scaledBitmap.height
        val pixels = IntArray(width * height)

        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        scaledBitmap.recycle()

        val sampleStep =
            if (lowResMode) {
                maxOf(1, pixels.size / 3000)
            } else {
                maxOf(1, pixels.size / 5000)
            }

        for (i in pixels.indices step sampleStep) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            val binIndex = (luminance * HISTOGRAM_BINS / 256).coerceIn(0, HISTOGRAM_BINS - 1)
            histogram[binIndex]++
        }

        val total = (pixels.size / sampleStep).toFloat()
        for (i in histogram.indices) {
            histogram[i] /= total
        }

        return histogram
    }

    private fun calculateHistogramSimilarity(
        hist1: FloatArray,
        hist2: FloatArray,
    ): Float {
        var sum = 0f
        val minSize = minOf(hist1.size, hist2.size)
        for (i in 0 until minSize) {
            sum += kotlin.math.min(hist1[i], hist2[i])
        }
        return sum
    }

    private fun calculateBrightness(
        bitmap: Bitmap,
        lowResMode: Boolean,
    ): Float {
        val scaledBitmap =
            if (lowResMode) {
                Bitmap.createScaledBitmap(bitmap, LOW_RES_WIDTH, LOW_RES_HEIGHT, false)
            } else {
                Bitmap.createScaledBitmap(bitmap, 320, 180, false)
            }

        val width = scaledBitmap.width
        val height = scaledBitmap.height
        val pixels = IntArray(width * height)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        scaledBitmap.recycle()

        val sampleStep =
            if (lowResMode) {
                maxOf(1, pixels.size / 2000)
            } else {
                maxOf(1, pixels.size / 3000)
            }

        var totalBrightness = 0.0
        var count = 0

        for (i in pixels.indices step sampleStep) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalBrightness += (r + g + b) / 3.0
            count++
        }

        return if (count > 0) {
            (totalBrightness / count / 255.0).toFloat()
        } else {
            0.5f
        }
    }
}
