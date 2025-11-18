package dev.abbasian.exoboost.data.local.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.abbasian.exoboost.presentation.ui.component.CustomPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "equalizer_settings")

class EqualizerPreferencesManager(
    private val context: Context,
) {
    companion object {
        private val CUSTOM_PRESETS_KEY = stringPreferencesKey("custom_presets")
        private val CURRENT_PRESET_KEY = stringPreferencesKey("current_preset")
        private val CURRENT_VALUES_KEY = stringPreferencesKey("current_values")
    }

    private val gson = Gson()

    val customPresets: Flow<List<CustomPreset>> =
        context.dataStore.data.map { preferences ->
            val json = preferences[CUSTOM_PRESETS_KEY]
            if (json != null) {
                try {
                    val type = object : TypeToken<List<CustomPreset>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

    val currentPresetName: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[CURRENT_PRESET_KEY] ?: ""
        }

    val currentValues: Flow<List<Float>> =
        context.dataStore.data.map { preferences ->
            val json = preferences[CURRENT_VALUES_KEY]
            if (json != null) {
                try {
                    val type = object : TypeToken<List<Float>>() {}.type
                    gson.fromJson(json, type) ?: List(8) { 0.5f }
                } catch (e: Exception) {
                    List(8) { 0.5f }
                }
            } else {
                List(8) { 0.5f }
            }
        }

    suspend fun saveCustomPresets(presets: List<CustomPreset>) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_PRESETS_KEY] = gson.toJson(presets)
        }
    }

    suspend fun saveCurrentPresetName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_PRESET_KEY] = name
        }
    }

    suspend fun saveCurrentValues(values: List<Float>) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_VALUES_KEY] = gson.toJson(values)
        }
    }

    suspend fun saveEqualizerState(
        presetName: String,
        values: List<Float>,
        customPresets: List<CustomPreset>,
    ) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_PRESET_KEY] = presetName
            preferences[CURRENT_VALUES_KEY] = gson.toJson(values)
            preferences[CUSTOM_PRESETS_KEY] = gson.toJson(customPresets)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
