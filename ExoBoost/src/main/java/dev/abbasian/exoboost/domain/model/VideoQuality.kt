package dev.abbasian.exoboost.domain.model

data class VideoQuality(
    val trackGroup: Int,
    val trackIndex: Int,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val label: String,
    val isSelected: Boolean = false
) {
    fun getQualityLabel(): String {
        return when {
            height >= 2160 -> "4K (${height}p)"
            height >= 1440 -> "2K (${height}p)"
            height >= 1080 -> "Full HD (${height}p)"
            height >= 720 -> "HD (${height}p)"
            height >= 480 -> "SD (${height}p)"
            height >= 360 -> "360p"
            height >= 240 -> "240p"
            else -> "Auto"
        }
    }
}