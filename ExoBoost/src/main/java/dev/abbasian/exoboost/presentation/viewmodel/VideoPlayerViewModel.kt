package dev.abbasian.exoboost.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.domain.model.VideoInfo
import dev.abbasian.exoboost.domain.model.VideoPlayerConfig
import dev.abbasian.exoboost.domain.model.VideoState
import dev.abbasian.exoboost.domain.usecase.CacheVideoUseCase
import dev.abbasian.exoboost.domain.usecase.PlayVideoUseCase
import dev.abbasian.exoboost.domain.usecase.RetryVideoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class VideoPlayerViewModel(
    private val playVideoUseCase: PlayVideoUseCase,
    private val cacheVideoUseCase: CacheVideoUseCase,
    private val retryVideoUseCase: RetryVideoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var retryCount = 0
    private var maxRetryCount = 3
    private var currentJob: Job? = null
    private var isVideoLoaded = false

    fun loadVideo(url: String, config: VideoPlayerConfig) {
        maxRetryCount = config.maxRetryCount

        if (isVideoLoaded && _uiState.value.currentUrl == url &&
            _uiState.value.videoState !is VideoState.Error) {
            Log.d("VideoPlayerViewModel", "Video already loaded: $url")
            return
        }

        currentJob?.cancel()

        currentJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    videoState = VideoState.Loading,
                    currentUrl = url,
                    isLoading = true
                )

                playVideoUseCase.execute(url, config)
                isVideoLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("VideoPlayerViewModel", "Error loading video", e)
                isVideoLoaded = false
                _uiState.value = _uiState.value.copy(
                    videoState = VideoState.Error(
                        PlayerError.UnknownError(
                            "Failed to load video: ${e.message}",
                            e
                        )
                    ),
                    isLoading = false
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
                Log.e("VideoPlayerViewModel", "Error in playPause", e)
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                playVideoUseCase.seekTo(position)
            } catch (e: Exception) {
                Log.e("VideoPlayerViewModel", "Error seeking", e)
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
                Log.e("VideoPlayerViewModel", "Error setting volume", e)
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
                        videoState = VideoState.Loading,
                        isLoading = true
                    )
                    retryVideoUseCase.execute()
                } catch (e: Exception) {
                    Log.e("VideoPlayerViewModel", "Error retrying", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                videoState = VideoState.Error(
                    PlayerError.UnknownError("Maximum retry attempts reached")
                ),
                isLoading = false
            )
        }
    }

    fun updateVideoState(state: VideoState) {
        _uiState.value = _uiState.value.copy(
            videoState = state,
            isLoading = state is VideoState.Loading
        )

        if (state is VideoState.Ready || state is VideoState.Playing) {
            retryCount = 0
            isVideoLoaded = true
        }

        if (state is VideoState.Error && retryCount < maxRetryCount) {
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
                    isVideoLoaded = false
                }
            }
        }
    }

    fun updateVideoInfo(info: VideoInfo) {
        _uiState.value = _uiState.value.copy(videoInfo = info)
    }

    fun resetPlayer() {
        currentJob?.cancel()
        retryCount = 0
        isVideoLoaded = false
        _uiState.value = VideoPlayerUiState()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            currentJob?.cancel()
            playVideoUseCase.release()
        } catch (e: Exception) {
            Log.e("VideoPlayerViewModel", "Error releasing resources", e)
        }
    }
}

data class VideoPlayerUiState(
    val videoState: VideoState = VideoState.Idle,
    val videoInfo: VideoInfo = VideoInfo(),
    val showControls: Boolean = true,
    val volume: Float = 1f,
    val brightness: Float = 0.5f,
    val isLoading: Boolean = false,
    val currentUrl: String = ""
)