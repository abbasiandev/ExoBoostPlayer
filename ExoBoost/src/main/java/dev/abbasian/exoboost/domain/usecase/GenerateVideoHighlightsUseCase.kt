package dev.abbasian.exoboost.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import dev.abbasian.exoboost.data.ai.AudioAnalysisEngine
import dev.abbasian.exoboost.data.ai.ChapterGenerator
import dev.abbasian.exoboost.data.ai.HighlightScoringEngine
import dev.abbasian.exoboost.data.ai.MLKitFaceDetector
import dev.abbasian.exoboost.data.ai.MotionAnalysisEngine
import dev.abbasian.exoboost.data.ai.SceneDetectionEngine
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.HighlightSegment
import dev.abbasian.exoboost.domain.model.MotionScore
import dev.abbasian.exoboost.domain.model.Scene
import dev.abbasian.exoboost.domain.model.VideoHighlights
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.measureTimeMillis

class GenerateVideoHighlightsUseCase(
    private val context: Context,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "GenerateHighlights"

        private const val FRAME_INTERVAL_MS = 15000L
        private const val FACE_INTERVAL_MS = 30000L
        private const val MAX_ANALYSIS_DURATION = 300_000L

        private const val FRAME_READ_DELAY_MS = 200L
        private const val BATCH_DELAY_MS = 500L
        private const val BATCH_SIZE = 5
    }

    private val sceneDetector = SceneDetectionEngine(logger)
    private val motionAnalyzer = MotionAnalysisEngine(logger)
    private val audioAnalyzer = AudioAnalysisEngine(logger)
    private val highlightScorer = HighlightScoringEngine(logger)
    private val chapterGenerator = ChapterGenerator(logger)
    private val faceDetector = MLKitFaceDetector(logger)

    private val highlightCache = mutableMapOf<String, VideoHighlights>()

    suspend fun execute(
        videoUri: Uri,
        config: HighlightConfig = HighlightConfig(),
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
                        logger.info(TAG, "Starting video analysis")

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
                            result =
                                Result.failure(IllegalArgumentException("Invalid video duration"))
                            retriever.release()
                            return@measureTimeMillis
                        }

                        logger.info(TAG, "Video duration: ${duration}ms")

                        logger.info(TAG, "Step 1/5: Detecting scenes...")
                        val scenes = sceneDetector.detectScenes(videoUri, retriever, duration)
                        delay(500)

                        logger.info(TAG, "Step 2/5: Analyzing motion...")
                        val motionScores = analyzeMotionOptimized(retriever, duration)
                        delay(500)

                        logger.info(TAG, "Step 3/5: Analyzing audio...")
                        val audioScores =
                            if (config.includeAudioAnalysis) {
                                audioAnalyzer.analyzeAudio(videoUri, duration)
                            } else {
                                emptyList()
                            }
                        delay(500)

                        logger.info(TAG, "Found ${scenes.size} scenes")
                        logger.info(TAG, "Analyzed ${motionScores.size} motion samples")
                        logger.info(TAG, "Analyzed ${audioScores.size} audio samples")

                        logger.info(TAG, "Step 4/5: Detecting faces...")
                        val faceDetections =
                            if (config.includeFaceDetection) {
                                detectFacesOptimized(retriever, duration, scenes)
                            } else {
                                emptyList()
                            }
                        logger.info(
                            TAG,
                            "Detected faces in ${faceDetections.count { it.second }} frames",
                        )

                        logger.info(TAG, "Step 5/5: Scoring highlights...")
                        val allSegments =
                            highlightScorer.scoreSegments(
                                scenes,
                                motionScores,
                                audioScores,
                                faceDetections,
                                config,
                            )

                        val selectedHighlights = selectBestHighlights(allSegments, config)
                        logger.info(TAG, "Selected ${selectedHighlights.size} highlight segments")

                        val chapters =
                            if (config.generateChapters) {
                                chapterGenerator.generateChapters(
                                    scenes,
                                    duration,
                                    config.chapterIntervalMs,
                                )
                            } else {
                                emptyList()
                            }

                        val avgScore =
                            if (selectedHighlights.isNotEmpty()) {
                                selectedHighlights.map { it.score }.average().toFloat()
                            } else {
                                0f
                            }

                        val totalHighlightDuration = selectedHighlights.sumOf { it.durationMs }

                        val videoHighlights =
                            VideoHighlights(
                                originalDuration = duration,
                                highlightDuration = totalHighlightDuration,
                                highlights = selectedHighlights,
                                chapters = chapters,
                                analysisTimeMs = 0L,
                                confidenceScore = avgScore,
                            )

                        retriever.release()
                        motionAnalyzer.clearCache()

                        highlightCache[cacheKey] = videoHighlights

                        logger.info(TAG, "Analysis complete!")
                        result = Result.success(videoHighlights)
                    } catch (e: CancellationException) {
                        logger.info(TAG, "Analysis cancelled")
                        throw e
                    } catch (e: Exception) {
                        logger.error(TAG, "Highlight generation failed", e)
                        result = Result.failure(e)
                    }
                }

            result.map { it.copy(analysisTimeMs = analysisTime) }
        }

    private suspend fun analyzeMotionOptimized(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
    ): List<MotionScore> =
        withContext(Dispatchers.Default) {
            val scores = mutableListOf<MotionScore>()
            var previousFrame: Bitmap? = null
            var consecutiveFailures = 0
            val maxConsecutiveFailures = 5

            var currentTime = 0L
            var frameCount = 0

            while (currentTime < durationMs && consecutiveFailures < maxConsecutiveFailures) {
                try {
                    if (frameCount > 0 && frameCount % BATCH_SIZE == 0) {
                        delay(BATCH_DELAY_MS)
                        logger.debug(TAG, "Batch delay after $frameCount frames")
                    }

                    delay(FRAME_READ_DELAY_MS)

                    val frame =
                        retriever.getFrameAtTime(
                            currentTime * 1000,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        )

                    if (frame != null) {
                        val score =
                            motionAnalyzer.calculateMotion(previousFrame, frame, currentTime)
                        scores.add(score)

                        previousFrame?.recycle()
                        previousFrame = frame
                        consecutiveFailures = 0
                        frameCount++
                    } else {
                        consecutiveFailures++
                        logger.warning(
                            TAG,
                            "Null frame at ${currentTime}ms (failure $consecutiveFailures)",
                        )

                        delay(500)
                    }
                } catch (e: Exception) {
                    consecutiveFailures++
                    logger.warning(
                        TAG,
                        "Motion analysis failed at ${currentTime}ms (failure $consecutiveFailures)",
                        e,
                    )

                    if (consecutiveFailures >= maxConsecutiveFailures) {
                        logger.error(TAG, "Too many consecutive failures, stopping motion analysis")
                        break
                    }

                    delay(500L * consecutiveFailures)
                }

                currentTime += FRAME_INTERVAL_MS
            }

            previousFrame?.recycle()
            logger.info(
                TAG,
                "Motion analysis complete: ${scores.size} samples, $consecutiveFailures failures",
            )
            scores
        }

    private suspend fun detectFacesOptimized(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        scenes: List<Scene>,
    ): List<Pair<Long, Boolean>> =
        withContext(Dispatchers.Default) {
            val detections = mutableListOf<Pair<Long, Boolean>>()
            var processedCount = 0

            scenes.forEach { scene ->
                val checkPoints =
                    listOf(
                        scene.startMs,
                        scene.startMs + (scene.endMs - scene.startMs) / 2,
                    )

                checkPoints.forEach { timestamp ->
                    if (timestamp < durationMs) {
                        try {
                            delay(FRAME_READ_DELAY_MS)

                            val frame =
                                retriever.getFrameAtTime(
                                    timestamp * 1000,
                                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                )

                            frame?.let {
                                val result = faceDetector.detectFaces(it)
                                detections.add(timestamp to result.hasFaces)
                                it.recycle()
                                processedCount++

                                if (processedCount % BATCH_SIZE == 0) {
                                    delay(BATCH_DELAY_MS)
                                }
                            }
                        } catch (e: Exception) {
                            logger.warning(TAG, "Face detection failed at ${timestamp}ms", e)
                            delay(500)
                        }
                    }
                }
            }

            logger.info(
                TAG,
                "Face detection complete: ${detections.size} detections from $processedCount frames",
            )
            detections
        }

    private fun selectBestHighlights(
        segments: List<HighlightSegment>,
        config: HighlightConfig,
    ): List<HighlightSegment> {
        val qualified =
            segments.filter {
                it.score >= config.minHighlightScore &&
                    it.durationMs >= config.minSegmentDuration &&
                    it.durationMs <= config.maxSegmentDuration
            }

        logger.info(TAG, "Qualified segments: ${qualified.size} out of ${segments.size}")

        val sorted = qualified.sortedByDescending { it.score }
        val selected = mutableListOf<HighlightSegment>()
        var totalDuration = 0L

        for (segment in sorted) {
            if (totalDuration + segment.durationMs <= config.targetDuration) {
                selected.add(segment)
                totalDuration += segment.durationMs
            }

            if (totalDuration >= config.targetDuration) break
        }

        return selected.sortedBy { it.startTimeMs }
    }

    fun clearCache() {
        highlightCache.clear()
    }

    fun release() {
        faceDetector.release()
        motionAnalyzer.clearCache()
        highlightCache.clear()
    }

    private fun isRemoteUrl(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }
}
