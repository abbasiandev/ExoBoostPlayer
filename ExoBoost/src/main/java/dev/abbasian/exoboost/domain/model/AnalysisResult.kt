package dev.abbasian.exoboost.domain.model

data class AnalysisResult(
    val scenes: List<Scene>,
    val highlights: List<HighlightSegment>,
    val chapters: List<VideoChapter>,
    val audioScores: List<AudioScore>,
    val motionScores: List<MotionScore>,
    val faceDetections: List<Pair<Long, Boolean>>,
)