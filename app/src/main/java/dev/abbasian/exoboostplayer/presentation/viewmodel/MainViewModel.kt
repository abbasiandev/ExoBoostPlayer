package dev.abbasian.exoboostplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectMedia(mediaItem: MediaItem) {
        _uiState.value = _uiState.value.copy(
            selectedMedia = mediaItem,
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
    val selectedMedia: MediaItem? = null,
    val showPlayer: Boolean = false,
    val errorMessage: String? = null
)

data class MediaItem(
    val title: String,
    val url: String,
    val description: String,
    val artist: String? = null,
)