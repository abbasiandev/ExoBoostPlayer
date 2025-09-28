package dev.abbasian.exoboost.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abbasian.exoboost.domain.model.MediaInfo
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.usecase.CacheVideoUseCase
import dev.abbasian.exoboost.domain.usecase.PlayMediaUseCase
import dev.abbasian.exoboost.domain.usecase.RetryMediaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MediaPlayerViewModel(
    private val playMediaUseCase: PlayMediaUseCase,
    private val cacheVideoUseCase: CacheVideoUseCase,
    private val retryMediaUseCase: RetryMediaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaPlayerUiState())
    val uiState: StateFlow<MediaPlayerUiState> = _uiState.asStateFlow()

    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private var retryCount = 0
    private var maxRetryCount = 3
    private var currentJob: Job? = null
    private var isMediaLoaded = false

    fun loadMedia(url: String, config: MediaPlayerConfig) {
        maxRetryCount = config.maxRetryCount

        if (isMediaLoaded && _uiState.value.currentUrl == url &&
            _uiState.value.mediaState !is MediaState.Error) {
            Log.d("MediaPlayerViewModel", "media already loaded: $url")
            return
        }

        currentJob?.cancel()

        currentJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    mediaState = MediaState.Loading,
                    currentUrl = url,
                    isLoading = true
                )

                playMediaUseCase.execute(url, config)
                isMediaLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error loading media", e)
                isMediaLoaded = false
                _uiState.value = _uiState.value.copy(
                    mediaState = MediaState.Error(
                        PlayerError.UnknownError(
                            "Failed to load media: ${e.message}",
                            e
                        )
                    ),
                    isLoading = false
                )
            }
        }
    }

    fun toggleEqualizer() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(showEqualizer = !currentState.showEqualizer)
        Log.d("MediaPlayerViewModel", "Toggled equalizer: ${_uiState.value.showEqualizer}")
    }

    fun applyEqualizerValues(values: List<Float>) {
        viewModelScope.launch {
            try {
                Log.d("MediaPlayerViewModel", "Applying equalizer values: $values")
                playMediaUseCase.applyEqualizerValues(values)
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error applying equalizer values", e)
            }
        }
    }

    fun getEqualizerFrequencies(): List<String> {
        return viewModelScope.async {
            try {
                playMediaUseCase.getEqualizerFrequencies()
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error getting frequencies", e)
                listOf("60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz")
            }
        }.getCompleted()
    }

    fun playPause() {
        viewModelScope.launch {
            try {
                if (_uiState.value.mediaInfo.isPlaying) {
                    playMediaUseCase.pause()
                } else {
                    playMediaUseCase.play()
                }
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error in playPause", e)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                playMediaUseCase.setPlaybackSpeed(speed)
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error setting playback speed", e)
            }
        }
    }

    fun selectQuality(quality: VideoQuality) {
        viewModelScope.launch {
            try {
                playMediaUseCase.selectQuality(quality)
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error selecting quality", e)
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                playMediaUseCase.seekTo(position)
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error seeking", e)
            }
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            try {
                val clampedVolume = volume.coerceIn(0f, 1f)
                playMediaUseCase.setVolume(clampedVolume)
                _uiState.value = _uiState.value.copy(volume = clampedVolume)
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Error setting volume", e)
            }
        }
    }

    fun setBrightness(brightness: Float) {
        val clampedBrightness = brightness.coerceIn(0f, 1f)
        _uiState.value = _uiState.value.copy(brightness = clampedBrightness)
    }

    fun showControls(show: Boolean) {
        _uiState.value = _uiState.value.copy(showControls = show)
    }

    fun retry() {
        if (retryCount < maxRetryCount) {
            retryCount++
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(
                        mediaState = MediaState.Loading,
                        isLoading = true
                    )
                    retryMediaUseCase.execute()
                } catch (e: Exception) {
                    Log.e("MediaPlayerViewModel", "Error retrying", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                mediaState = MediaState.Error(
                    PlayerError.UnknownError("Maximum retry attempts reached")
                ),
                isLoading = false
            )
        }
    }

    fun updateMediaState(state: MediaState) {
        _uiState.value = _uiState.value.copy(
            mediaState = state,
            isLoading = state is MediaState.Loading
        )

        if (state is MediaState.Ready || state is MediaState.Playing) {
            retryCount = 0
            isMediaLoaded = true
        }

        if (state is MediaState.Error && retryCount < maxRetryCount) {
            when (state.error) {
                is PlayerError.NetworkError -> {
                    if (state.error.isRetryable) {
                        viewModelScope.launch {
                            delay(2000)
                            retry()
                        }
                    }
                }
                else -> {
                    isMediaLoaded = false
                }
            }
        }
    }

    fun updateMediaInfo(info: MediaInfo) {
        _uiState.value = _uiState.value.copy(mediaInfo = info)
    }

    fun resetPlayer() {
        currentJob?.cancel()
        retryCount = 0
        isMediaLoaded = false
        _uiState.value = MediaPlayerUiState()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            currentJob?.cancel()
            playMediaUseCase.release()
        } catch (e: Exception) {
            Log.e("MediaPlayerViewModel", "Error releasing resources", e)
        }
    }
}

data class MediaPlayerUiState(
    val mediaState: MediaState = MediaState.Idle,
    val mediaInfo: MediaInfo = MediaInfo(),
    val showControls: Boolean = true,
    val volume: Float = 1f,
    val brightness: Float = 0.5f,
    val isLoading: Boolean = false,
    val currentUrl: String = "",
    val showEqualizer: Boolean = false
)