package dev.abbasian.exoboostplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectVideo(videoItem: VideoItem) {
        _uiState.value = _uiState.value.copy(
            selectedVideo = videoItem,
            showPlayer = true
        )
    }

    fun goBack() {
        _uiState.value = _uiState.value.copy(
            showPlayer = false,
            errorMessage = null
        )
    }

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun hideError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class MainUiState(
    val selectedVideo: VideoItem? = null,
    val showPlayer: Boolean = false,
    val errorMessage: String? = null
)

data class VideoItem(
    val title: String,
    val url: String,
    val description: String
)