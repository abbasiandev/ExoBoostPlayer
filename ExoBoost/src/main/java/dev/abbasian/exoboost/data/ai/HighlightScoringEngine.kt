package dev.abbasian.exoboost.data.ai

import dev.abbasian.exoboost.domain.model.AudioScore
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.HighlightReason
import dev.abbasian.exoboost.domain.model.HighlightSegment
import dev.abbasian.exoboost.domain.model.MotionScore
import dev.abbasian.exoboost.domain.model.Scene
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HighlightScoringEngine(
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "HighlightScoringEngine"
        private const val MIN_SEGMENT_DURATION = 3_000L
        private const val MOTION_BOOST_MULTIPLIER = 1.2f
        private const val AUDIO_BOOST_MULTIPLIER = 1.15f
        private const val FACE_BOOST_MULTIPLIER = 1.1f
        private const val SCENE_CHANGE_BOOST_MULTIPLIER = 1.1f
    }

    suspend fun scoreSegments(
        scenes: List<Scene>,
        motionScores: List<MotionScore>,
        audioScores: List<AudioScore>,
        faceDetections: List<Pair<Long, Boolean>>,
        config: HighlightConfig = HighlightConfig(),
    ): List<HighlightSegment> =
        withContext(Dispatchers.Default) {
            val segments = mutableListOf<HighlightSegment>()

            try {
                if (scenes.isEmpty()) {
                    logger.warning(TAG, "No scenes to score")
                    return@withContext emptyList()
                }

                val sortedMotion = motionScores.sortedBy { it.timestampMs }
                val sortedAudio = audioScores.sortedBy { it.timestampMs }
                val sortedFaces = faceDetections.sortedBy { it.first }

                scenes.forEachIndexed { index, scene ->
                    try {
                        val sceneDuration = scene.endMs - scene.startMs
                        if (sceneDuration < MIN_SEGMENT_DURATION) {
                            return@forEachIndexed
                        }

                        val motionScore =
                            calculateAverageMotionOptimized(
                                scene,
                                sortedMotion,
                            )
                        val audioScore =
                            calculateAverageAudioOptimized(
                                scene,
                                sortedAudio,
                            )
                        val visualScore = scene.changeIntensity
                        val faceScore =
                            calculateFacePresenceOptimized(
                                scene,
                                sortedFaces,
                            )

                        var totalScore = (
                            motionScore * config.motionWeight +
                                audioScore * config.audioWeight +
                                visualScore * config.visualWeight +
                                faceScore * config.faceWeight
                        )

                        if (motionScore > config.highMotionThreshold) {
                            totalScore *= MOTION_BOOST_MULTIPLIER
                        }
                        if (audioScore > config.loudAudioThreshold) {
                            totalScore *= AUDIO_BOOST_MULTIPLIER
                        }
                        if (faceScore > 0.7f) {
                            totalScore *= FACE_BOOST_MULTIPLIER
                        }
                        if (visualScore > config.sceneChangeThreshold) {
                            totalScore *= SCENE_CHANGE_BOOST_MULTIPLIER
                        }

                        totalScore = totalScore.coerceIn(0f, 1f)

                        val reason =
                            determinePrimaryReason(
                                motionScore,
                                audioScore,
                                visualScore,
                                faceScore,
                                config,
                            )

                        val features =
                            buildFeatureList(
                                motionScore,
                                audioScore,
                                visualScore,
                                faceScore,
                                scene.averageBrightness,
                                config,
                            )

                        segments.add(
                            HighlightSegment(
                                startTimeMs = scene.startMs,
                                endTimeMs = scene.endMs,
                                durationMs = sceneDuration,
                                score = totalScore,
                                reason = reason,
                                keyFeatures = features,
                            ),
                        )
                    } catch (e: Exception) {
                        logger.warning(TAG, "Failed to score scene $index", e)
                    }
                }

                logger.info(TAG, "Scored ${segments.size} segments")
                return@withContext segments
            } catch (e: Exception) {
                logger.error(TAG, "Segment scoring failed", e)
                return@withContext emptyList()
            }
        }

    private fun calculateAverageMotionOptimized(
        scene: Scene,
        sortedMotionScores: List<MotionScore>,
    ): Float {
        val relevant =
            sortedMotionScores.filter {
                it.timestampMs >= scene.startMs && it.timestampMs <= scene.endMs
            }

        return if (relevant.isEmpty()) {
            0f
        } else {
            var sum = 0f
            relevant.forEach { sum += it.motionIntensity }
            sum / relevant.size
        }
    }

    private fun calculateAverageAudioOptimized(
        scene: Scene,
        sortedAudioScores: List<AudioScore>,
    ): Float {
        val relevant =
            sortedAudioScores.filter {
                it.timestampMs >= scene.startMs && it.timestampMs <= scene.endMs
            }

        return if (relevant.isEmpty()) {
            0f
        } else {
            var sum = 0f
            relevant.forEach { sum += it.volumeLevel }
            sum / relevant.size
        }
    }

    private fun calculateFacePresenceOptimized(
        scene: Scene,
        sortedFaceDetections: List<Pair<Long, Boolean>>,
    ): Float {
        val relevant =
            sortedFaceDetections.filter {
                it.first >= scene.startMs && it.first <= scene.endMs
            }

        return if (relevant.isEmpty()) {
            0f
        } else {
            relevant.count { it.second }.toFloat() / relevant.size
        }
    }

    private fun determinePrimaryReason(
        motionScore: Float,
        audioScore: Float,
        visualScore: Float,
        faceScore: Float,
        config: HighlightConfig,
    ): HighlightReason =
        when {
            motionScore > config.highMotionThreshold -> HighlightReason.HIGH_MOTION
            audioScore > config.loudAudioThreshold -> HighlightReason.AUDIO_PEAK
            faceScore > 0.7f -> HighlightReason.FACE_ACTIVITY
            visualScore > config.sceneChangeThreshold -> HighlightReason.SCENE_CHANGE
            else -> HighlightReason.COMBINED
        }

    private fun buildFeatureList(
        motionScore: Float,
        audioScore: Float,
        visualScore: Float,
        faceScore: Float,
        brightness: Float,
        config: HighlightConfig,
    ): List<String> {
        val features = mutableListOf<String>()
        if (motionScore > 0.6f) features.add("high_motion")
        if (audioScore > config.loudAudioThreshold) features.add("loud_audio")
        if (faceScore > 0.6f) features.add("faces_detected")
        if (visualScore > config.sceneChangeThreshold) features.add("scene_change")
        if (brightness > config.brightSceneThreshold) features.add("bright_scene")
        return features
    }
}
