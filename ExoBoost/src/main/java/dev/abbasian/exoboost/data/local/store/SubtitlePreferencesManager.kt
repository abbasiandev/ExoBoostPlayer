package dev.abbasian.exoboost.data.local.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.abbasian.exoboost.domain.model.SubtitleSize
import dev.abbasian.exoboost.domain.model.SubtitleStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.subtitleDataStore: DataStore<Preferences> by preferencesDataStore(name = "subtitle_preferences")

class SubtitlePreferencesManager(
    private val context: Context,
) {
    companion object {
        private val TEXT_SIZE_KEY = stringPreferencesKey("subtitle_text_size")
        private val TEXT_COLOR_KEY = intPreferencesKey("subtitle_text_color")
        private val BACKGROUND_COLOR_KEY = intPreferencesKey("subtitle_background_color")
        private val BACKGROUND_OPACITY_KEY = floatPreferencesKey("subtitle_background_opacity")
        private val OUTLINE_COLOR_KEY = intPreferencesKey("subtitle_outline_color")
        private val OUTLINE_WIDTH_KEY = floatPreferencesKey("subtitle_outline_width")
        private val IS_BOLD_KEY = stringPreferencesKey("subtitle_is_bold")
        private val IS_ITALIC_KEY = stringPreferencesKey("subtitle_is_italic")
        private val PREFERRED_LANGUAGES_KEY = stringPreferencesKey("subtitle_preferred_languages")
        private val AUTO_LOAD_KEY = stringPreferencesKey("subtitle_auto_load")
    }

    val subtitleStyleFlow: Flow<SubtitleStyle> =
        context.subtitleDataStore.data.map { preferences ->
            SubtitleStyle(
                textSize =
                    preferences[TEXT_SIZE_KEY]?.let {
                        SubtitleSize.valueOf(it)
                    } ?: SubtitleSize.MEDIUM,
                textColor = preferences[TEXT_COLOR_KEY] ?: 0xFFFFFFFF.toInt(),
                backgroundColor = preferences[BACKGROUND_COLOR_KEY] ?: 0x80000000.toInt(),
                backgroundOpacity = preferences[BACKGROUND_OPACITY_KEY] ?: 0.5f,
                outlineColor = preferences[OUTLINE_COLOR_KEY] ?: 0xFF000000.toInt(),
                outlineWidth = preferences[OUTLINE_WIDTH_KEY] ?: 2f,
                isBold = preferences[IS_BOLD_KEY]?.toBoolean() ?: false,
                isItalic = preferences[IS_ITALIC_KEY]?.toBoolean() ?: false,
            )
        }

    val preferredLanguagesFlow: Flow<List<String>> =
        context.subtitleDataStore.data.map { preferences ->
            preferences[PREFERRED_LANGUAGES_KEY]?.split(",") ?: listOf("en", "fa", "ar")
        }

    val autoLoadFlow: Flow<Boolean> =
        context.subtitleDataStore.data.map { preferences ->
            preferences[AUTO_LOAD_KEY]?.toBoolean() ?: true
        }

    suspend fun saveSubtitleStyle(style: SubtitleStyle) {
        context.subtitleDataStore.edit { preferences ->
            preferences[TEXT_SIZE_KEY] = style.textSize.name
            preferences[TEXT_COLOR_KEY] = style.textColor
            preferences[BACKGROUND_COLOR_KEY] = style.backgroundColor
            preferences[BACKGROUND_OPACITY_KEY] = style.backgroundOpacity
            preferences[OUTLINE_COLOR_KEY] = style.outlineColor
            preferences[OUTLINE_WIDTH_KEY] = style.outlineWidth
            preferences[IS_BOLD_KEY] = style.isBold.toString()
            preferences[IS_ITALIC_KEY] = style.isItalic.toString()
        }
    }

    suspend fun saveTextSize(size: SubtitleSize) {
        context.subtitleDataStore.edit { preferences ->
            preferences[TEXT_SIZE_KEY] = size.name
        }
    }

    suspend fun saveTextColor(color: Int) {
        context.subtitleDataStore.edit { preferences ->
            preferences[TEXT_COLOR_KEY] = color
        }
    }

    suspend fun saveBackgroundColor(color: Int) {
        context.subtitleDataStore.edit { preferences ->
            preferences[BACKGROUND_COLOR_KEY] = color
        }
    }

    suspend fun saveBackgroundOpacity(opacity: Float) {
        context.subtitleDataStore.edit { preferences ->
            preferences[BACKGROUND_OPACITY_KEY] = opacity
        }
    }

    suspend fun savePreferredLanguages(languages: List<String>) {
        context.subtitleDataStore.edit { preferences ->
            preferences[PREFERRED_LANGUAGES_KEY] = languages.joinToString(",")
        }
    }

    suspend fun saveAutoLoad(autoLoad: Boolean) {
        context.subtitleDataStore.edit { preferences ->
            preferences[AUTO_LOAD_KEY] = autoLoad.toString()
        }
    }

    suspend fun resetToDefaults() {
        context.subtitleDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
