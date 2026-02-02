package dev.abbasian.exoboost.domain.usecase

import dev.abbasian.exoboost.data.repository.SubtitleRepository
import dev.abbasian.exoboost.domain.model.SubtitleDownloadResult
import dev.abbasian.exoboost.domain.model.SubtitleStyle
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.flow.Flow

class ManageSubtitleUseCase(
    private val subtitleRepository: SubtitleRepository,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "ManageSubtitleUseCase"
    }

    suspend fun downloadSubtitle(track: SubtitleTrack): Result<SubtitleDownloadResult> =
        try {
            logger.debug(TAG, "Downloading subtitle: ${track.language}")
            val result = subtitleRepository.downloadSubtitle(track)
            when (result) {
                is SubtitleDownloadResult.Success -> {
                    logger.info(TAG, "Subtitle downloaded successfully: ${track.language}")
                }

                is SubtitleDownloadResult.Error -> {
                    logger.error(TAG, "Failed to download subtitle: ${result.message}")
                }

                else -> {}
            }
            Result.success(result)
        } catch (e: Exception) {
            logger.error(TAG, "Error downloading subtitle", e)
            Result.failure(e)
        }

    fun getAvailableSubtitles(): Flow<List<SubtitleTrack>> = subtitleRepository.getAvailableSubtitles()

    fun getCurrentSubtitle(): Flow<SubtitleTrack?> = subtitleRepository.getCurrentSubtitle()

    fun getSubtitleStyle(): Flow<SubtitleStyle> = subtitleRepository.getSubtitleStyle()

    suspend fun saveSubtitleStyle(style: SubtitleStyle): Result<Unit> =
        try {
            logger.debug(TAG, "Saving subtitle style")
            subtitleRepository.saveSubtitleStyle(style)
            logger.info(TAG, "Subtitle style saved")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error saving subtitle style", e)
            Result.failure(e)
        }

    fun getPreferredLanguages(): Flow<List<String>> = subtitleRepository.getPreferredLanguages()

    suspend fun savePreferredLanguages(languages: List<String>): Result<Unit> =
        try {
            logger.debug(TAG, "Saving preferred languages: $languages")
            subtitleRepository.savePreferredLanguages(languages)
            logger.info(TAG, "Preferred languages saved")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error saving preferred languages", e)
            Result.failure(e)
        }

    fun clearSubtitle() {
        logger.debug(TAG, "Clearing subtitle")
        subtitleRepository.clearSubtitle()
    }

    fun clearCache() {
        logger.debug(TAG, "Clearing subtitle cache")
        subtitleRepository.clearCache()
    }

    fun getCacheSize(): Long {
        val size = subtitleRepository.getCacheSize()
        logger.debug(TAG, "Subtitle cache size: $size bytes")
        return size
    }
}
