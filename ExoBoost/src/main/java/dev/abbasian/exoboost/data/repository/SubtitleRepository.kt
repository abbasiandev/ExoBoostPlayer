package dev.abbasian.exoboost.data.repository

import dev.abbasian.exoboost.data.local.store.SubtitlePreferencesManager
import dev.abbasian.exoboost.data.manager.SubtitleManager
import dev.abbasian.exoboost.domain.model.SubtitleDownloadResult
import dev.abbasian.exoboost.domain.model.SubtitleQuery
import dev.abbasian.exoboost.domain.model.SubtitleStyle
import dev.abbasian.exoboost.domain.model.SubtitleTrack
import kotlinx.coroutines.flow.Flow

class SubtitleRepository(
    private val subtitleManager: SubtitleManager,
    private val preferencesManager: SubtitlePreferencesManager,
) {
    suspend fun searchSubtitles(query: SubtitleQuery): List<SubtitleTrack> = subtitleManager.searchSubtitles(query)

    suspend fun autoSearchSubtitles(
        videoUrl: String,
        videoName: String? = null,
    ): List<SubtitleTrack> = subtitleManager.autoSearchSubtitles(videoUrl, videoName)

    suspend fun downloadSubtitle(track: SubtitleTrack): SubtitleDownloadResult = subtitleManager.downloadSubtitle(track)

    fun getAvailableSubtitles(): Flow<List<SubtitleTrack>> = subtitleManager.availableSubtitles

    fun getCurrentSubtitle(): Flow<SubtitleTrack?> = subtitleManager.currentSubtitle

    fun getSubtitleStyle(): Flow<SubtitleStyle> = preferencesManager.subtitleStyleFlow

    suspend fun saveSubtitleStyle(style: SubtitleStyle) {
        preferencesManager.saveSubtitleStyle(style)
    }

    fun getPreferredLanguages(): Flow<List<String>> = preferencesManager.preferredLanguagesFlow

    suspend fun savePreferredLanguages(languages: List<String>) {
        preferencesManager.savePreferredLanguages(languages)
    }

    fun clearSubtitle() {
        subtitleManager.clearSubtitle()
    }

    fun clearCache() {
        subtitleManager.clearCache()
    }

    fun getCacheSize(): Long = subtitleManager.getCacheSize()
}
