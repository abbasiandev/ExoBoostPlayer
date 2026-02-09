package dev.abbasian.exoboost.presentation.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abbasian.exoboost.domain.error.ErrorClassifier
import dev.abbasian.exoboost.domain.model.HighlightConfig
import dev.abbasian.exoboost.domain.model.MediaInfo
import dev.abbasian.exoboost.domain.model.MediaPlayerConfig
import dev.abbasian.exoboost.domain.model.MediaState
import dev.abbasian.exoboost.domain.model.PlayerError
import dev.abbasian.exoboost.domain.model.SubtitleDownloadResult
import dev.abbasian.exoboost.domain.model.SubtitleStyle
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import dev.abbasian.exoboost.domain.model.VideoChapter
import dev.abbasian.exoboost.domain.model.VideoQuality
import dev.abbasian.exoboost.domain.usecase.GenerateVideoHighlightsUseCase
import dev.abbasian.exoboost.domain.usecase.ManageHighlightCacheUseCase
import dev.abbasian.exoboost.domain.usecase.ManageSubtitleUseCase
import dev.abbasian.exoboost.domain.usecase.PlayMediaUseCase
import dev.abbasian.exoboost.domain.usecase.RetryMediaUseCase
import dev.abbasian.exoboost.domain.usecase.SearchSubtitlesUseCase
import dev.abbasian.exoboost.presentation.state.HighlightsState
import dev.abbasian.exoboost.util.ExoBoostLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MediaPlayerViewModel(
    private val playMediaUseCase: PlayMediaUseCase,
    private val retryMediaUseCase: RetryMediaUseCase,
    private val generateHighlightsUseCase: GenerateVideoHighlightsUseCase,
    private val manageHighlightCacheUseCase: ManageHighlightCacheUseCase,
    private val searchSubtitlesUseCase: SearchSubtitlesUseCase,
    private val manageSubtitleUseCase: ManageSubtitleUseCase,
    private val errorClassifier: ErrorClassifier,
    private val logger: ExoBoostLogger,
) : ViewModel() {
    companion object {
        private const val TAG = "MediaPlayerViewModel"
    }

    private val _errorState = MutableStateFlow<PlayerError?>(null)
    val errorState: StateFlow<PlayerError?> = _errorState

    private val _uiState = MutableStateFlow(MediaPlayerUiState())
    val uiState: StateFlow<MediaPlayerUiState> = _uiState.asStateFlow()

    private val _showEqualizer = MutableStateFlow(false)
    val showEqualizer: StateFlow<Boolean> = _showEqualizer.asStateFlow()

    private val _highlightsState = MutableStateFlow<HighlightsState>(HighlightsState.Idle)
    val highlightsState: StateFlow<HighlightsState> = _highlightsState

    private val _showHighlightsBottomSheet = MutableStateFlow(false)
    val showHighlightsBottomSheet: StateFlow<Boolean> = _showHighlightsBottomSheet.asStateFlow()

    private val _showSubtitleSheet = MutableStateFlow(false)
    val showSubtitleSheet: StateFlow<Boolean> = _showSubtitleSheet.asStateFlow()

    private val _availableSubtitles = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val availableSubtitles: StateFlow<List<SubtitleTrack>> = _availableSubtitles.asStateFlow()

    private val _currentSubtitle = MutableStateFlow<SubtitleTrack?>(null)
    val currentSubtitle: StateFlow<SubtitleTrack?> = _currentSubtitle.asStateFlow()

    private val _subtitleStyle = MutableStateFlow(SubtitleStyle())
    val subtitleStyle: StateFlow<SubtitleStyle> = _subtitleStyle.asStateFlow()

    private var retryCount = 0
    private var maxRetryCount = 3
    private var currentJob: Job? = null
    private var highlightsJob: Job? = null
    private var subtitleJob: Job? = null
    private var isMediaLoaded = false

    init {
        viewModelScope.launch {
            manageSubtitleUseCase.getSubtitleStyle().collect { style ->
                _subtitleStyle.value = style
            }
        }
    }

    fun loadMedia(
        url: String,
        config: MediaPlayerConfig,
    ) {
        maxRetryCount = config.maxRetryCount

        if (isMediaLoaded && _uiState.value.currentUrl == url &&
            _uiState.value.mediaState !is MediaState.Error
        ) {
            logger.debug(TAG, "media already loaded: $url")
            return
        }

        currentJob?.cancel()

        currentJob =
            viewModelScope.launch {
                try {
                    _uiState.value =
                        _uiState.value.copy(
                            mediaState = MediaState.Loading,
                            currentUrl = url,
                            isLoading = true,
                        )

                    _errorState.value = null

                    playMediaUseCase.execute(url, config)
                    isMediaLoaded = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(TAG, "Error loading media", e)
                    isMediaLoaded = false
                    _uiState.value =
                        _uiState.value.copy(
                            mediaState =
                                MediaState.Error(
                                    PlayerError.UnknownError(
                                        "Failed to load media: ${e.message}",
                                        e,
                                    ),
                                ),
                            isLoading = false,
                        )
                }
            }
    }

    fun toggleEqualizer() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(showEqualizer = !currentState.showEqualizer)
        logger.debug(TAG, "Toggled equalizer: ${_uiState.value.showEqualizer}")
    }

    fun applyEqualizerValues(values: List<Float>) {
        viewModelScope.launch {
            try {
                logger.debug(TAG, "Applying equalizer values: $values")
                playMediaUseCase.applyEqualizerValues(values)
            } catch (e: Exception) {
                logger.error(TAG, "Error applying equalizer values", e)
            }
        }
    }

    fun getEqualizerFrequencies(): List<String> =
        viewModelScope
            .async {
                try {
                    playMediaUseCase.getEqualizerFrequencies()
                } catch (e: Exception) {
                    logger.error(TAG, "Error getting frequencies", e)
                    listOf("60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz")
                }
            }.getCompleted()

    fun playPause() {
        viewModelScope.launch {
            try {
                if (_uiState.value.mediaInfo.isPlaying) {
                    playMediaUseCase.pause()
                } else {
                    playMediaUseCase.play()
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error in playPause", e)
                _errorState.value = errorClassifier.classifyError(e)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                playMediaUseCase.setPlaybackSpeed(speed)
            } catch (e: Exception) {
                logger.error(TAG, "Error setting playback speed", e)
                _errorState.value = errorClassifier.classifyError(e)
            }
        }
    }

    fun setPlaylistInfo(
        currentIndex: Int,
        totalTracks: Int,
    ) {
        _uiState.value =
            _uiState.value.copy(
                currentTrackIndex = currentIndex,
                hasNext = currentIndex < totalTracks - 1,
                hasPrevious = currentIndex > 0,
            )
    }

    fun updateHighlightsState(state: HighlightsState) {
        _highlightsState.value = state
        logger.debug(TAG, "Highlights state updated from service: ${state::class.simpleName}")
    }

    fun toggleHighlightsBottomSheet(show: Boolean) {
        _showHighlightsBottomSheet.value = show
        logger.debug(TAG, "Highlights bottom sheet: $show")
    }

    fun dismissHighlightsBottomSheet() {
        _showHighlightsBottomSheet.value = false
        logger.debug(TAG, "Highlights bottom sheet dismissed")
    }

    fun generateHighlights(
        videoUrl: String,
        config: HighlightConfig = HighlightConfig(),
        useCache: Boolean = true,
    ) {
        highlightsJob?.cancel()

        highlightsJob =
            viewModelScope.launch {
                var wasPlaying = false

                try {
                    if (useCache) {
                        val cached = manageHighlightCacheUseCase.getCachedHighlight(videoUrl)
                        if (cached != null) {
                            logger.info(TAG, "Using cached highlights")
                            _highlightsState.value = HighlightsState.Success(cached)
                            return@launch
                        }
                    }

                    wasPlaying = _uiState.value.mediaInfo.isPlaying
                    if (wasPlaying) {
                        playMediaUseCase.pause()
                        delay(500)
                    }

                    _highlightsState.value = HighlightsState.Analyzing("Initializing...", 0)
                    delay(800)

                    _highlightsState.value = HighlightsState.Analyzing("Extracting frames...", 15)
                    delay(800)

                    _highlightsState.value = HighlightsState.Analyzing("Analyzing motion...", 35)
                    delay(800)

                    _highlightsState.value = HighlightsState.Analyzing("Processing audio...", 55)
                    delay(800)

                    _highlightsState.value = HighlightsState.Analyzing("Detecting scenes...", 75)
                    delay(800)

                    _highlightsState.value =
                        HighlightsState.Analyzing("Finalizing highlights...", 90)

                    val uri = videoUrl.toUri()
                    val result = generateHighlightsUseCase.execute(uri, config)

                    result.onSuccess { highlights ->
                        logger.info(
                            TAG,
                            "Highlights generated: ${highlights.highlights.size} segments, " +
                                "analysis took ${highlights.analysisTimeMs}ms",
                        )

                        manageHighlightCacheUseCase.saveHighlight(videoUrl, highlights)
                        _highlightsState.value = HighlightsState.Success(highlights)
                    }

                    result.onFailure { error ->
                        logger.error(TAG, "Highlight generation failed", error)
                        val errorMessage =
                            when {
                                error.message?.contains("Cannot access video") == true -> {
                                    "Cannot access video. Please use a local video file."
                                }

                                error.message?.contains("Invalid video duration") == true -> {
                                    "Invalid video file or duration."
                                }

                                else -> {
                                    error.message ?: "Failed to generate highlights"
                                }
                            }
                        _highlightsState.value = HighlightsState.Error(errorMessage)
                    }
                } catch (e: CancellationException) {
                    logger.info(TAG, "Highlight generation cancelled by user")
                    _highlightsState.value = HighlightsState.Idle
                    throw e
                } catch (e: Exception) {
                    logger.error(TAG, "Unexpected error", e)
                    _highlightsState.value =
                        HighlightsState.Error(
                            e.message ?: "An unexpected error occurred",
                        )
                } finally {
                    if (wasPlaying) {
                        delay(300)
                        try {
                            playMediaUseCase.play()
                        } catch (e: Exception) {
                            logger.warning(TAG, "Failed to resume playback", e)
                        }
                    }
                }
            }
    }

    fun cancelHighlightGeneration() {
        highlightsJob?.cancel()
        _highlightsState.value = HighlightsState.Idle
        _showHighlightsBottomSheet.value = false
        logger.info(TAG, "Highlight generation cancelled")
    }

    fun playHighlights() {
        val state = _highlightsState.value
        if (state is HighlightsState.Success) {
            state.highlights.highlights.firstOrNull()?.let { segment ->
                seekTo(segment.startTimeMs)
                if (!_uiState.value.mediaInfo.isPlaying) {
                    playPause()
                }
                logger.debug(TAG, "Playing highlight at ${segment.startTimeMs}ms")
            }
        }
    }

    fun jumpToHighlight(index: Int) {
        val state = _highlightsState.value
        if (state is HighlightsState.Success) {
            state.highlights.highlights.getOrNull(index)?.let { segment ->
                seekTo(segment.startTimeMs)
                logger.debug(TAG, "Jumped to highlight $index at ${segment.startTimeMs}ms")
            }
        }
    }

    fun jumpToChapter(chapter: VideoChapter) {
        seekTo(chapter.startTimeMs)
        logger.debug(TAG, "Jumped to chapter: ${chapter.title}")
    }

    fun canNavigateNext(): Boolean = _uiState.value.hasNext

    fun canNavigatePrevious(): Boolean = _uiState.value.hasPrevious

    fun selectQuality(quality: VideoQuality) {
        viewModelScope.launch {
            try {
                playMediaUseCase.selectQuality(quality)
                logger.debug(TAG, "Quality selected: $quality")
            } catch (e: Exception) {
                logger.error(TAG, "Error selecting quality", e)
                _errorState.value = errorClassifier.classifyError(e)
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                playMediaUseCase.seekTo(position)
            } catch (e: Exception) {
                logger.error(TAG, "Error seeking", e)
                _errorState.value = errorClassifier.classifyError(e)
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
                logger.error(TAG, "Error setting volume", e)
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
            logger.info(TAG, "Retry attempt $retryCount/$maxRetryCount")

            viewModelScope.launch {
                try {
                    _uiState.value =
                        _uiState.value.copy(
                            mediaState = MediaState.Loading,
                            isLoading = true,
                        )
                    _errorState.value = null

                    retryMediaUseCase.execute()
                } catch (e: Exception) {
                    logger.error(TAG, "Error retrying", e)
                    _errorState.value = errorClassifier.classifyError(e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        } else {
            logger.warning(TAG, "Max retry attempts reached")
            _uiState.value =
                _uiState.value.copy(
                    mediaState =
                        MediaState.Error(
                            PlayerError.UnknownError("Maximum retry attempts reached"),
                        ),
                    isLoading = false,
                )
        }
    }

    fun updateMediaState(state: MediaState) {
        logger.debug(TAG, "State updated: ${state::class.simpleName}")

        _uiState.value =
            _uiState.value.copy(
                mediaState = state,
                isLoading = state is MediaState.Loading,
            )

        if (state is MediaState.Ready || state is MediaState.Playing) {
            retryCount = 0
            isMediaLoaded = true
            _errorState.value = null
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
        highlightsJob?.cancel()
        retryCount = 0
        isMediaLoaded = false
        _uiState.value = MediaPlayerUiState()
        _highlightsState.value = HighlightsState.Idle
        logger.debug(TAG, "Player reset")
    }

    fun clearHighlights() {
        highlightsJob?.cancel()
        generateHighlightsUseCase.clearCache()
        _highlightsState.value = HighlightsState.Idle
        _showHighlightsBottomSheet.value = false
        logger.debug(TAG, "Highlights cleared")
    }

    fun clearHighlightCache(videoUrl: String? = null) {
        viewModelScope.launch {
            if (videoUrl != null) {
                manageHighlightCacheUseCase.deleteHighlight(videoUrl)
                logger.debug(TAG, "Cleared cache for: $videoUrl")
            } else {
                manageHighlightCacheUseCase.clearCache()
                logger.debug(TAG, "Cleared all highlight cache")
            }
        }
    }

    fun getRecentHighlights(limit: Int = 10) {
        viewModelScope.launch {
            try {
                val recent = manageHighlightCacheUseCase.getRecentHighlights(limit)
                logger.debug(TAG, "Retrieved ${recent.size} recent highlights")
            } catch (e: Exception) {
                logger.error(TAG, "Error getting recent highlights", e)
            }
        }
    }

    fun toggleSubtitleSheet(show: Boolean) {
        _showSubtitleSheet.value = show
        logger.debug(TAG, "Subtitle sheet: $show")
    }

    fun searchSubtitles(
        videoUrl: String,
        videoName: String? = null,
    ) {
        subtitleJob?.cancel()

        subtitleJob =
            viewModelScope.launch {
                try {
                    logger.debug(TAG, "Searching subtitles for: $videoUrl")
                    val result = searchSubtitlesUseCase.autoSearch(videoUrl, videoName)
                    result.onSuccess { subtitles ->
                        _availableSubtitles.value = subtitles
                        logger.debug(TAG, "Found ${subtitles.size} subtitles")
                    }
                    result.onFailure { error ->
                        logger.error(TAG, "Error searching subtitles", error)
                    }
                } catch (e: Exception) {
                    logger.error(TAG, "Error searching subtitles", e)
                }
            }
    }

    fun selectSubtitle(track: SubtitleTrack?) {
        subtitleJob?.cancel()

        if (track == null) {
            _currentSubtitle.value = null
            manageSubtitleUseCase.clearSubtitle()
            playMediaUseCase.setSubtitleEnabled(false)
            logger.debug(TAG, "Subtitles disabled")
            return
        }

        subtitleJob =
            viewModelScope.launch {
                try {
                    val result = manageSubtitleUseCase.downloadSubtitle(track)

                    result.onSuccess { downloadResult ->
                        when (downloadResult) {
                            is SubtitleDownloadResult.Success -> {
                                val currentUrl = _uiState.value.currentUrl

                                if (currentUrl.isNotEmpty()) {
                                    val subtitleConfig =
                                        manageSubtitleUseCase
                                            .createSubtitleConfiguration(
                                                track,
                                                downloadResult.content,
                                            )

                                    playMediaUseCase.addSubtitleToCurrentMedia(subtitleConfig)

                                    _currentSubtitle.value = track
                                    _availableSubtitles.value =
                                        (_availableSubtitles.value + track).distinctBy { it.id }
                                } else {
                                    logger.warning(TAG, "Cannot load subtitle: no video loaded")
                                }
                            }

                            is SubtitleDownloadResult.Error -> {
                                _currentSubtitle.value = null
                            }

                            else -> {}
                        }
                    }
                    result.onFailure { error ->
                        logger.error(TAG, "Error selecting subtitle", error)
                        _currentSubtitle.value = null
                    }
                } catch (e: Exception) {
                    logger.error(TAG, "Error selecting subtitle", e)
                    _currentSubtitle.value = null
                }
            }
    }

    fun updateSubtitleStyle(style: SubtitleStyle) {
        viewModelScope.launch {
            try {
                val result = manageSubtitleUseCase.saveSubtitleStyle(style)
                result.onSuccess {
                    _subtitleStyle.value = style
                    logger.debug(TAG, "Subtitle style updated")
                }
                result.onFailure { error ->
                    logger.error(TAG, "Error updating subtitle style", error)
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error updating subtitle style", e)
            }
        }
    }

    fun clearSubtitleCache() {
        viewModelScope.launch {
            try {
                manageSubtitleUseCase.clearCache()
                logger.debug(TAG, "Subtitle cache cleared")
            } catch (e: Exception) {
                logger.error(TAG, "Error clearing subtitle cache", e)
            }
        }
    }

    fun loadExternalSubtitle(
        uri: android.net.Uri,
        language: String = "Unknown",
    ) {
        subtitleJob?.cancel()

        subtitleJob =
            viewModelScope.launch {
                try {
                    val result = playMediaUseCase.loadExternalSubtitle(uri, language)

                    when (result) {
                        is SubtitleDownloadResult.Success -> {
                            val currentUrl = _uiState.value.currentUrl

                            if (currentUrl.isNotEmpty()) {
                                logger.debug(TAG, "Creating subtitle configuration...")

                                val subtitleConfig =
                                    manageSubtitleUseCase
                                        .createSubtitleConfiguration(
                                            result.subtitle,
                                            result.content,
                                        )

                                playMediaUseCase.addSubtitleToCurrentMedia(subtitleConfig)

                                _currentSubtitle.value = result.subtitle
                                _availableSubtitles.value =
                                    (_availableSubtitles.value + result.subtitle).distinctBy { it.id }
                            } else {
                                logger.warning(TAG, "Cannot load subtitle: no video loaded")
                            }
                        }

                        is SubtitleDownloadResult.Error -> {
                            logger.error(TAG, "Failed to load external subtitle: ${result.message}")
                            _currentSubtitle.value = null
                        }

                        else -> {}
                    }
                } catch (e: Exception) {
                    logger.error(TAG, "Error loading external subtitle", e)
                    _currentSubtitle.value = null
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            currentJob?.cancel()
            highlightsJob?.cancel()
            subtitleJob?.cancel()
            playMediaUseCase.release()
            generateHighlightsUseCase.release()
            logger.debug(TAG, "ViewModel cleared")
        } catch (e: Exception) {
            logger.error(TAG, "Error releasing resources", e)
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
    val showEqualizer: Boolean = false,
    val currentTrackIndex: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)
