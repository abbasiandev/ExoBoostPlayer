package dev.abbasian.exoboost.data.ai

import android.media.MediaMetadataRetriever
import android.net.Uri
import dev.abbasian.exoboost.domain.model.AnalysisProgress
import dev.abbasian.exoboost.domain.model.AnalysisResult
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.HighlightSegment
import dev.abbasian.exoboost.domain.model.MotionScore
import dev.abbasian.exoboost.domain.model.Scene
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class VideoAnalysisCoordinator(
    private val sceneDetector: SceneDetectionEngine,
    private val audioAnalyzer: AudioAnalysisEngine,
    private val motionAnalyzer: MotionAnalysisEngine,
    private val faceDetector: MLKitFaceDetector?,
    private val scoringEngine: HighlightScoringEngine,
    private val chapterGenerator: ChapterGenerator,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "VideoAnalysisCoordinator"
    }

    suspend fun analyzeVideo(
        videoUri: Uri,
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        config: HighlightConfig = HighlightConfig(),
        onProgress: ((AnalysisProgress) -> Unit)? = null,
    ): AnalysisResult =
        coroutineScope {
            try {
                logger.info(TAG, "Starting video analysis (parallel=${config.parallelProcessing})")

                if (config.parallelProcessing) {
                    analyzeVideoParallel(videoUri, retriever, durationMs, config, onProgress)
                } else {
                    analyzeVideoSequential(videoUri, retriever, durationMs, config, onProgress)
                }
            } catch (e: Exception) {
                logger.error(TAG, "Video analysis failed", e)
                AnalysisResult(
                    scenes = emptyList(),
                    highlights = emptyList(),
                    chapters = emptyList(),
                    audioScores = emptyList(),
                    motionScores = emptyList(),
                    faceDetections = emptyList(),
                )
            }
        }

    private suspend fun analyzeVideoParallel(
        videoUri: Uri,
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        config: HighlightConfig,
        onProgress: ((AnalysisProgress) -> Unit)?,
    ): AnalysisResult =
        coroutineScope {
            logger.info(TAG, "Using parallel processing mode")

            var sceneProgress = 0f
            var audioProgress = 0f
            var motionProgress = 0f

            fun updateProgress() {
                val total = (sceneProgress + audioProgress + motionProgress) / 3f
                onProgress?.invoke(AnalysisProgress("Analyzing video", total * 0.7f))
            }

            val scenesDeferred =
                async {
                    logger.info(TAG, "Starting parallel scene detection")
                    sceneDetector.detectScenes(
                        videoUri,
                        retriever,
                        durationMs,
                        config,
                    ) { progress ->
                        sceneProgress = progress
                        updateProgress()
                    }
                }

            val audioDeferred =
                async {
                    if (config.includeAudioAnalysis) {
                        logger.info(TAG, "Starting parallel audio analysis")
                        audioAnalyzer.analyzeAudio(videoUri, durationMs, config) { progress ->
                            audioProgress = progress
                            updateProgress()
                        }
                    } else {
                        logger.info(TAG, "Skipping audio analysis")
                        audioProgress = 1f
                        emptyList()
                    }
                }

            val motionDeferred =
                async {
                    if (config.enableMotionAnalysis) {
                        logger.info(TAG, "Starting parallel motion analysis")
                        analyzeMotionOptimized(retriever, durationMs, config) { progress ->
                            motionProgress = progress
                            updateProgress()
                        }
                    } else {
                        logger.info(TAG, "Skipping motion analysis")
                        motionProgress = 1f
                        emptyList()
                    }
                }

            val scenes = scenesDeferred.await()
            val audioScores = audioDeferred.await()
            val motionScores = motionDeferred.await()

            logger.info(
                TAG,
                "Parallel phase complete: ${scenes.size} scenes, ${audioScores.size} audio, ${motionScores.size} motion",
            )

            onProgress?.invoke(AnalysisProgress("Detecting faces", 0.75f))

            val faceDetections =
                if (config.includeFaceDetection && faceDetector != null) {
                    logger.info(TAG, "Starting face detection")
                    detectFacesOptimized(retriever, durationMs, scenes, config)
                } else {
                    logger.info(TAG, "Skipping face detection")
                    emptyList()
                }

            onProgress?.invoke(AnalysisProgress("Scoring highlights", 0.85f))

            val allSegments =
                scoringEngine.scoreSegments(
                    scenes,
                    motionScores,
                    audioScores,
                    faceDetections,
                    config,
                )

            val selectedHighlights = selectBestHighlights(allSegments, config)
            logger.info(
                TAG,
                "Selected ${selectedHighlights.size} highlights from ${allSegments.size} segments",
            )

            onProgress?.invoke(AnalysisProgress("Generating chapters", 0.95f))

            val chapters =
                if (config.generateChapters) {
                    logger.info(TAG, "Generating chapters")
                    chapterGenerator.generateChapters(
                        scenes,
                        durationMs,
                        config.chapterIntervalMs,
                    )
                } else {
                    emptyList()
                }

            onProgress?.invoke(AnalysisProgress("Complete", 1f))

            logger.info(
                TAG,
                "Analysis complete: ${scenes.size} scenes, ${selectedHighlights.size} highlights, ${chapters.size} chapters",
            )

            AnalysisResult(
                scenes = scenes,
                highlights = selectedHighlights,
                chapters = chapters,
                audioScores = audioScores,
                motionScores = motionScores,
                faceDetections = faceDetections,
            )
        }

    private suspend fun analyzeVideoSequential(
        videoUri: Uri,
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        config: HighlightConfig,
        onProgress: ((AnalysisProgress) -> Unit)?,
    ): AnalysisResult {
        logger.info(TAG, "Using sequential processing mode")

        onProgress?.invoke(AnalysisProgress("Detecting scenes", 0f))
        val scenes =
            sceneDetector.detectScenes(
                videoUri,
                retriever,
                durationMs,
                config,
            ) { progress ->
                onProgress?.invoke(AnalysisProgress("Detecting scenes", progress * 0.25f))
            }

        onProgress?.invoke(AnalysisProgress("Analyzing motion", 0.25f))
        val motionScores =
            if (config.enableMotionAnalysis) {
                analyzeMotionOptimized(retriever, durationMs, config) { progress ->
                    onProgress?.invoke(
                        AnalysisProgress(
                            "Analyzing motion",
                            0.25f + progress * 0.25f,
                        ),
                    )
                }
            } else {
                emptyList()
            }

        onProgress?.invoke(AnalysisProgress("Analyzing audio", 0.5f))
        val audioScores =
            if (config.includeAudioAnalysis) {
                audioAnalyzer.analyzeAudio(videoUri, durationMs, config) { progress ->
                    onProgress?.invoke(AnalysisProgress("Analyzing audio", 0.5f + progress * 0.2f))
                }
            } else {
                emptyList()
            }

        onProgress?.invoke(AnalysisProgress("Detecting faces", 0.7f))
        val faceDetections =
            if (config.includeFaceDetection && faceDetector != null) {
                detectFacesOptimized(retriever, durationMs, scenes, config)
            } else {
                emptyList()
            }

        onProgress?.invoke(AnalysisProgress("Scoring highlights", 0.85f))
        val allSegments =
            scoringEngine.scoreSegments(
                scenes,
                motionScores,
                audioScores,
                faceDetections,
                config,
            )

        val selectedHighlights = selectBestHighlights(allSegments, config)

        onProgress?.invoke(AnalysisProgress("Generating chapters", 0.95f))
        val chapters =
            if (config.generateChapters) {
                chapterGenerator.generateChapters(
                    scenes,
                    durationMs,
                    config.chapterIntervalMs,
                )
            } else {
                emptyList()
            }

        onProgress?.invoke(AnalysisProgress("Complete", 1f))

        return AnalysisResult(
            scenes = scenes,
            highlights = selectedHighlights,
            chapters = chapters,
            audioScores = audioScores,
            motionScores = motionScores,
            faceDetections = faceDetections,
        )
    }

    private suspend fun analyzeMotionOptimized(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        config: HighlightConfig,
        onProgress: ((Float) -> Unit)? = null,
    ): List<MotionScore> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val scores = mutableListOf<MotionScore>()
            var previousFrame: android.graphics.Bitmap? = null
            var consecutiveFailures = 0
            val maxConsecutiveFailures = 5

            val frameInterval = calculateMotionFrameInterval(durationMs, config)

            val analysisLimit =
                config.maxAnalysisDurationMs?.let {
                    minOf(it, durationMs)
                } ?: durationMs

            var currentTime = 0L
            var frameCount = 0
            val totalFrames = (analysisLimit / frameInterval).toInt()

            logger.info(TAG, "Motion analysis: interval=${frameInterval}ms, frames=$totalFrames")

            while (currentTime < analysisLimit && consecutiveFailures < maxConsecutiveFailures) {
                try {
                    kotlinx.coroutines.yield()

                    if (frameCount > 0 && frameCount % 5 == 0) {
                        kotlinx.coroutines.delay(100)
                    }

                    onProgress?.invoke(currentTime.toFloat() / analysisLimit)

                    kotlinx.coroutines.delay(50)

                    val frame =
                        retriever.getFrameAtTime(
                            currentTime * 1000,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        )

                    if (frame != null) {
                        val score =
                            motionAnalyzer.calculateMotion(
                                previousFrame,
                                frame,
                                currentTime,
                                config.quickMode,
                            )
                        scores.add(score)

                        previousFrame?.recycle()
                        previousFrame = frame
                        consecutiveFailures = 0
                        frameCount++
                    } else {
                        consecutiveFailures++
                        kotlinx.coroutines.delay(500)
                    }
                } catch (e: Exception) {
                    consecutiveFailures++
                    logger.warning(TAG, "Motion analysis failed at ${currentTime}ms", e)

                    if (consecutiveFailures >= maxConsecutiveFailures) {
                        logger.error(TAG, "Too many consecutive failures in motion analysis")
                        break
                    }

                    kotlinx.coroutines.delay(500L * consecutiveFailures)
                }

                currentTime += frameInterval
            }

            previousFrame?.recycle()
            onProgress?.invoke(1f)

            logger.info(TAG, "Motion analysis complete: ${scores.size} samples")
            scores
        }

    private fun calculateMotionFrameInterval(
        durationMs: Long,
        config: HighlightConfig,
    ): Long {
        if (!config.adaptiveSampling) {
            return 15_000L
        }

        return when {
            durationMs < 60_000 -> 10_000L
            durationMs < 180_000 -> 15_000L
            durationMs < 600_000 -> 20_000L
            else -> 30_000L
        }
    }

    private suspend fun detectFacesOptimized(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        scenes: List<Scene>,
        config: HighlightConfig,
    ): List<Pair<Long, Boolean>> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val detections = mutableListOf<Pair<Long, Boolean>>()
            var processedCount = 0

            val scenesToProcess =
                if (config.quickMode) {
                    scenes.filterIndexed { index, _ -> index % 2 == 0 }
                } else {
                    scenes
                }

            scenesToProcess.forEach { scene ->
                val checkPoints =
                    if (config.quickMode) {
                        listOf(scene.startMs + (scene.endMs - scene.startMs) / 2)
                    } else {
                        listOf(
                            scene.startMs,
                            scene.startMs + (scene.endMs - scene.startMs) / 2,
                        )
                    }

                checkPoints.forEach { timestamp ->
                    if (timestamp < durationMs) {
                        try {
                            kotlinx.coroutines.delay(50)

                            val frame =
                                retriever.getFrameAtTime(
                                    timestamp * 1000,
                                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                )

                            frame?.let {
                                val result = faceDetector?.detectFaces(it, config.lowResolutionMode)
                                detections.add(timestamp to (result?.hasFaces ?: false))
                                it.recycle()
                                processedCount++

                                if (processedCount % 5 == 0) {
                                    kotlinx.coroutines.delay(200)
                                }
                            }
                        } catch (e: Exception) {
                            logger.warning(TAG, "Face detection failed at ${timestamp}ms", e)
                            kotlinx.coroutines.delay(500)
                        }
                    }
                }
            }

            logger.info(TAG, "Face detection complete: ${detections.size} detections")
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
            if (selected.size >= config.maxHighlights) break

            if (totalDuration + segment.durationMs <= config.targetDuration) {
                selected.add(segment)
                totalDuration += segment.durationMs
            }

            if (totalDuration >= config.targetDuration) break
        }

        return selected.sortedBy { it.startTimeMs }
    }

    fun clearCache() {
        motionAnalyzer.clearCache()
    }

    fun release() {
        faceDetector?.release()
        motionAnalyzer.clearCache()
    }
}
