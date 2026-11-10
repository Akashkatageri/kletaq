package com.kletaq.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Pure Monochrome Tokens (Exclusive for Onboarding / Auth Screens)
val AccentLight = Color(0xFF000000)
val AccentDark = Color(0xFFFFFFFF)

// Rich Oceanic Teal Tokens (Balanced Cyan-Teal to eliminate forest/pine green tones)
val TealPrimary = Color(0xFF0A8491)         // True vibrant cyan-teal (#0A8491)
val TealPrimaryDark = Color(0xFF2DD4BF)     // Electric turquoise-teal for dark theme
val TealMintBackground = Color(0xFFE0F7F6)  // Fresh cyan-mint pill background
val TealMintBorder = Color(0xFFB2EBF2)      // Subtle cyan-mint border
val StreakOrangeAccent = Color(0xFFC2410C)  // Rich burnt orange for active streak
val FrozenIceColor = Color(0xFF0284C7)      // Frosty ice blue for frozen streak days
val FrozenIceBg = Color(0xFFE0F2FE)         // Ice blue pill/circle background

// Unified App-wide Brand Tokens (The entire app theme is Teal)
val PrimaryAccentColor = TealPrimary        // Primary brand accent across all screens
val PrimaryAccentDark = TealPrimaryDark
val PurpleAccent = TealPrimary              // Replaced purple/indigo with Teal across the entire app
val StreakOrange = StreakOrangeAccent       // Streak Orange Accent
val XpAmber = Color(0xFFB45309)             // Amber Accent for XP
val BacklogRed = Color(0xFFEF4444)          // Warning Red for Backlogs

// Status Colors
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Light Theme Ink & Paper Tokens
val AppBackgroundLight = Color(0xFFF4F6FB)  // Soft cool grey-blue background from mock
val CardSurfaceLight = Color(0xFFFFFFFF)     // Pure white card sheet surface
val SurfaceVariantLight = Color(0xFFF4F3EF)
val BorderLight = Color(0xFFE5E7EB)         // Clean light border
val TextPrimaryLight = Color(0xFF111827)    // Dark graphite text
val TextSecondaryLight = Color(0xFF6B7280)  // Secondary grey text

// Dark Theme Ink & Paper Tokens
val AppBackgroundDark = Color(0xFF121214)   // Dark notebook paper background
val CardSurfaceDark = Color(0xFF1C1C1E)      // Dark paper sheet surface
val SurfaceVariantDark = Color(0xFF262629)
val BorderDark = Color(0xFF2E2E38)          // Subtle dark border
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

val isAppInDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
