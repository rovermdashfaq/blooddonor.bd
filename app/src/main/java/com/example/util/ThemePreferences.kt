package com.example.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppColorTheme(
    val label: String,
    val primary: Color,
    val dark: Color,
    val lightPrimary: Color,
    val containerLight: Color,
    val containerDark: Color
) {
    CRIMSON(
        label = "Crimson Red",
        primary = Color(0xFFB71C1C),
        dark = Color(0xFF8B0000),
        lightPrimary = Color(0xFFFF8A80),
        containerLight = Color(0xFFFFDAD6),
        containerDark = Color(0xFF7F0000)
    ),
    MEDICAL_BLUE(
        label = "Medical Blue",
        primary = Color(0xFF1565C0),
        dark = Color(0xFF0D47A1),
        lightPrimary = Color(0xFF9ECAFF),
        containerLight = Color(0xFFD6E4FF),
        containerDark = Color(0xFF003258)
    ),
    EMERALD(
        label = "Emerald Care",
        primary = Color(0xFF2E7D32),
        dark = Color(0xFF1B5E20),
        lightPrimary = Color(0xFF81C784),
        containerLight = Color(0xFFC8E6C9),
        containerDark = Color(0xFF0A3D0E)
    ),
    TEAL_HOSPITAL(
        label = "Hospital Teal",
        primary = Color(0xFF00695C),
        dark = Color(0xFF004D40),
        lightPrimary = Color(0xFF80CBC4),
        containerLight = Color(0xFFB2DFDB),
        containerDark = Color(0xFF00332C)
    ),
    ROYAL_VIOLET(
        label = "Royal Violet",
        primary = Color(0xFF6A1B9A),
        dark = Color(0xFF4A148C),
        lightPrimary = Color(0xFFCE93D8),
        containerLight = Color(0xFFF3E5F5),
        containerDark = Color(0xFF38006B)
    ),
    AMBER_CORAL(
        label = "Amber Coral",
        primary = Color(0xFFD84315),
        dark = Color(0xFFBF360C),
        lightPrimary = Color(0xFFFFAB91),
        containerLight = Color(0xFFFFCCBC),
        containerDark = Color(0xFF6D1B00)
    ),
    ROSE_RUBY(
        label = "Ruby Rose",
        primary = Color(0xFFAD1457),
        dark = Color(0xFF880E4F),
        lightPrimary = Color(0xFFF48FB1),
        containerLight = Color(0xFFFCE4EC),
        containerDark = Color(0xFF560027)
    ),
    DEEP_INDIGO(
        label = "Deep Indigo",
        primary = Color(0xFF283593),
        dark = Color(0xFF1A237E),
        lightPrimary = Color(0xFF9FA8DA),
        containerLight = Color(0xFFC5CAE9),
        containerDark = Color(0xFF0E1454)
    ),
    IMPERIAL_GOLD(
        label = "Imperial Gold",
        primary = Color(0xFF9E6B00),
        dark = Color(0xFF6D4800),
        lightPrimary = Color(0xFFFFD54F),
        containerLight = Color(0xFFFFECB3),
        containerDark = Color(0xFF4B3100)
    ),
    DYNAMIC_SYSTEM(
        label = "Material You",
        primary = Color(0xFFB71C1C),
        dark = Color(0xFF8B0000),
        lightPrimary = Color(0xFFFF8A80),
        containerLight = Color(0xFFFFDAD6),
        containerDark = Color(0xFF7F0000)
    )
}

data class ThemeSettings(
    val mode: AppThemeMode = AppThemeMode.SYSTEM,
    val color: AppColorTheme = AppColorTheme.CRIMSON
)

object ThemeController {
    private const val PREFS_NAME = "blood_donor_bd_theme_prefs"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_COLOR = "theme_color"

    private val _settings = MutableStateFlow(ThemeSettings())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(KEY_MODE, AppThemeMode.SYSTEM.name)
        val colorName = prefs.getString(KEY_COLOR, AppColorTheme.CRIMSON.name)
        val mode = AppThemeMode.entries.firstOrNull { it.name == modeName } ?: AppThemeMode.SYSTEM
        val color = AppColorTheme.entries.firstOrNull { it.name == colorName } ?: AppColorTheme.CRIMSON
        _settings.value = ThemeSettings(mode = mode, color = color)
        initialized = true
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(mode = mode)
    }

    fun setColorTheme(context: Context, color: AppColorTheme) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_COLOR, color.name).apply()
        _settings.value = _settings.value.copy(color = color)
    }
}
