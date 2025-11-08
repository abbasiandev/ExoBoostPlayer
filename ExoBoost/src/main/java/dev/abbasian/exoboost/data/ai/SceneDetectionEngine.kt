package dev.abbasian.exoboost.data.ai

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
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
        private const val SAMPLE_INTERVAL_MS = 2000L
        private const val SCENE_THRESHOLD = 0.3f
        private const val FRAME_READ_DELAY_MS = 150L
        private const val BATCH_SIZE = 5
        private const val BATCH_DELAY_MS = 400L
        private const val MAX_CONSECUTIVE_FAILURES = 5
        private const val MIN_SCENE_DURATION = 3000L
    }

    suspend fun detectScenes(
        videoUri: Uri,
        retriever: MediaMetadataRetriever,
        durationMs: Long,
    ): List<Scene> =
        withContext(Dispatchers.Default) {
            val scenes = mutableListOf<Scene>()
            var currentSceneStart = 0L
            var previousHistogram: FloatArray? = null
            var consecutiveFailures = 0
            var frameCount = 0

            try {
                var currentTime = 0L
                while (currentTime < durationMs && consecutiveFailures < MAX_CONSECUTIVE_FAILURES) {
                    yield()

                    try {
                        if (frameCount > 0 && frameCount % BATCH_SIZE == 0) {
                            delay(BATCH_DELAY_MS)
                        }

                        delay(FRAME_READ_DELAY_MS)

                        val frame =
                            retriever.getFrameAtTime(
                                currentTime * 1000,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            )

                        frame?.let { bitmap ->
                            val histogram = calculateColorHistogram(bitmap)
                            val brightness = calculateBrightness(bitmap)

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
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) break
                        delay(500L * consecutiveFailures)
                    }

                    currentTime += SAMPLE_INTERVAL_MS
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

    private fun calculateColorHistogram(bitmap: Bitmap): FloatArray {
        val histogram = FloatArray(256)
        val width = bitmap.width
        val height = bitmap.height
        val sampleStep = maxOf(1, (width * height) / 10000)
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices step sampleStep) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            histogram[luminance.coerceIn(0, 255)]++
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
        for (i in hist1.indices) {
            sum += kotlin.math.min(hist1[i], hist2[i])
        }
        return sum
    }

    private fun calculateBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val sampleStep = maxOf(1, (width * height) / 5000)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

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

        return (totalBrightness / count / 255.0).toFloat()
    }
}
