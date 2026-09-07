package com.example.privatebrowser.model

import com.example.privatebrowser.R

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ToolbarPosition {
    TOP,
    BOTTOM
}

enum class AccentColor(val colorResId: Int, val hexName: String) {
    BLUE(R.color.accent_blue, "Blue"),
    EMERALD(R.color.accent_emerald, "Emerald"),
    PURPLE(R.color.accent_purple, "Purple"),
    AMBER(R.color.accent_amber, "Amber"),
    ROSE(R.color.accent_rose, "Rose"),
    CYAN(R.color.accent_cyan, "Cyan");

    companion object {
        fun fromName(name: String): AccentColor {
            return values().find { it.name.equals(name, ignoreCase = true) } ?: BLUE
        }
    }
}
