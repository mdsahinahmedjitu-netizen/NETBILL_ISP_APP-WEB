package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IspTealLight,
    onPrimary = SlateDarkBg,
    primaryContainer = IspTealDark,
    onPrimaryContainer = IspTealContainer,
    secondary = CyanAccent,
    onSecondary = SlateDarkBg,
    secondaryContainer = SlateDarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = IspAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = IspOnAmberContainer,
    onTertiaryContainer = IspAmberContainer,
    background = SlateDarkBg,
    onBackground = Color.White,
    surface = SlateDarkSurface,
    onSurface = Color.White,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8), // Slate400 for secondary text in dark mode
    outline = SlateDarkBorder,
    outlineVariant = Color(0xFF334155), // Slate700
    error = CoralWarning,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IspTealPrimary,
    onPrimary = Color.White,
    primaryContainer = IspTealContainer,
    onPrimaryContainer = IspOnTealContainer,
    secondary = IspBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = IspBlueContainer,
    onSecondaryContainer = IspOnBlueContainer,
    tertiary = IspAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = IspAmberContainer,
    onTertiaryContainer = IspOnAmberContainer,
    background = SlateBgLight,
    onBackground = Color(0xFF0F172A), // Slate900
    surface = SlateSurfaceLight,
    onSurface = Color(0xFF0F172A), // Slate900
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569), // Slate600
    outline = SlateBorderLight,
    outlineVariant = Color(0xFFE2E8F0), // Slate200
    error = CoralWarning,
    onError = Color.White
)

/**
 * Custom Material 3 AppTheme component for NetBill ISP Management tool.
 * Provides professional color schemes, typography, and component shapes.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve brand ISP palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * Alias theme wrapper for backward compatibility.
 */
@Composable
fun NetBillISPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AppTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
