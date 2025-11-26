package dev.abbasian.exoboost.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import dev.abbasian.exoboost.data.local.converter.HighlightConverters

@Entity(tableName = "video_highlights")
@TypeConverters(HighlightConverters::class)
data class VideoHighlightEntity(
    @PrimaryKey
    val videoUrl: String,
    val originalDuration: Long,
    val highlightDuration: Long,
    val highlights: List<HighlightSegmentEntity>,
    val chapters: List<VideoChapterEntity>,
    val analysisTimeMs: Long,
    val confidenceScore: Float,
    val generatedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
)

data class HighlightSegmentEntity(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val score: Float,
    val reason: HighlightReasonEntity,
    val keyFeatures: List<String>,
)

enum class HighlightReasonEntity {
    HIGH_MOTION,
    AUDIO_PEAK,
    SCENE_CHANGE,
    FACE_ACTIVITY,
    VISUAL_INTEREST,
    COMBINED,
}

data class VideoChapterEntity(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val title: String,
    val chapterType: ChapterTypeEntity,
    val confidence: Float,
)

enum class ChapterTypeEntity {
    INTRODUCTION,
    MAIN_CONTENT,
    KEY_MOMENT,
    TRANSITION,
    CONCLUSION,
    UNKNOWN,
}
