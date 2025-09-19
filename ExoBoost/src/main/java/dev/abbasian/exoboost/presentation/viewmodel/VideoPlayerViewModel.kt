package dev.abbasian.exoboost.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abbasian.exoboost.domain.model.VideoInfo
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.domain.usecase.CacheVideoUseCase
import dev.abbasian.exoboost.domain.usecase.PlayVideoUseCase
import dev.abbasian.exoboost.domain.usecase.RetryVideoUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VideoPlayerViewModel(
    private val playVideoUseCase: PlayVideoUseCase,
    private val cacheVideoUseCase: CacheVideoUseCase,
    private val retryVideoUseCase: RetryVideoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var retryCount = 0
    private var maxRetryCount = 3

    fun loadVideo(url: String, config: VideoPlayerConfig) {
        maxRetryCount = config.maxRetryCount
        viewModelScope.launch {
            try {
                playVideoUseCase.execute(url, config)
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerViewModel", "Error loading video", e)
                _uiState.value = _uiState.value.copy(
                    videoState = VideoState.Error(
                        dev.abbasian.exoboost.domain.model.PlayerError.UnknownError(
                            "Failed to load video: ${e.message}",
                            e
                        )
                    )
                )
            }
        }
    }

    fun playPause() {
        viewModelScope.launch {
            try {
                if (_uiState.value.videoInfo.isPlaying) {
                    playVideoUseCase.pause()
                } else {
                    playVideoUseCase.play()
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerViewModel", "Error in playPause", e)
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                playVideoUseCase.seekTo(position)
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerViewModel", "Error seeking", e)
            }
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            try {
                val clampedVolume = volume.coerceIn(0f, 1f)
                playVideoUseCase.setVolume(clampedVolume)
                _uiState.value = _uiState.value.copy(volume = clampedVolume)
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerViewModel", "Error setting volume", e)
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
                    retryVideoUseCase.execute()
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerViewModel", "Error retrying", e)
                }
            }
        }
    }

    fun updateVideoState(state: VideoState) {
        _uiState.value = _uiState.value.copy(videoState = state)

        if (state is VideoState.Ready || state is VideoState.Playing) {
            retryCount = 0
        }

        if (state is VideoState.Error && retryCount < maxRetryCount) {
            retry()
        }
    }

    fun updateVideoInfo(info: VideoInfo) {
        _uiState.value = _uiState.value.copy(videoInfo = info)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            playVideoUseCase.release()
        } catch (e: Exception) {
            android.util.Log.e("VideoPlayerViewModel", "Error releasing resources", e)
        }
    }
}

data class VideoPlayerUiState(
    val videoState: VideoState = VideoState.Idle,
    val videoInfo: VideoInfo = VideoInfo(),
    val showControls: Boolean = true,
    val volume: Float = 1f,
    val brightness: Float = 0.5f,
    val isLoading: Boolean = false
)