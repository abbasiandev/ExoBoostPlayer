package dev.abbasian.exoboost.domain.usecase

import android.content.Context
import dev.abbasian.exoboost.data.repository.SubtitleRepository
import dev.abbasian.exoboost.domain.model.SubtitleDownloadResult
import dev.abbasian.exoboost.domain.model.SubtitleFormat
import dev.abbasian.exoboost.domain.model.SubtitleSource
import dev.abbasian.exoboost.domain.model.SubtitleStyle
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.flow.Flow
import java.io.IOException

class ManageSubtitleUseCase(
    private val subtitleRepository: SubtitleRepository,
    private val logger: ExoBoostLogger,
    private val context: Context,
) {
    companion object {
        private const val TAG = "ManageSubtitleUseCase"
        private const val ASSETS_SUBTITLE_PATH = "subtitles"
    }

    suspend fun downloadSubtitle(track: SubtitleTrack): Result<SubtitleDownloadResult> =
        try {
            logger.debug(TAG, "Downloading subtitle: ${track.language}")

            val result =
                if (track.url.startsWith("asset:///")) {
                    logger.debug(TAG, "Loading subtitle from assets: ${track.url}")
                    val contentResult = readSubtitleFromAssets(track.url)

                    contentResult.fold(
                        onSuccess = { content ->
                            SubtitleDownloadResult.Success(
                                subtitle = track,
                                content = content,
                            )
                        },
                        onFailure = { error ->
                            SubtitleDownloadResult.Error(
                                message = "Failed to load asset subtitle: ${error.message}",
                            )
                        },
                    )
                } else {
                    subtitleRepository.downloadSubtitle(track)
                }

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

    fun createSubtitleConfiguration(
        track: SubtitleTrack,
        content: String,
    ): androidx.media3.common.MediaItem.SubtitleConfiguration {
        logger.debug(TAG, "Creating subtitle configuration for: ${track.language}")
        return subtitleRepository.createSubtitleConfiguration(track, content)
    }

    suspend fun loadSubtitlesFromAssets(): Result<List<SubtitleTrack>> {
        return try {
            val assetManager = context.assets
            val subtitleFiles =
                try {
                    assetManager.list(ASSETS_SUBTITLE_PATH)?.toList() ?: emptyList()
                } catch (e: IOException) {
                    logger.warning(TAG, "Assets subtitle folder not found or empty")
                    emptyList()
                }

            if (subtitleFiles.isEmpty()) {
                return Result.success(emptyList())
            }

            val tracks =
                subtitleFiles
                    .filter { it.endsWith(".srt") || it.endsWith(".vtt") }
                    .mapNotNull { fileName ->
                        try {
                            val format =
                                when {
                                    fileName.endsWith(".srt") -> SubtitleFormat.SRT
                                    fileName.endsWith(".vtt") -> SubtitleFormat.VTT
                                    else -> return@mapNotNull null
                                }

                            val languageCode = extractLanguageFromFilename(fileName)
                            val languageName = getLanguageName(languageCode)

                            SubtitleTrack(
                                id = "asset_$fileName",
                                language = languageName,
                                languageCode = languageCode,
                                url = "asset:///$ASSETS_SUBTITLE_PATH/$fileName",
                                format = format,
                                source = SubtitleSource.EXTERNAL,
                                isDefault = false,
                            )
                        } catch (e: Exception) {
                            logger.error(TAG, "Error processing asset file: $fileName", e)
                            null
                        }
                    }

            logger.info(TAG, "Loaded ${tracks.size} subtitle(s) from assets")
            Result.success(tracks)
        } catch (e: Exception) {
            logger.error(TAG, "Error loading subtitles from assets", e)
            Result.failure(e)
        }
    }

    suspend fun readSubtitleFromAssets(fileName: String): Result<String> =
        try {
            logger.debug(TAG, "Reading subtitle from assets: $fileName")
            val assetManager = context.assets
            val path =
                if (fileName.startsWith("asset:///")) {
                    fileName.removePrefix("asset:///")
                } else {
                    "$ASSETS_SUBTITLE_PATH/$fileName"
                }

            val content = assetManager.open(path).bufferedReader().use { it.readText() }
            logger.info(TAG, "Successfully read ${content.length} characters from $fileName")
            Result.success(content)
        } catch (e: Exception) {
            logger.error(TAG, "Error reading subtitle from assets: $fileName", e)
            Result.failure(e)
        }

    private fun extractLanguageFromFilename(fileName: String): String {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val parts = nameWithoutExt.split("_", "-")

        val lastPart = parts.lastOrNull()?.lowercase() ?: ""
        return if (lastPart.length in 2..3 && lastPart.all { it.isLetter() }) {
            lastPart
        } else {
            "en"
        }
    }

    private fun getLanguageName(code: String): String =
        when (code.lowercase()) {
            "en" -> "English"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh" -> "Chinese"
            "ar" -> "Arabic"
            else -> code.uppercase()
        }
}
