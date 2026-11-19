package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.CosmicPalettes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.starChessPrefs by preferencesDataStore(name = "star_chess_prefs")

data class ShellPreferences(
    val preferredLanguage: String,
    val soundEnabled: Boolean,
    val bgmEnabled: Boolean,
    val lastCloudSyncMs: Long,
    val visitorMode: Boolean = false,
    val cosmicTheme: String = "default",
)

class PreferencesStore(context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val BGM_ENABLED = booleanPreferencesKey("bgm_enabled")
        val LAST_CLOUD_SYNC_MS = longPreferencesKey("last_cloud_sync_ms")
        val VISITOR_MODE = booleanPreferencesKey("visitor_mode")
        val COSMIC_THEME = stringPreferencesKey("cosmic_theme")
    }

    val preferencesFlow: Flow<ShellPreferences> = appContext.starChessPrefs.data.map { prefs ->
        ShellPreferences(
            preferredLanguage = normalizeLanguage(prefs[Keys.PREFERRED_LANGUAGE] ?: defaultLanguageForLocale()),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            bgmEnabled = prefs[Keys.BGM_ENABLED] ?: true,
            lastCloudSyncMs = prefs[Keys.LAST_CLOUD_SYNC_MS] ?: 0L,
            visitorMode = prefs[Keys.VISITOR_MODE] ?: false,
            cosmicTheme = CosmicPalettes.normalizeKey(prefs[Keys.COSMIC_THEME]),
        )
    }

    suspend fun setLanguage(languageCode: String) {
        val normalized = normalizeLanguage(languageCode)
        appContext.starChessPrefs.edit { it[Keys.PREFERRED_LANGUAGE] = normalized }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        appContext.starChessPrefs.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setBgmEnabled(enabled: Boolean) {
        appContext.starChessPrefs.edit { it[Keys.BGM_ENABLED] = enabled }
    }

    suspend fun setLastCloudSyncMs(timestampMs: Long) {
        appContext.starChessPrefs.edit { it[Keys.LAST_CLOUD_SYNC_MS] = timestampMs }
    }

    suspend fun setVisitorMode(enabled: Boolean) {
        appContext.starChessPrefs.edit { it[Keys.VISITOR_MODE] = enabled }
    }

    suspend fun setCosmicTheme(themeKey: String) {
        val normalized = CosmicPalettes.normalizeKey(themeKey)
        appContext.starChessPrefs.edit { it[Keys.COSMIC_THEME] = normalized }
    }

    companion object {
        val SUPPORTED_LANGUAGES = setOf("en", "zh-Hant", "zh-Hans", "ja")

        fun normalizeLanguage(code: String): String {
            return if (code in SUPPORTED_LANGUAGES) code else "zh-Hant"
        }

        fun defaultLanguageForLocale(): String {
            val locale = Locale.getDefault()
            return when (locale.language) {
                "ja" -> "ja"
                "en" -> "en"
                "zh" -> {
                    val script = locale.script
                    val country = locale.country
                    if (script == "Hans" || country in setOf("CN", "SG")) "zh-Hans" else "zh-Hant"
                }
                else -> "zh-Hant"
            }
        }
    }
}
