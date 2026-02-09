package dev.abbasian.exoboost.data.manager

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import dev.abbasian.exoboost.data.api.sources.SubtitleApiAggregator
import dev.abbasian.exoboost.data.parser.SubtitleParser
import dev.abbasian.exoboost.domain.model.ParsedSubtitle
import dev.abbasian.exoboost.domain.model.SubtitleConfig
import dev.abbasian.exoboost.domain.model.SubtitleDownloadResult
import dev.abbasian.exoboost.domain.model.SubtitleFormat
import dev.abbasian.exoboost.domain.model.SubtitleQuery
import dev.abbasian.exoboost.domain.model.SubtitleSource
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

class SubtitleManager(
    private val context: Context,
    private val subtitleApiAggregator: SubtitleApiAggregator,
    private val subtitleParser: SubtitleParser,
    private val client: OkHttpClient,
    private val logger: ExoBoostLogger,
    private val config: SubtitleConfig = SubtitleConfig(),
) {
    companion object {
        private const val TAG = "SubtitleManager"
        private const val SUBTITLE_CACHE_DIR = "subtitle_cache"
    }

    private val _availableSubtitles = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val availableSubtitles: StateFlow<List<SubtitleTrack>> = _availableSubtitles.asStateFlow()

    private val _currentSubtitle = MutableStateFlow<SubtitleTrack?>(null)
    val currentSubtitle: StateFlow<SubtitleTrack?> = _currentSubtitle.asStateFlow()

    private val cacheDir: File by lazy {
        File(context.cacheDir, SUBTITLE_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun searchSubtitles(query: SubtitleQuery): List<SubtitleTrack> =
        withContext(Dispatchers.IO) {
            try {
                logger.debug(TAG, "Searching subtitles for: ${query.videoName}")

                val results = subtitleApiAggregator.searchAllSources(query)
                _availableSubtitles.value = results

                logger.debug(TAG, "Found ${results.size} subtitle tracks")
                results
            } catch (e: Exception) {
                logger.error(TAG, "Error searching subtitles", e)
                emptyList()
            }
        }

    suspend fun autoSearchSubtitles(
        videoUrl: String,
        videoName: String? = null,
    ): List<SubtitleTrack> {
        val name = videoName ?: extractVideoName(videoUrl)
        val query =
            SubtitleQuery(
                videoName = name,
                language = config.preferredLanguages.firstOrNull() ?: "en",
            )

        return searchSubtitles(query)
    }

    suspend fun downloadSubtitle(track: SubtitleTrack): SubtitleDownloadResult =
        withContext(Dispatchers.IO) {
            try {
                logger.debug(TAG, "Downloading subtitle: ${track.id}")

                if (config.cacheSubtitles) {
                    val cachedContent = getCachedSubtitle(track)
                    if (cachedContent != null) {
                        logger.debug(TAG, "Using cached subtitle")
                        return@withContext SubtitleDownloadResult.Success(track, cachedContent)
                    }
                }

                val content =
                    when (track.source) {
                        SubtitleSource.EXTERNAL -> {
                            getCachedSubtitle(track)
                                ?: return@withContext SubtitleDownloadResult.Error(
                                    "External subtitle not found in cache. Please reload the file.",
                                )
                        }

                        SubtitleSource.EMBEDDED, SubtitleSource.CUSTOM -> {
                            downloadFromUrl(track.url)
                        }

                        else -> {
                            subtitleApiAggregator.downloadSubtitle(track)
                        }
                    }

                if (content == null) {
                    return@withContext SubtitleDownloadResult.Error("Failed to download subtitle")
                }

                if (config.cacheSubtitles) {
                    cacheSubtitle(track, content)
                }

                _currentSubtitle.value = track
                SubtitleDownloadResult.Success(track, content)
            } catch (e: Exception) {
                logger.error(TAG, "Error downloading subtitle", e)
                SubtitleDownloadResult.Error("Download failed: ${e.message}", e)
            }
        }

    fun parseSubtitle(
        content: String,
        format: SubtitleFormat,
    ): ParsedSubtitle? =
        try {
            subtitleParser.parse(content, format)
        } catch (e: Exception) {
            logger.error(TAG, "Error parsing subtitle", e)
            null
        }

    fun createSubtitleConfiguration(
        track: SubtitleTrack,
        content: String,
    ): MediaItem.SubtitleConfiguration {
        val subtitleFile = File(cacheDir, "${track.id}.${track.format.extension}")

        if (!subtitleFile.exists()) {
            subtitleFile.writeText(content)
            logger.debug(TAG, "Cached new subtitle file: ${track.id}")
        } else {
            logger.debug(TAG, "Using existing cached subtitle: ${track.id}")
        }

        return MediaItem.SubtitleConfiguration
            .Builder(Uri.fromFile(subtitleFile))
            .setMimeType(track.format.mimeType)
            .setLanguage(track.languageCode)
            .setSelectionFlags(
                if (track.isDefault) {
                    androidx.media3.common.C.SELECTION_FLAG_DEFAULT
                } else {
                    0
                },
            ).build()
    }

    suspend fun loadExternalSubtitle(
        uri: Uri,
        language: String = "Unknown",
    ): SubtitleDownloadResult =
        withContext(Dispatchers.IO) {
            try {
                val content =
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: return@withContext SubtitleDownloadResult.Error("Failed to read file")

                val format = subtitleParser.detectFormat(content) ?: SubtitleFormat.SRT

                val track =
                    SubtitleTrack(
                        id = "external_${uri.hashCode()}",
                        language = language,
                        languageCode = language.lowercase(),
                        url = uri.toString(),
                        format = format,
                        source = SubtitleSource.EXTERNAL,
                        isDefault = false,
                    )

                if (config.cacheSubtitles) {
                    cacheSubtitle(track, content)
                    logger.debug(TAG, "External subtitle cached: ${track.id}")
                }

                _currentSubtitle.value = track
                SubtitleDownloadResult.Success(track, content)
            } catch (e: Exception) {
                logger.error(TAG, "Error loading external subtitle", e)
                SubtitleDownloadResult.Error("Failed to load file: ${e.message}", e)
            }
        }

    fun clearSubtitle() {
        _currentSubtitle.value = null
        logger.debug(TAG, "Subtitle cleared")
    }

    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            logger.debug(TAG, "Subtitle cache cleared")
        } catch (e: Exception) {
            logger.error(TAG, "Error clearing cache", e)
        }
    }

    fun getCacheSize(): Long =
        try {
            cacheDir
                .walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            0L
        }

    private suspend fun downloadFromUrl(url: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    logger.warning(TAG, "Download failed: ${response.code}")
                    return@withContext null
                }

                response.body?.string()
            } catch (e: Exception) {
                logger.error(TAG, "Error downloading from URL", e)
                null
            }
        }

    private fun getCachedSubtitle(track: SubtitleTrack): String? =
        try {
            val cacheFile = File(cacheDir, getCacheFileName(track))
            if (cacheFile.exists()) {
                cacheFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error reading cached subtitle", e)
            null
        }

    private fun cacheSubtitle(
        track: SubtitleTrack,
        content: String,
    ) {
        try {
            if (getCacheSize() > config.maxCacheSize) {
                clearOldestCacheFiles()
            }

            val cacheFile = File(cacheDir, getCacheFileName(track))
            cacheFile.writeText(content)
            logger.debug(TAG, "Subtitle cached: ${track.id}")
        } catch (e: Exception) {
            logger.error(TAG, "Error caching subtitle", e)
        }
    }

    private fun clearOldestCacheFiles() {
        try {
            val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
            val targetSize = config.maxCacheSize * 0.7 // Clear to 70% of max

            var currentSize = getCacheSize()
            for (file in files) {
                if (currentSize <= targetSize) break
                currentSize -= file.length()
                file.delete()
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error clearing old cache", e)
        }
    }

    private fun getCacheFileName(track: SubtitleTrack): String {
        val hash = track.id.hashCode().toString()
        return "$hash.${track.format.extension}"
    }

    private fun extractVideoName(url: String): String =
        try {
            val uri = Uri.parse(url)
            uri.lastPathSegment?.substringBeforeLast(".") ?: "video"
        } catch (e: Exception) {
            "video"
        }

    suspend fun calculateVideoHash(videoPath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(videoPath)
                if (!file.exists()) return@withContext null

                val fileSize = file.length()
                val chunkSize = 65536L

                val md = MessageDigest.getInstance("MD5")

                file.inputStream().use { stream ->
                    // first chunk
                    val buffer = ByteArray(chunkSize.toInt())
                    stream.read(buffer)
                    md.update(buffer)

                    // last chunk
                    stream.skip(maxOf(0, fileSize - chunkSize * 2))
                    stream.read(buffer)
                    md.update(buffer)
                }

                md.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                logger.error(TAG, "Error calculating video hash", e)
                null
            }
        }
}
