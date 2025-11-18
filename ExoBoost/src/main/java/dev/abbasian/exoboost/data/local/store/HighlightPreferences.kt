package dev.abbasian.exoboost.data.local.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.highlightDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "highlight_preferences",
)

class HighlightPreferences(
    private val context: Context,
) {
    private object PreferenceKeys {
        val AUTO_GENERATE = booleanPreferencesKey("auto_generate_highlights")
        val ENABLE_CACHE = booleanPreferencesKey("enable_highlight_cache")
        val MAX_CACHE_SIZE = intPreferencesKey("max_cache_size")
        val CACHE_EXPIRY_DAYS = intPreferencesKey("cache_expiry_days")
        val LAST_GENERATED_VIDEO = stringPreferencesKey("last_generated_video")
        val TOTAL_HIGHLIGHTS_GENERATED = intPreferencesKey("total_highlights_generated")
        val PREFERRED_MIN_DURATION = longPreferencesKey("preferred_min_duration")
        val PREFERRED_MAX_DURATION = longPreferencesKey("preferred_max_duration")
        val AUDIO_THRESHOLD = floatPreferencesKey("audio_threshold")
        val MOTION_THRESHOLD = floatPreferencesKey("motion_threshold")
    }

    val autoGenerateHighlights: Flow<Boolean> =
        context.highlightDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[PreferenceKeys.AUTO_GENERATE] ?: false
            }

    val enableCache: Flow<Boolean> =
        context.highlightDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[PreferenceKeys.ENABLE_CACHE] ?: true
            }

    val maxCacheSize: Flow<Int> =
        context.highlightDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[PreferenceKeys.MAX_CACHE_SIZE] ?: 50
            }

    val cacheExpiryDays: Flow<Int> =
        context.highlightDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[PreferenceKeys.CACHE_EXPIRY_DAYS] ?: 30
            }

    val highlightConfig: Flow<HighlightConfigPrefs> =
        context.highlightDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                HighlightConfigPrefs(
                    minDuration = preferences[PreferenceKeys.PREFERRED_MIN_DURATION] ?: 3000L,
                    maxDuration = preferences[PreferenceKeys.PREFERRED_MAX_DURATION] ?: 30000L,
                    audioThreshold = preferences[PreferenceKeys.AUDIO_THRESHOLD] ?: 0.5f,
                    motionThreshold = preferences[PreferenceKeys.MOTION_THRESHOLD] ?: 0.6f,
                )
            }

    suspend fun setAutoGenerateHighlights(enabled: Boolean) {
        context.highlightDataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_GENERATE] = enabled
        }
    }

    suspend fun setEnableCache(enabled: Boolean) {
        context.highlightDataStore.edit { preferences ->
            preferences[PreferenceKeys.ENABLE_CACHE] = enabled
        }
    }

    suspend fun setMaxCacheSize(size: Int) {
        context.highlightDataStore.edit { preferences ->
            preferences[PreferenceKeys.MAX_CACHE_SIZE] = size
        }
    }

    suspend fun setCacheExpiryDays(days: Int) {
        context.highlightDataStore.edit { preferences ->
            preferences[PreferenceKeys.CACHE_EXPIRY_DAYS] = days
        }
    }

    suspend fun updateHighlightConfig(config: HighlightConfigPrefs) {
        context.highlightDataStore.edit { preferences ->
            preferences[PreferenceKeys.PREFERRED_MIN_DURATION] = config.minDuration
            preferences[PreferenceKeys.PREFERRED_MAX_DURATION] = config.maxDuration
            preferences[PreferenceKeys.AUDIO_THRESHOLD] = config.audioThreshold
            preferences[PreferenceKeys.MOTION_THRESHOLD] = config.motionThreshold
        }
    }

    suspend fun recordHighlightGeneration(videoUrl: String) {
        context.highlightDataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_GENERATED_VIDEO] = videoUrl
            val current = preferences[PreferenceKeys.TOTAL_HIGHLIGHTS_GENERATED] ?: 0
            preferences[PreferenceKeys.TOTAL_HIGHLIGHTS_GENERATED] = current + 1
        }
    }

    suspend fun clearAll() {
        context.highlightDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class HighlightConfigPrefs(
    val minDuration: Long,
    val maxDuration: Long,
    val audioThreshold: Float,
    val motionThreshold: Float,
)
