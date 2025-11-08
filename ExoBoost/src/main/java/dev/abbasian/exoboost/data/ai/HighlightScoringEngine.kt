package dev.abbasian.exoboost.data.ai

import dev.abbasian.exoboost.domain.model.AudioScore
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.HighlightReason
import dev.abbasian.exoboost.domain.model.HighlightSegment
import dev.abbasian.exoboost.domain.model.MotionScore
import dev.abbasian.exoboost.domain.model.Scene
import dev.abbasian.exoboost.util.ExoBoostLogger

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

    fun scoreSegments(
        scenes: List<Scene>,
        motionScores: List<MotionScore>,
        audioScores: List<AudioScore>,
        faceDetections: List<Pair<Long, Boolean>>,
        config: HighlightConfig = HighlightConfig(),
    ): List<HighlightSegment> {
        val segments = mutableListOf<HighlightSegment>()

        try {
            if (scenes.isEmpty()) {
                logger.warning(TAG, "No scenes to score")
                return emptyList()
            }

            scenes.forEachIndexed { index, scene ->
                try {
                    val sceneDuration = scene.endMs - scene.startMs
                    if (sceneDuration < MIN_SEGMENT_DURATION) {
                        return@forEachIndexed
                    }

                    val motionScore = calculateAverageMotion(scene, motionScores)
                    val audioScore = calculateAverageAudio(scene, audioScores)
                    val visualScore = scene.changeIntensity
                    val faceScore = calculateFacePresence(scene, faceDetections)

                    var totalScore = (
                        motionScore * config.motionWeight +
                            audioScore * config.audioWeight +
                            visualScore * config.visualWeight +
                            faceScore * config.faceWeight
                    )

                    if (motionScore > config.highMotionThreshold) totalScore *= MOTION_BOOST_MULTIPLIER
                    if (audioScore > config.loudAudioThreshold) totalScore *= AUDIO_BOOST_MULTIPLIER
                    if (faceScore > 0.7f) totalScore *= FACE_BOOST_MULTIPLIER
                    if (visualScore > config.sceneChangeThreshold) totalScore *= SCENE_CHANGE_BOOST_MULTIPLIER

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
            return segments
        } catch (e: Exception) {
            logger.error(TAG, "Segment scoring failed", e)
            return emptyList()
        }
    }

    private fun calculateAverageMotion(
        scene: Scene,
        motionScores: List<MotionScore>,
    ): Float {
        val relevant =
            motionScores.filter { it.timestampMs >= scene.startMs && it.timestampMs <= scene.endMs }
        return if (relevant.isEmpty()) {
            0f
        } else {
            relevant
                .map { it.motionIntensity }
                .average()
                .toFloat()
        }
    }

    private fun calculateAverageAudio(
        scene: Scene,
        audioScores: List<AudioScore>,
    ): Float {
        val relevant =
            audioScores.filter { it.timestampMs >= scene.startMs && it.timestampMs <= scene.endMs }
        return if (relevant.isEmpty()) 0f else relevant.map { it.volumeLevel }.average().toFloat()
    }

    private fun calculateFacePresence(
        scene: Scene,
        faceDetections: List<Pair<Long, Boolean>>,
    ): Float {
        val relevant =
            faceDetections.filter { it.first >= scene.startMs && it.first <= scene.endMs }
        return if (relevant.isEmpty()) {
            0f
        } else {
            relevant
                .count { it.second }
                .toFloat() / relevant.size
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
