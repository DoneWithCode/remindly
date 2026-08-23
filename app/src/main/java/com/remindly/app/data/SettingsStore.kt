package com.remindly.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "remindly_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    /** Time of day all-day reminders fire. */
    val defaultHour: Int = 9,
    val defaultMinute: Int = 0,
    /** Hours after the due time before an open task is auto-completed. 0 = never. */
    val autoCompleteAfterHours: Int = 24,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showCompletedOnToday: Boolean = false
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val HOUR = intPreferencesKey("default_hour")
        val MINUTE = intPreferencesKey("default_minute")
        val AUTO_HOURS = intPreferencesKey("auto_complete_hours")
        val THEME = stringPreferencesKey("theme_mode")
        val SHOW_DONE_TODAY = booleanPreferencesKey("show_done_today")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultHour = prefs[Keys.HOUR] ?: 9,
            defaultMinute = prefs[Keys.MINUTE] ?: 0,
            autoCompleteAfterHours = prefs[Keys.AUTO_HOURS] ?: 24,
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            showCompletedOnToday = prefs[Keys.SHOW_DONE_TODAY] ?: false
        )
    }

    suspend fun setDefaultTime(hour: Int, minute: Int) {
        context.dataStore.edit { it[Keys.HOUR] = hour; it[Keys.MINUTE] = minute }
    }

    suspend fun setAutoCompleteHours(hours: Int) {
        context.dataStore.edit { it[Keys.AUTO_HOURS] = hours }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setShowCompletedOnToday(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_DONE_TODAY] = show }
    }
}
