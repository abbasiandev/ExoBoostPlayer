package dev.abbasian.exoboost.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HighlightConfig(
    val maxHighlights: Int = 5,
    val targetDuration: Long = 180_000L,
    val minSegmentDuration: Long = 5_000L,
    val maxSegmentDuration: Long = 30_000L,
    val minHighlightScore: Float = 0.4f,
    val includeAudioAnalysis: Boolean = false,
    val includeFaceDetection: Boolean = false,
    val enableMotionAnalysis: Boolean = false,
    val enableSceneDetection: Boolean = false,
    val generateChapters: Boolean = false,
    val chapterIntervalMs: Long = 60_000L,
    val motionWeight: Float = 0.3f,
    val audioWeight: Float = 0.3f,
    val visualWeight: Float = 0.2f,
    val faceWeight: Float = 0.2f,
    val sceneChangeThreshold: Float = 0.3f,
    val highMotionThreshold: Float = 0.6f,
    val loudAudioThreshold: Float = 0.5f,
    val brightSceneThreshold: Float = 0.7f,
    val audioBufferSize: Int = 4096,
    val quickMode: Boolean = false,
    val maxAnalysisDurationMs: Long? = null,
    val adaptiveSampling: Boolean = false,
    val parallelProcessing: Boolean = true,
    val lowResolutionMode: Boolean = false,
) : Parcelable {
    init {
        require(maxHighlights > 0) { "maxHighlights must be positive" }
        require(minSegmentDuration <= maxSegmentDuration) {
            "minSegmentDuration must be <= maxSegmentDuration"
        }
        require(minHighlightScore in 0f..1f) { "minHighlightScore must be between 0 and 1" }

        val totalWeight = motionWeight + audioWeight + visualWeight + faceWeight
        require(totalWeight in 0.99f..1.01f) {
            "Scoring weights must sum to 1.0 (current: $totalWeight)"
        }

        require(sceneChangeThreshold in 0f..1f) { "sceneChangeThreshold must be between 0 and 1" }
        require(chapterIntervalMs > 0) { "chapterIntervalMs must be positive" }
    }

    companion object {
        fun fast() =
            HighlightConfig(
                maxHighlights = 5,
                minHighlightScore = 0.4f,
                includeFaceDetection = false,
                includeAudioAnalysis = false,
                generateChapters = false,
                motionWeight = 0.6f,
                audioWeight = 0.0f,
                visualWeight = 0.4f,
                faceWeight = 0.0f,
                quickMode = true,
                adaptiveSampling = true,
                parallelProcessing = true,
                lowResolutionMode = true,
            )

        fun balanced() =
            HighlightConfig(
                maxHighlights = 10,
                minHighlightScore = 0.4f,
                includeFaceDetection = false,
                includeAudioAnalysis = true,
                generateChapters = false,
                motionWeight = 0.4f,
                audioWeight = 0.4f,
                visualWeight = 0.2f,
                faceWeight = 0.0f,
                adaptiveSampling = true,
                parallelProcessing = true,
            )

        fun highQuality() =
            HighlightConfig(
                maxHighlights = 15,
                minHighlightScore = 0.3f,
                includeFaceDetection = true,
                includeAudioAnalysis = true,
                generateChapters = true,
                motionWeight = 0.3f,
                audioWeight = 0.3f,
                visualWeight = 0.2f,
                faceWeight = 0.2f,
                adaptiveSampling = false,
                parallelProcessing = true,
            )

        fun audioFocused() =
            HighlightConfig(
                maxHighlights = 10,
                minHighlightScore = 0.35f,
                audioWeight = 0.6f,
                motionWeight = 0.2f,
                visualWeight = 0.2f,
                faceWeight = 0.0f,
                includeFaceDetection = false,
                includeAudioAnalysis = true,
                loudAudioThreshold = 0.45f,
                adaptiveSampling = true,
                parallelProcessing = true,
            )

        fun motionFocused() =
            HighlightConfig(
                maxHighlights = 12,
                minHighlightScore = 0.35f,
                motionWeight = 0.6f,
                audioWeight = 0.0f,
                visualWeight = 0.3f,
                faceWeight = 0.1f,
                includeAudioAnalysis = false,
                includeFaceDetection = false,
                highMotionThreshold = 0.5f,
                adaptiveSampling = true,
                parallelProcessing = true,
            )

        fun peopleFocused() =
            HighlightConfig(
                maxHighlights = 10,
                minHighlightScore = 0.35f,
                faceWeight = 0.5f,
                motionWeight = 0.2f,
                audioWeight = 0.2f,
                visualWeight = 0.1f,
                includeFaceDetection = true,
                includeAudioAnalysis = true,
                adaptiveSampling = true,
                parallelProcessing = true,
            )

        fun minimal() =
            HighlightConfig(
                maxHighlights = 3,
                minHighlightScore = 0.3f,
                includeFaceDetection = false,
                includeAudioAnalysis = false,
                generateChapters = false,
                motionWeight = 0.6f,
                audioWeight = 0.0f,
                visualWeight = 0.4f,
                faceWeight = 0.0f,
                quickMode = true,
                adaptiveSampling = true,
                parallelProcessing = true,
                lowResolutionMode = true,
            )

        fun sceneFocused() =
            HighlightConfig(
                maxHighlights = 12,
                minHighlightScore = 0.35f,
                visualWeight = 0.5f,
                motionWeight = 0.2f,
                audioWeight = 0.2f,
                faceWeight = 0.1f,
                sceneChangeThreshold = 0.25f,
                includeAudioAnalysis = true,
                includeFaceDetection = false,
                adaptiveSampling = true,
                parallelProcessing = true,
            )

        fun stable() =
            HighlightConfig(
                maxHighlights = 5,
                minHighlightScore = 0.4f,
                includeFaceDetection = false,
                includeAudioAnalysis = true,
                generateChapters = false,
                motionWeight = 0.4f,
                audioWeight = 0.4f,
                visualWeight = 0.2f,
                faceWeight = 0.0f,
                minSegmentDuration = 5_000L,
                maxSegmentDuration = 30_000L,
                adaptiveSampling = true,
                parallelProcessing = true,
            )

        fun shortVideo() =
            HighlightConfig(
                maxHighlights = 3,
                minHighlightScore = 0.3f,
                targetDuration = 60_000L,
                minSegmentDuration = 3_000L,
                maxSegmentDuration = 15_000L,
                includeFaceDetection = false,
                includeAudioAnalysis = true,
                generateChapters = false,
                motionWeight = 0.4f,
                audioWeight = 0.4f,
                visualWeight = 0.2f,
                faceWeight = 0.0f,
                adaptiveSampling = true,
                parallelProcessing = true,
            )

        fun longVideo() =
            HighlightConfig(
                maxHighlights = 15,
                minHighlightScore = 0.45f,
                targetDuration = 300_000L,
                minSegmentDuration = 10_000L,
                maxSegmentDuration = 45_000L,
                includeFaceDetection = false,
                includeAudioAnalysis = true,
                generateChapters = true,
                chapterIntervalMs = 120_000L,
                motionWeight = 0.3f,
                audioWeight = 0.4f,
                visualWeight = 0.3f,
                faceWeight = 0.0f,
                adaptiveSampling = true,
                parallelProcessing = true,
                quickMode = true,
                maxAnalysisDurationMs = 600_000L,
            )

        fun custom(
            maxHighlights: Int = 10,
            minScore: Float = 0.4f,
            includeAudio: Boolean = true,
            includeFaces: Boolean = false,
            includeChapters: Boolean = false,
            motionWeight: Float = 0.3f,
            audioWeight: Float = 0.3f,
            visualWeight: Float = 0.2f,
            faceWeight: Float = 0.2f,
            quickMode: Boolean = false,
            adaptiveSampling: Boolean = true,
            parallelProcessing: Boolean = true,
        ) = HighlightConfig(
            maxHighlights = maxHighlights,
            minHighlightScore = minScore,
            includeAudioAnalysis = includeAudio,
            includeFaceDetection = includeFaces,
            generateChapters = includeChapters,
            motionWeight = motionWeight,
            audioWeight = audioWeight,
            visualWeight = visualWeight,
            faceWeight = faceWeight,
            quickMode = quickMode,
            adaptiveSampling = adaptiveSampling,
            parallelProcessing = parallelProcessing,
        )
    }
}
