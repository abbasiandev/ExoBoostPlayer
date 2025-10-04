package dev.abbasian.exoboostplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectMedia(mediaItem: MediaItem) {
        _uiState.update {
            it.copy(
                selectedMedia = mediaItem,
                showPlayer = true,
                playlist = emptyList(),
                currentTrackIndex = 0,
                errorMessage = null
            )
        }
    }

    fun selectMediaWithPlaylist(playlist: List<MediaItem>, index: Int) {
        if (playlist.isEmpty() || index !in playlist.indices) {
            showError("Invalid playlist or index")
            return
        }

        _uiState.update {
            it.copy(
                playlist = playlist,
                currentTrackIndex = index,
                selectedMedia = playlist[index],
                showPlayer = true,
                errorMessage = null
            )
        }
    }

    fun playNext() {
        val currentState = _uiState.value

        if (currentState.playlist.isEmpty()) {
            return
        }

        val nextIndex = currentState.currentTrackIndex + 1

        if (nextIndex < currentState.playlist.size) {
            _uiState.update {
                it.copy(
                    currentTrackIndex = nextIndex,
                    selectedMedia = currentState.playlist[nextIndex],
                    errorMessage = null
                )
            }
        }
    }

    fun playPrevious() {
        val currentState = _uiState.value

        if (currentState.playlist.isEmpty()) {
            return
        }

        val previousIndex = currentState.currentTrackIndex - 1

        if (previousIndex >= 0) {
            _uiState.update {
                it.copy(
                    currentTrackIndex = previousIndex,
                    selectedMedia = currentState.playlist[previousIndex],
                    errorMessage = null
                )
            }
        }
    }


    fun goBack() {
        _uiState.update {
            it.copy(
                showPlayer = false,
                selectedMedia = null,
                playlist = emptyList(),
                currentTrackIndex = 0,
                errorMessage = null
            )
        }
    }

    fun showError(message: String) {
        _uiState.update {
            it.copy(errorMessage = message)
        }
    }

    fun hideError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    fun hasNext(): Boolean {
        val state = _uiState.value
        return state.playlist.isNotEmpty() &&
                state.currentTrackIndex < state.playlist.size - 1
    }

    fun hasPrevious(): Boolean {
        val state = _uiState.value
        return state.playlist.isNotEmpty() &&
                state.currentTrackIndex > 0
    }
}

data class MainUiState(
    val selectedMedia: MediaItem? = null,
    val showPlayer: Boolean = false,
    val errorMessage: String? = null,
    val playlist: List<MediaItem> = emptyList(),
    val currentTrackIndex: Int = 0
)

data class MediaItem(
    val title: String,
    val url: String,
    val description: String,
    val artist: String? = null,
    val mimeType: String? = null
)