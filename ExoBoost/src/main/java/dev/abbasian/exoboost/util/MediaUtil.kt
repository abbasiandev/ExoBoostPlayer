package dev.abbasian.exoboost.util

object MediaUtil {

    private val audioExtensions = setOf(
        "mp3", "aac", "flac", "wav", "ogg", "m4a", "opus", "wma"
    )

    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v"
    )

    private val audioMimeTypes = setOf(
        "audio/mpeg", "audio/aac", "audio/flac", "audio/wav",
        "audio/ogg", "audio/mp4", "audio/opus", "audio/x-ms-wma"
    )

    fun getMediaType(url: String, mimeType: String? = null): MediaType {
        mimeType?.let { mime ->
            when {
                mime.startsWith("audio/") -> return MediaType.AUDIO
                mime.startsWith("video/") -> return MediaType.VIDEO
            }
        }

        val extension = url.substringAfterLast('.', "").lowercase()
        return when {
            audioExtensions.contains(extension) -> MediaType.AUDIO
            videoExtensions.contains(extension) -> MediaType.VIDEO
            isHLSStream(url) || isMPDStream(url) -> MediaType.VIDEO
            else -> MediaType.UNKNOWN
        }
    }

    fun isHLSStream(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains(".m3u8") ||
                lowerUrl.contains("application/x-mpegurl") ||
                lowerUrl.contains("application/vnd.apple.mpegurl")
    }

    fun isMPDStream(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains(".mpd") ||
                lowerUrl.contains("application/dash+xml")
    }

    fun isAdaptiveStream(url: String): Boolean {
        return isHLSStream(url) || isMPDStream(url)
    }

    fun getStreamType(url: String): StreamType {
        return when {
            isHLSStream(url) -> StreamType.HLS
            isMPDStream(url) -> StreamType.DASH
            else -> StreamType.PROGRESSIVE
        }
    }
}

enum class StreamType {
    HLS, DASH, PROGRESSIVE
}

enum class MediaType {
    AUDIO, VIDEO, UNKNOWN
}