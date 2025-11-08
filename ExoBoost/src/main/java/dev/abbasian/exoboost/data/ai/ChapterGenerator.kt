package dev.abbasian.exoboost.data.ai

import dev.abbasian.exoboost.domain.model.ChapterType
import dev.abbasian.exoboost.domain.model.Scene
import dev.abbasian.exoboost.domain.model.VideoChapter
import dev.abbasian.exoboost.util.ExoBoostLogger

class ChapterGenerator(
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "ChapterGenerator"
        private const val DEFAULT_MIN_INTERVAL_MS = 60_000L
        private const val MIN_VIDEO_DURATION_FOR_CHAPTERS = 60_000L
        private const val INTRO_PERCENTAGE = 0.05
        private const val OUTRO_PERCENTAGE = 0.95
        private const val MIN_CHAPTER_DURATION = 10_000L
        private const val SCENE_CHANGE_THRESHOLD = 0.5f
    }

    fun generateChapters(
        scenes: List<Scene>,
        durationMs: Long,
        minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS,
    ): List<VideoChapter> {
        val chapters = mutableListOf<VideoChapter>()

        try {
            if (durationMs < MIN_VIDEO_DURATION_FOR_CHAPTERS) {
                return emptyList()
            }

            if (scenes.isEmpty()) {
                return listOf(
                    VideoChapter(
                        startTimeMs = 0L,
                        endTimeMs = durationMs,
                        title = "Full Video",
                        chapterType = ChapterType.MAIN_CONTENT,
                        confidence = 0.5f,
                    ),
                )
            }

            val introThreshold = (durationMs * INTRO_PERCENTAGE).toLong()
            val outroThreshold = (durationMs * OUTRO_PERCENTAGE).toLong()

            if (durationMs > MIN_VIDEO_DURATION_FOR_CHAPTERS) {
                chapters.add(
                    VideoChapter(
                        startTimeMs = 0L,
                        endTimeMs = introThreshold,
                        title = "Introduction",
                        chapterType = ChapterType.INTRODUCTION,
                        confidence = 0.9f,
                    ),
                )
            }

            var chapterStart = introThreshold
            var chapterIndex = 1

            scenes.forEach { scene ->
                val isSignificantChange = scene.changeIntensity > SCENE_CHANGE_THRESHOLD
                val hasMinInterval = (scene.startMs - chapterStart) >= minIntervalMs
                val isNotInIntro = scene.startMs > introThreshold
                val isNotInOutro = scene.startMs < outroThreshold

                if (isSignificantChange && hasMinInterval && isNotInIntro && isNotInOutro) {
                    val chapterEnd = scene.startMs
                    val chapterDuration = chapterEnd - chapterStart

                    if (chapterDuration >= MIN_CHAPTER_DURATION) {
                        chapters.add(
                            VideoChapter(
                                startTimeMs = chapterStart,
                                endTimeMs = chapterEnd,
                                title = "Chapter $chapterIndex",
                                chapterType = ChapterType.MAIN_CONTENT,
                                confidence = scene.changeIntensity,
                            ),
                        )
                        chapterStart = chapterEnd
                        chapterIndex++
                    }
                }
            }

            val remainingDuration = outroThreshold - chapterStart
            if (remainingDuration >= MIN_CHAPTER_DURATION) {
                chapters.add(
                    VideoChapter(
                        startTimeMs = chapterStart,
                        endTimeMs = outroThreshold,
                        title = if (chapterIndex == 1) "Main Content" else "Chapter $chapterIndex",
                        chapterType = ChapterType.MAIN_CONTENT,
                        confidence = 0.7f,
                    ),
                )
            }

            if (durationMs > MIN_VIDEO_DURATION_FOR_CHAPTERS &&
                (durationMs - outroThreshold) >= MIN_CHAPTER_DURATION
            ) {
                chapters.add(
                    VideoChapter(
                        startTimeMs = outroThreshold,
                        endTimeMs = durationMs,
                        title = "Conclusion",
                        chapterType = ChapterType.CONCLUSION,
                        confidence = 0.9f,
                    ),
                )
            }

            val validatedChapters = validateChapters(chapters, durationMs)

            if (validatedChapters.isEmpty()) {
                return listOf(
                    VideoChapter(
                        startTimeMs = 0L,
                        endTimeMs = durationMs,
                        title = "Full Video",
                        chapterType = ChapterType.MAIN_CONTENT,
                        confidence = 0.5f,
                    ),
                )
            }

            return validatedChapters
        } catch (e: Exception) {
            logger.error(TAG, "Chapter generation failed", e)
            return listOf(
                VideoChapter(
                    startTimeMs = 0L,
                    endTimeMs = durationMs,
                    title = "Full Video",
                    chapterType = ChapterType.MAIN_CONTENT,
                    confidence = 0.5f,
                ),
            )
        }
    }

    private fun validateChapters(
        chapters: List<VideoChapter>,
        durationMs: Long,
    ): List<VideoChapter> {
        if (chapters.isEmpty()) return emptyList()

        val validated = mutableListOf<VideoChapter>()
        var previousEnd = 0L

        chapters.sortedBy { it.startTimeMs }.forEach { chapter ->
            val adjustedStart =
                if (chapter.startTimeMs < previousEnd) {
                    previousEnd
                } else {
                    chapter.startTimeMs
                }
            val adjustedEnd = minOf(chapter.endTimeMs, durationMs)

            if (adjustedEnd > adjustedStart &&
                (adjustedEnd - adjustedStart) >= MIN_CHAPTER_DURATION
            ) {
                validated.add(
                    chapter.copy(
                        startTimeMs = adjustedStart,
                        endTimeMs = adjustedEnd,
                    ),
                )
                previousEnd = adjustedEnd
            }
        }

        if (validated.isNotEmpty()) {
            val lastChapter = validated.last()
            if (lastChapter.endTimeMs < durationMs) {
                validated[validated.lastIndex] = lastChapter.copy(endTimeMs = durationMs)
            }
        }

        return validated
    }
}
