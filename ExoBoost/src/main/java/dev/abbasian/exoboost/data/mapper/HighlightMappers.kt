package dev.abbasian.exoboost.data.mapper

import dev.abbasian.exoboost.data.local.ChapterTypeEntity
import dev.abbasian.exoboost.data.local.HighlightReasonEntity
import dev.abbasian.exoboost.data.local.HighlightSegmentEntity
import dev.abbasian.exoboost.data.local.VideoChapterEntity
import dev.abbasian.exoboost.data.local.VideoHighlightEntity
import dev.abbasian.exoboost.domain.model.ChapterType
import dev.abbasian.exoboost.domain.model.HighlightReason
import dev.abbasian.exoboost.domain.model.HighlightSegment
import dev.abbasian.exoboost.domain.model.VideoChapter
import dev.abbasian.exoboost.domain.model.VideoHighlights

fun VideoHighlights.toEntity(videoUrl: String): VideoHighlightEntity {
    val totalHighlightDuration = highlights.sumOf { it.durationMs }

    return VideoHighlightEntity(
        videoUrl = videoUrl,
        originalDuration = this.originalDuration,
        highlightDuration = this.highlightDuration,
        highlights = this.highlights.map { it.toEntity() },
        chapters = this.chapters.map { it.toEntity() },
        analysisTimeMs = this.analysisTimeMs,
        confidenceScore = this.confidenceScore,
    )
}

fun HighlightSegment.toEntity(): HighlightSegmentEntity {
    return HighlightSegmentEntity(
        startTimeMs = this.startTimeMs,
        endTimeMs = this.endTimeMs,
        durationMs = this.durationMs,
        score = this.score,
        reason = this.reason.toEntity(),
        keyFeatures = this.keyFeatures,
    )
}

fun VideoChapter.toEntity(): VideoChapterEntity {
    return VideoChapterEntity(
        startTimeMs = this.startTimeMs,
        endTimeMs = this.endTimeMs,
        title = this.title,
        chapterType = this.chapterType.toEntity(),
        confidence = this.confidence,
    )
}

fun HighlightReason.toEntity(): HighlightReasonEntity {
    return when (this) {
        HighlightReason.HIGH_MOTION -> HighlightReasonEntity.HIGH_MOTION
        HighlightReason.AUDIO_PEAK -> HighlightReasonEntity.AUDIO_PEAK
        HighlightReason.SCENE_CHANGE -> HighlightReasonEntity.SCENE_CHANGE
        HighlightReason.FACE_ACTIVITY -> HighlightReasonEntity.FACE_ACTIVITY
        HighlightReason.VISUAL_INTEREST -> HighlightReasonEntity.VISUAL_INTEREST
        HighlightReason.COMBINED -> HighlightReasonEntity.COMBINED
    }
}

fun ChapterType.toEntity(): ChapterTypeEntity {
    return when (this) {
        ChapterType.INTRODUCTION -> ChapterTypeEntity.INTRODUCTION
        ChapterType.MAIN_CONTENT -> ChapterTypeEntity.MAIN_CONTENT
        ChapterType.KEY_MOMENT -> ChapterTypeEntity.KEY_MOMENT
        ChapterType.TRANSITION -> ChapterTypeEntity.TRANSITION
        ChapterType.CONCLUSION -> ChapterTypeEntity.CONCLUSION
        ChapterType.UNKNOWN -> ChapterTypeEntity.UNKNOWN
    }
}

fun VideoHighlightEntity.toDomain(): VideoHighlights {
    return VideoHighlights(
        originalDuration = this.originalDuration,
        highlightDuration = this.highlightDuration,
        highlights = this.highlights.map { it.toDomain() },
        chapters = this.chapters.map { it.toDomain() },
        analysisTimeMs = this.analysisTimeMs,
        confidenceScore = this.confidenceScore,
    )
}

fun HighlightSegmentEntity.toDomain(): HighlightSegment {
    return HighlightSegment(
        startTimeMs = this.startTimeMs,
        endTimeMs = this.endTimeMs,
        durationMs = this.durationMs,
        score = this.score,
        reason = this.reason.toDomain(),
        keyFeatures = this.keyFeatures,
    )
}

fun VideoChapterEntity.toDomain(): VideoChapter {
    return VideoChapter(
        startTimeMs = this.startTimeMs,
        endTimeMs = this.endTimeMs,
        title = this.title,
        chapterType = this.chapterType.toDomain(),
        confidence = this.confidence,
    )
}

fun HighlightReasonEntity.toDomain(): HighlightReason {
    return when (this) {
        HighlightReasonEntity.HIGH_MOTION -> HighlightReason.HIGH_MOTION
        HighlightReasonEntity.AUDIO_PEAK -> HighlightReason.AUDIO_PEAK
        HighlightReasonEntity.SCENE_CHANGE -> HighlightReason.SCENE_CHANGE
        HighlightReasonEntity.FACE_ACTIVITY -> HighlightReason.FACE_ACTIVITY
        HighlightReasonEntity.VISUAL_INTEREST -> HighlightReason.VISUAL_INTEREST
        HighlightReasonEntity.COMBINED -> HighlightReason.COMBINED
    }
}

fun ChapterTypeEntity.toDomain(): ChapterType {
    return when (this) {
        ChapterTypeEntity.INTRODUCTION -> ChapterType.INTRODUCTION
        ChapterTypeEntity.MAIN_CONTENT -> ChapterType.MAIN_CONTENT
        ChapterTypeEntity.KEY_MOMENT -> ChapterType.KEY_MOMENT
        ChapterTypeEntity.TRANSITION -> ChapterType.TRANSITION
        ChapterTypeEntity.CONCLUSION -> ChapterType.CONCLUSION
        ChapterTypeEntity.UNKNOWN -> ChapterType.UNKNOWN
    }
}
