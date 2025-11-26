package dev.abbasian.exoboost.presentation.state

import dev.abbasian.exoboost.domain.model.VideoHighlights

sealed class HighlightsState {
    object Idle : HighlightsState()

    data class Analyzing(
        val progress: String,
        val progressPercent: Int = 0,
    ) : HighlightsState()

    data class Success(
        val highlights: VideoHighlights,
    ) : HighlightsState()

    data class Error(
        val message: String,
    ) : HighlightsState()
}
