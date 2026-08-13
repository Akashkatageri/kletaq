package com.kletaq.app.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

object ThemeManager {
    var currentThemeMode by mutableStateOf(ThemeMode.LIGHT)

    fun setTheme(mode: ThemeMode) {
        currentThemeMode = mode
    }

    fun setThemeByName(name: String) {
        currentThemeMode = when (name.lowercase()) {
            "dark" -> ThemeMode.DARK
            "system" -> ThemeMode.SYSTEM
            else -> ThemeMode.LIGHT
        }
    }
}
