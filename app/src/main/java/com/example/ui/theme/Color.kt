package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand Colors for ISP Management Tool
val IspTealPrimary = Color(0xFF0D9488) // Deep Tech Teal
val IspTealDark = Color(0xFF0F766E)
val IspTealLight = Color(0xFF14B8A6)
val IspTealContainer = Color(0xFFCCFBF1)
val IspOnTealContainer = Color(0xFF115E59)

val IspBlueSecondary = Color(0xFF0284C7) // Network Blue
val IspBlueContainer = Color(0xFFE0F2FE)
val IspOnBlueContainer = Color(0xFF0369A1)

val IspAmberTertiary = Color(0xFFD97706) // Signal Amber
val IspAmberContainer = Color(0xFFFEF3C7)
val IspOnAmberContainer = Color(0xFF78350F)

// Light Interface Palette (Default NetBill ISP Theme Base Colors)
val SlateBgLight = Color(0xFFF8FAFC)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateSurfaceVariantLight = Color(0xFFF1F5F9)
val SlateBorderLight = Color(0xFFE2E8F0)

// Slate Typography Palette (Dynamic getters for perfect Light/Dark Mode contrast)
val Slate900: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val Slate800: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val Slate700: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val Slate600: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Slate500: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Slate400: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Slate200: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val Slate100: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val SlateBg: Color @Composable get() = MaterialTheme.colorScheme.background
val SlateSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val SlateSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val SlateBorder: Color @Composable get() = MaterialTheme.colorScheme.outline

// Dark Theme Surface/Bg (For technician night mode)
val SlateDarkBg = Color(0xFF0B0F19)
val SlateDarkSurface = Color(0xFF151C2C)
val SlateDarkSurfaceVariant = Color(0xFF1E293B)
val SlateDarkBorder = Color(0xFF2D3748)

// Sleek Theme Aliases (Dynamic based on active MaterialTheme.colorScheme)
val SleekBg: Color @Composable get() = MaterialTheme.colorScheme.background
val SleekSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val SleekCard: Color @Composable get() = MaterialTheme.colorScheme.surface
val SleekBorder: Color @Composable get() = MaterialTheme.colorScheme.outline
val SleekTextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val SleekTextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val Teal600 = IspTealPrimary
val Teal700 = IspTealDark
val Teal500 = IspTealLight
val Teal100 = IspTealContainer
val Teal50: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)

val Navy900: Color @Composable get() = MaterialTheme.colorScheme.background
val Navy800: Color @Composable get() = MaterialTheme.colorScheme.surface
val Navy700: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val ElectricBlue = IspBlueSecondary
val CyanAccent = Color(0xFF06B6D4)

val BkashPink = Color(0xFFE2136E)
val NagadOrange = Color(0xFFF7921E)
val RocketViolet = Color(0xFF8C3494)

val EmeraldSuccess = Color(0xFF10B981)
val SuccessGreen = EmeraldSuccess
val CoralWarning = Color(0xFFEF4444)
val AmberAlert = Color(0xFFF59E0B)
val Teal200 = Color(0xFF99F6E4)
val Slate300 = Color(0xFFCBD5E1)

val LightSurfaceBg: Color @Composable get() = MaterialTheme.colorScheme.background
val LightCardBg: Color @Composable get() = MaterialTheme.colorScheme.surface

