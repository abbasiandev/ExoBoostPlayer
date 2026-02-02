package dev.abbasian.exoboost.data.api.sources

import dev.abbasian.exoboost.domain.model.SubtitleQuery
import dev.abbasian.exoboost.domain.model.SubtitleSource
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SubtitleApiAggregator(
    private val openSubtitlesApi: OpenSubtitlesApi,
    private val yifySubtitlesApi: YifySubtitlesApi,
    private val podnapisiApi: PodnapisiApi,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "SubtitleApiAggregator"
    }

    suspend fun searchAllSources(
        query: SubtitleQuery,
        sources: List<SubtitleSource> =
            listOf(
                SubtitleSource.OPENSUBTITLES,
                SubtitleSource.YIFY,
                SubtitleSource.PODNAPISI,
            ),
    ): List<SubtitleTrack> =
        coroutineScope {
            logger.debug(TAG, "Searching subtitles across ${sources.size} sources")

            val searchJobs =
                sources.map { source ->
                    async {
                        try {
                            when (source) {
                                SubtitleSource.OPENSUBTITLES -> openSubtitlesApi.searchSubtitles(query)
                                SubtitleSource.YIFY -> yifySubtitlesApi.searchSubtitles(query)
                                SubtitleSource.PODNAPISI -> podnapisiApi.searchSubtitles(query)
                                else -> emptyList()
                            }
                        } catch (e: Exception) {
                            logger.error(TAG, "Error searching $source", e)
                            emptyList()
                        }
                    }
                }

            val results = searchJobs.awaitAll().flatten()
            logger.debug(TAG, "Found ${results.size} subtitles total")

            results
                .distinctBy { "${it.languageCode}_${it.language}" }
                .sortedWith(
                    compareBy(
                        { it.languageCode },
                        { sourcePriority(it.source) },
                    ),
                )
        }

    suspend fun downloadSubtitle(track: SubtitleTrack): String? =
        try {
            when (track.source) {
                SubtitleSource.OPENSUBTITLES -> openSubtitlesApi.downloadSubtitle(track)
                SubtitleSource.YIFY -> yifySubtitlesApi.downloadSubtitle(track)
                SubtitleSource.PODNAPISI -> podnapisiApi.downloadSubtitle(track)
                else -> null
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error downloading subtitle from ${track.source}", e)
            null
        }

    private fun sourcePriority(source: SubtitleSource): Int =
        when (source) {
            SubtitleSource.OPENSUBTITLES -> 1
            SubtitleSource.YIFY -> 2
            SubtitleSource.PODNAPISI -> 3
            SubtitleSource.EMBEDDED -> 0
            else -> 999
        }
}
