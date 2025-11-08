package dev.abbasian.exoboost.presentation.state

import dev.abbasian.exoboost.domain.model.VideoHighlights

sealed class HighlightsState {
    object Idle : HighlightsState()

    data class Analyzing(
        val progress: String,
    ) : HighlightsState()

    data class Success(
        val highlights: VideoHighlights,
    ) : HighlightsState()

    data class Error(
        val message: String,
    ) : HighlightsState()
}
