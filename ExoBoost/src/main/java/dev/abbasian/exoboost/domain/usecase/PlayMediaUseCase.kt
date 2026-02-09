package dev.abbasian.exoboost.domain.usecase

import androidx.media3.common.MediaItem
import dev.abbasian.exoboost.data.manager.SubtitleManager
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.repository.MediaRepository

class PlayMediaUseCase(
    private val repository: MediaRepository,
    private val subtitleManager: SubtitleManager,
) {
    suspend fun execute(
        url: String,
        config: MediaPlayerConfig,
        subtitleTrack: SubtitleTrack? = null,
        subtitleContent: String? = null,
    ) {
        val subtitleConfigurations =
            if (subtitleTrack != null && subtitleContent != null) {
                listOf(subtitleManager.createSubtitleConfiguration(subtitleTrack, subtitleContent))
            } else {
                emptyList()
            }

        repository.loadMedia(url, config, subtitleConfigurations)
    }

    suspend fun applyEqualizerValues(values: List<Float>) {
        repository.applyEqualizerValues(values)
    }

    suspend fun getEqualizerBandCount(): Int = repository.getEqualizerBandCount()

    suspend fun getEqualizerFrequencies(): List<String> = repository.getEqualizerFrequencies()

    suspend fun play() = repository.play()

    suspend fun pause() = repository.pause()

    suspend fun setPlaybackSpeed(speed: Float) = repository.setPlaybackSpeed(speed)

    suspend fun selectQuality(quality: VideoQuality) = repository.selectQuality(quality)

    suspend fun getAvailableQualities(): List<VideoQuality> = repository.getAvailableQualities()

    suspend fun seekTo(position: Long) = repository.seekTo(position)

    suspend fun setVolume(volume: Float) = repository.setVolume(volume)

    fun setSubtitleEnabled(enabled: Boolean) = repository.setSubtitleEnabled(enabled)

    fun selectSubtitleTrack(languageCode: String) = repository.selectSubtitleTrack(languageCode)

    suspend fun loadExternalSubtitle(
        uri: android.net.Uri,
        language: String = "Unknown",
    ) = subtitleManager.loadExternalSubtitle(uri, language)

    fun addSubtitleToCurrentMedia(subtitleConfiguration: MediaItem.SubtitleConfiguration) =
        repository.addSubtitleToCurrentMedia(subtitleConfiguration)

    fun release() = repository.release()
}
