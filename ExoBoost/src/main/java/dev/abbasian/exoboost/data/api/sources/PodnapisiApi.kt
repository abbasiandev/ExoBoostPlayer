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
import org.json.JSONArray

class PodnapisiApi(
    private val client: OkHttpClient,
    private val logger: ExoBoostLogger,
) {
    companion object {
        private const val TAG = "PodnapisiApi"
        private const val BASE_URL = "https://www.podnapisi.net"
        private const val API_URL = "$BASE_URL/subtitles/search"
    }

    suspend fun searchSubtitles(query: SubtitleQuery): List<SubtitleTrack> =
        withContext(Dispatchers.IO) {
            try {
                logger.debug(TAG, "Searching Podnapisi for: ${query.videoName}")

                val urlBuilder = StringBuilder(API_URL)
                urlBuilder.append("?keywords=${query.videoName.replace(" ", "+")}")
                urlBuilder.append("&language=${query.language}")

                val request =
                    Request
                        .Builder()
                        .url(urlBuilder.toString())
                        .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    logger.warning(TAG, "Podnapisi API error: ${response.code}")
                    return@withContext emptyList()
                }

                val jsonResponse = response.body?.string() ?: return@withContext emptyList()
                parseSearchResults(jsonResponse)
            } catch (e: Exception) {
                logger.error(TAG, "Error searching Podnapisi", e)
                emptyList()
            }
        }

    private fun parseSearchResults(json: String): List<SubtitleTrack> {
        val tracks = mutableListOf<SubtitleTrack>()

        try {
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                val id = item.optString("id", "")
                val language = item.optString("language", "Unknown")
                val downloadUrl = item.optString("url", "")
                val format = item.optString("format", "srt")

                if (id.isNotEmpty() && downloadUrl.isNotEmpty()) {
                    tracks.add(
                        SubtitleTrack(
                            id = "podnapisi_$id",
                            language = language,
                            languageCode = language.lowercase(),
                            url = if (downloadUrl.startsWith("http")) downloadUrl else "$BASE_URL$downloadUrl",
                            format =
                                when (format.lowercase()) {
                                    "srt" -> SubtitleFormat.SRT
                                    "vtt" -> SubtitleFormat.VTT
                                    "ass" -> SubtitleFormat.ASS
                                    "ssa" -> SubtitleFormat.SSA
                                    else -> SubtitleFormat.SRT
                                },
                            source = SubtitleSource.PODNAPISI,
                            isDefault = false,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error parsing Podnapisi results", e)
        }

        return tracks
    }

    suspend fun downloadSubtitle(track: SubtitleTrack): String? =
        withContext(Dispatchers.IO) {
            try {
                logger.debug(TAG, "Downloading Podnapisi subtitle: ${track.id}")

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
