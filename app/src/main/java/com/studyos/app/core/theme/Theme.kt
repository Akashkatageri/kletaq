package com.studyos.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Pure Monochrome Tokens (Exclusive for Onboarding / Auth Screens)
val AccentLight = Color(0xFF000000)
val AccentDark = Color(0xFFFFFFFF)

// Ink & Paper Color System Tokens
val PrimaryAccentColor = Color(0xFF6366F1) // Minimal Indigo Accent
val PrimaryAccentDark = Color(0xFF6366F1)
val PurpleAccent = Color(0xFF6366F1)        // Minimal Indigo Accent
val StreakOrange = Color(0xFF6366F1)        // Indigo Accent for Streaks
val XpAmber = Color(0xFF6366F1)             // Indigo Accent for XP
val BacklogRed = Color(0xFFEF4444)          // Warning Red for Backlogs

// Status Colors
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Light Theme Ink & Paper Tokens
val AppBackgroundLight = Color(0xFFFAF9F6)  // Off-white paper background
val CardSurfaceLight = Color(0xFFFFFFFF)     // White paper sheet surface
val SurfaceVariantLight = Color(0xFFF4F3EF)
val BorderLight = Color(0xFF27272A)         // Thin sketch ink border
val TextPrimaryLight = Color(0xFF1A1A1A)    // Black ink text
val TextSecondaryLight = Color(0xFF52525B)  // Graphite pencil text

// Dark Theme Ink & Paper Tokens
val AppBackgroundDark = Color(0xFF121214)   // Dark notebook paper background
val CardSurfaceDark = Color(0xFF1C1C1E)      // Dark paper sheet surface
val SurfaceVariantDark = Color(0xFF262629)
val BorderDark = Color(0xFFE4E4E7)          // Thin light sketch border
val TextPrimaryDark = Color(0xFFF4F4F5)     // White ink text
val TextSecondaryDark = Color(0xFFA1A1AA)   // Muted pencil text

val AppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val CardSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val BorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccentColor,
    onPrimary = Color.White,
    secondary = PurpleAccent,
    background = AppBackgroundLight,
    surface = CardSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccentDark,
    onPrimary = Color(0xFF0F172A),
    secondary = PurpleAccent,
    background = AppBackgroundDark,
    surface = CardSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = ErrorRed
)

@Composable
fun KletaqTheme(
    themeMode: ThemeMode = ThemeManager.currentThemeMode,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun StudyOSTheme(
    themeMode: ThemeMode = ThemeManager.currentThemeMode,
    content: @Composable () -> Unit
) {
    KletaqTheme(themeMode = themeMode, content = content)
}
