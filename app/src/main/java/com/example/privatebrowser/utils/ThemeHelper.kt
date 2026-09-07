package com.example.privatebrowser.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.privatebrowser.model.AccentColor
import com.example.privatebrowser.model.ThemeMode

object ThemeHelper {

    fun applyTheme(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun getAccentColorInt(context: Context, accentColor: AccentColor): Int {
        return ContextCompat.getColor(context, accentColor.colorResId)
    }
}
