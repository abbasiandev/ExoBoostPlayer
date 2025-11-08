package dev.abbasian.exoboost.domain.model

data class VideoHighlights(
    val originalDuration: Long,
    val highlightDuration: Long,
    val highlights: List<HighlightSegment>,
    val chapters: List<VideoChapter>,
    val analysisTimeMs: Long,
    val confidenceScore: Float,
)

data class HighlightSegment(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val score: Float,
    val reason: HighlightReason,
    val keyFeatures: List<String>,
)

enum class HighlightReason {
    HIGH_MOTION,
    AUDIO_PEAK,
    SCENE_CHANGE,
    FACE_ACTIVITY,
    VISUAL_INTEREST,
    COMBINED,
}

data class VideoChapter(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val title: String,
    val chapterType: ChapterType,
    val confidence: Float,
)

enum class ChapterType {
    INTRODUCTION,
    MAIN_CONTENT,
    KEY_MOMENT,
    TRANSITION,
    CONCLUSION,
    UNKNOWN,
}
