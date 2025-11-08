package dev.abbasian.exoboost.domain.usecase

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dev.abbasian.exoboost.data.ai.VideoAnalysisCoordinator
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.VideoHighlights
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.measureTimeMillis

class GenerateVideoHighlightsUseCase(
    private val context: Context,
    private val coordinator: VideoAnalysisCoordinator,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "GenerateHighlights"
    }

    private val highlightCache = mutableMapOf<String, VideoHighlights>()

    suspend fun execute(
        videoUri: Uri,
        config: HighlightConfig = HighlightConfig(),
        onProgress: ((String, Float) -> Unit)? = null,
    ): Result<VideoHighlights> =
        withContext(Dispatchers.IO) {
            val cacheKey = "${videoUri}_${config.hashCode()}"

            highlightCache[cacheKey]?.let {
                logger.info(TAG, "Returning cached highlights")
                return@withContext Result.success(it)
            }

            var result: Result<VideoHighlights>
            val analysisTime =
                measureTimeMillis {
                    try {
                        logger.info(TAG, "Starting video analysis with optimized pipeline")
                        onProgress?.invoke("Initializing", 0f)

                        val retriever = MediaMetadataRetriever()

                        try {
                            if (isRemoteUrl(videoUri)) {
                                retriever.setDataSource(videoUri.toString(), HashMap())
                            } else {
                                retriever.setDataSource(context, videoUri)
                            }
                            delay(500)
                        } catch (e: Exception) {
                            logger.error(TAG, "Failed to set data source", e)
                            result =
                                Result.failure(
                                    IllegalArgumentException(
                                        "Cannot access video. Please use a local video file.",
                                        e,
                                    ),
                                )
                            return@measureTimeMillis
                        }

                        val duration =
                            retriever
                                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull() ?: 0L

                        if (duration <= 0L) {
                            result = Result.failure(IllegalArgumentException("Invalid video duration"))
                            retriever.release()
                            return@measureTimeMillis
                        }

                        logger.info(TAG, "Video duration: ${duration}ms (${duration / 1000}s)")
                        logger.info(
                            TAG,
                            "Config: parallel=${config.parallelProcessing}, quick=${config.quickMode}",
                        )

                        val analysisResult =
                            coordinator.analyzeVideo(
                                videoUri = videoUri,
                                retriever = retriever,
                                durationMs = duration,
                                config = config,
                                onProgress = { progress ->
                                    onProgress?.invoke(progress.stage, progress.progress)
                                    logger.debug(
                                        TAG,
                                        "${progress.stage}: ${(progress.progress * 100).toInt()}%",
                                    )
                                },
                            )

                        logger.info(TAG, "Analysis results:")
                        logger.info(TAG, "  - Scenes: ${analysisResult.scenes.size}")
                        logger.info(TAG, "  - Highlights: ${analysisResult.highlights.size}")
                        logger.info(TAG, "  - Chapters: ${analysisResult.chapters.size}")
                        logger.info(TAG, "  - Audio samples: ${analysisResult.audioScores.size}")
                        logger.info(TAG, "  - Motion samples: ${analysisResult.motionScores.size}")
                        logger.info(TAG, "  - Face detections: ${analysisResult.faceDetections.size}")

                        val avgScore =
                            if (analysisResult.highlights.isNotEmpty()) {
                                analysisResult.highlights
                                    .map { it.score }
                                    .average()
                                    .toFloat()
                            } else {
                                0f
                            }

                        val totalHighlightDuration = analysisResult.highlights.sumOf { it.durationMs }

                        val videoHighlights =
                            VideoHighlights(
                                originalDuration = duration,
                                highlightDuration = totalHighlightDuration,
                                highlights = analysisResult.highlights,
                                chapters = analysisResult.chapters,
                                analysisTimeMs = 0L,
                                confidenceScore = avgScore,
                            )

                        retriever.release()
                        coordinator.clearCache()

                        highlightCache[cacheKey] = videoHighlights

                        logger.info(TAG, "Analysis complete! Average confidence: $avgScore")
                        result = Result.success(videoHighlights)
                    } catch (e: CancellationException) {
                        logger.info(TAG, "Analysis cancelled by user")
                        throw e
                    } catch (e: Exception) {
                        logger.error(TAG, "Highlight generation failed", e)
                        result = Result.failure(e)
                    }
                }

            logger.info(TAG, "Total analysis time: ${analysisTime}ms (${analysisTime / 1000}s)")

            result.map { it.copy(analysisTimeMs = analysisTime) }
        }

    fun clearCache() {
        highlightCache.clear()
        coordinator.clearCache()
    }

    fun release() {
        coordinator.release()
        highlightCache.clear()
    }

    private fun isRemoteUrl(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }
}
