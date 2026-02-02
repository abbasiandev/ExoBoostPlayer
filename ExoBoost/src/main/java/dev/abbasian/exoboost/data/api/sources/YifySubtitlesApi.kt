package dev.abbasian.exoboost.data.api.sources

import dev.abbasian.exoboost.domain.model.SubtitleFormat
import dev.abbasian.exoboost.domain.model.SubtitleQuery
import dev.abbasian.exoboost.domain.model.SubtitleSource
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class YifySubtitlesApi(
    private val client: OkHttpClient,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "YifySubtitlesApi"
        private const val BASE_URL = "https://yifysubtitles.org"
        private const val API_URL = "https://api.yifysubtitles.org"
    }

    suspend fun searchSubtitles(query: SubtitleQuery): List<SubtitleTrack> =
        withContext(Dispatchers.IO) {
            try {
                val imdbId = query.imdbId ?: return@withContext emptyList()

                logger.debug(TAG, "Searching YIFY subtitles for IMDB: $imdbId")

                val url = "$API_URL/subs/$imdbId"
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    logger.warning(TAG, "YIFY API error: ${response.code}")
                    return@withContext emptyList()
                }

                val jsonResponse = response.body?.string() ?: return@withContext emptyList()
                parseSearchResults(jsonResponse, imdbId)
            } catch (e: Exception) {
                logger.error(TAG, "Error searching YIFY subtitles", e)
                emptyList()
            }
        }

    private fun parseSearchResults(
        json: String,
        imdbId: String,
    ): List<SubtitleTrack> {
        val tracks = mutableListOf<SubtitleTrack>()

        try {
            val jsonObject = JSONObject(json)
            val subsObject = jsonObject.optJSONObject("subs") ?: return emptyList()
            val languageKeys = subsObject.keys()

            while (languageKeys.hasNext()) {
                val languageCode = languageKeys.next()
                val languageSubtitles = subsObject.getJSONObject(languageCode)
                val subtitlesKeys = languageSubtitles.keys()

                while (subtitlesKeys.hasNext()) {
                    val key = subtitlesKeys.next()
                    val subtitle = languageSubtitles.getJSONObject(key)

                    val url = subtitle.optString("url", "")
                    val language = subtitle.optString("lang", languageCode)

                    if (url.isNotEmpty()) {
                        tracks.add(
                            SubtitleTrack(
                                id = "yify_${imdbId}_$languageCode",
                                language = language,
                                languageCode = languageCode,
                                url = url,
                                format = SubtitleFormat.SRT,
                                source = SubtitleSource.YIFY,
                                isDefault = false,
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error parsing YIFY results", e)
        }

        return tracks
    }

    suspend fun downloadSubtitle(track: SubtitleTrack): String? =
        withContext(Dispatchers.IO) {
            try {
                logger.debug(TAG, "Downloading YIFY subtitle: ${track.id}")

                val request =
                    Request
                        .Builder()
                        .url(track.url)
                        .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    logger.warning(TAG, "Download failed: ${response.code}")
                    return@withContext null
                }

                response.body?.string()
            } catch (e: Exception) {
                logger.error(TAG, "Error downloading subtitle", e)
                null
            }
        }
}
