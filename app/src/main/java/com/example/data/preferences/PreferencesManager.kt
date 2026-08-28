package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calcpro_settings")

enum class AppTheme { DARK, LIGHT, SYSTEM }
enum class AccentColor { BLUE, CYAN, PURPLE, GREEN, ORANGE }

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("app_theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME]) {
            "LIGHT" -> AppTheme.LIGHT
            "SYSTEM" -> AppTheme.SYSTEM
            else -> AppTheme.DARK // Default dark premium theme
        }
    }

    val accentColorFlow: Flow<AccentColor> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.ACCENT_COLOR]) {
            "CYAN" -> AccentColor.CYAN
            "PURPLE" -> AccentColor.PURPLE
            "GREEN" -> AccentColor.GREEN
            "ORANGE" -> AccentColor.ORANGE
            else -> AccentColor.BLUE // Default electric blue
        }
    }

    val hapticsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_ENABLED] ?: true
    }

    val soundFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SOUND_ENABLED] ?: false
    }

    val angleUnitFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ANGLE_UNIT] ?: "DEG"
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAccentColor(accentColor: AccentColor) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = accentColor.name }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setAngleUnit(unit: String) {
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit }
    }
}
