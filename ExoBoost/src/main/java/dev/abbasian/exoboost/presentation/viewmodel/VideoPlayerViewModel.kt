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
            playVideoUseCase.execute(url, config)
        }
    }

    fun playPause() {
        viewModelScope.launch {
            if (_uiState.value.videoInfo.isPlaying) {
                playVideoUseCase.pause()
            } else {
                playVideoUseCase.play()
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            playVideoUseCase.seekTo(position)
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            playVideoUseCase.setVolume(volume)
            _uiState.value = _uiState.value.copy(volume = volume)
        }
    }

    fun setBrightness(brightness: Float) {
        _uiState.value = _uiState.value.copy(brightness = brightness)
    }

    fun showControls(show: Boolean) {
        _uiState.value = _uiState.value.copy(showControls = show)
    }

    fun retry() {
        if (retryCount < maxRetryCount) {
            retryCount++
            viewModelScope.launch {
                retryVideoUseCase.execute()
            }
        }
    }

    fun updateVideoState(state: VideoState) {
        _uiState.value = _uiState.value.copy(videoState = state)

        if (state is VideoState.Error && retryCount < maxRetryCount) {
            retry()
        }
    }

    fun updateVideoInfo(info: VideoInfo) {
        _uiState.value = _uiState.value.copy(videoInfo = info)
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