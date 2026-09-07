package com.example.privatebrowser.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.privatebrowser.model.AccentColor
import com.example.privatebrowser.model.SearchEngine
import com.example.privatebrowser.model.SearchEngineType
import com.example.privatebrowser.model.ThemeMode
import com.example.privatebrowser.model.ToolbarPosition

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getAccentColor(): AccentColor {
        val name = prefs.getString(KEY_ACCENT_COLOR, AccentColor.BLUE.name) ?: AccentColor.BLUE.name
        return AccentColor.fromName(name)
    }

    fun setAccentColor(accentColor: AccentColor) {
        prefs.edit().putString(KEY_ACCENT_COLOR, accentColor.name).apply()
    }

    fun getToolbarPosition(): ToolbarPosition {
        val name = prefs.getString(KEY_TOOLBAR_POS, ToolbarPosition.BOTTOM.name) ?: ToolbarPosition.BOTTOM.name
        return try {
            ToolbarPosition.valueOf(name)
        } catch (e: Exception) {
            ToolbarPosition.BOTTOM
        }
    }

    fun setToolbarPosition(pos: ToolbarPosition) {
        prefs.edit().putString(KEY_TOOLBAR_POS, pos.name).apply()
    }

    fun isBottomToolbarVisible(): Boolean {
        return prefs.getBoolean(KEY_TOOLBAR_VISIBLE, true)
    }

    fun setBottomToolbarVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_TOOLBAR_VISIBLE, visible).apply()
    }

    fun isCompactMode(): Boolean {
        return prefs.getBoolean(KEY_COMPACT_MODE, false)
    }

    fun setCompactMode(compact: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT_MODE, compact).apply()
    }

    fun getSearchEngine(): SearchEngine {
        val typeStr = prefs.getString(KEY_SEARCH_ENGINE_TYPE, SearchEngineType.GOOGLE.name) ?: SearchEngineType.GOOGLE.name
        val customUrl = prefs.getString(KEY_CUSTOM_SEARCH_URL, "") ?: ""
        val type = try {
            SearchEngineType.valueOf(typeStr)
        } catch (e: Exception) {
            SearchEngineType.GOOGLE
        }
        return SearchEngine(type, customUrl)
    }

    fun setSearchEngine(engine: SearchEngine) {
        prefs.edit()
            .putString(KEY_SEARCH_ENGINE_TYPE, engine.type.name)
            .putString(KEY_CUSTOM_SEARCH_URL, engine.customUrl)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "private_browser_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_TOOLBAR_POS = "toolbar_position"
        private const val KEY_TOOLBAR_VISIBLE = "toolbar_visible"
        private const val KEY_COMPACT_MODE = "compact_mode"
        private const val KEY_SEARCH_ENGINE_TYPE = "search_engine_type"
        private const val KEY_CUSTOM_SEARCH_URL = "custom_search_url"
    }
}
