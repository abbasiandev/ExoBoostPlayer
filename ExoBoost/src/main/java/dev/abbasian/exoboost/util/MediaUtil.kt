package dev.abbasian.exoboost.util

object MediaUtil {
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