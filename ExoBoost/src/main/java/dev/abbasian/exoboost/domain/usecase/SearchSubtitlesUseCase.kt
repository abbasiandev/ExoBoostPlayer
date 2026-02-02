package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.data.repository.SubtitleRepository
import dev.abbasian.exoboost.domain.model.SubtitleQuery
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.util.ExoBoostLogger

class SearchSubtitlesUseCase(
    private val subtitleRepository: SubtitleRepository,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "SearchSubtitlesUseCase"
    }

    suspend fun execute(query: SubtitleQuery): Result<List<SubtitleTrack>> =
        try {
            logger.debug(TAG, "Searching subtitles: ${query.videoName}")
            val subtitles = subtitleRepository.searchSubtitles(query)
            logger.info(TAG, "Found ${subtitles.size} subtitles")
            Result.success(subtitles)
        } catch (e: Exception) {
            logger.error(TAG, "Error searching subtitles", e)
            Result.failure(e)
        }

    suspend fun autoSearch(
        videoUrl: String,
        videoName: String? = null,
    ): Result<List<SubtitleTrack>> =
        try {
            logger.debug(TAG, "Auto-searching subtitles for: $videoUrl")
            val subtitles = subtitleRepository.autoSearchSubtitles(videoUrl, videoName)
            logger.info(TAG, "Found ${subtitles.size} subtitles")
            Result.success(subtitles)
        } catch (e: Exception) {
            logger.error(TAG, "Error auto-searching subtitles", e)
            Result.failure(e)
        }
}
