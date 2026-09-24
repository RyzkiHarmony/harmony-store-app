package com.harmony.tokoharmony.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Toko Harmony POS - Design Tokens from Stitch UI/UX Modern Minimalis
 * Palette: Emerald Deep Teal, Mint Forest Green, Warm Amber, Slate Grayscale, Coral Alert.
 */

// Primary - Emerald Deep Teal
val EmeraldPrimary = Color(0xFF005C55)
val EmeraldPrimaryContainer = Color(0xFF0F766E)
val EmeraldOnPrimary = Color(0xFFFFFFFF)
val EmeraldOnPrimaryContainer = Color(0xFFA3FAEF)
val EmeraldPrimaryFixed = Color(0xFF9CF2E8)
val EmeraldPrimaryFixedDim = Color(0xFF80D5CB)
val EmeraldOnPrimaryFixedVariant = Color(0xFF00504A)

// Secondary - Mint Forest Green
val MintSecondary = Color(0xFF006C49)
val MintSecondaryContainer = Color(0xFF6CF8BB)
val MintOnSecondary = Color(0xFFFFFFFF)
val MintOnSecondaryContainer = Color(0xFF00714D)
val MintSecondaryFixed = Color(0xFF6FFBBE)
val MintSecondaryFixedDim = Color(0xFF4EDEA3)
val MintOnSecondaryFixed = Color(0xFF002113)

// Tertiary - Warm Amber
val AmberTertiary = Color(0xFF734700)
val AmberTertiaryContainer = Color(0xFF945D00)
val AmberOnTertiary = Color(0xFFFFFFFF)
val AmberOnTertiaryContainer = Color(0xFFFFE6CC)
val AmberTertiaryFixed = Color(0xFFFFDDB8)
val AmberTertiaryFixedDim = Color(0xFFFFB95F)
val AmberOnTertiaryFixed = Color(0xFF2A1700)

// Surfaces & Background
val SurfaceBackground = Color(0xFFFAF8FF)
val SurfaceLowest = Color(0xFFFFFFFF)
val SurfaceLow = Color(0xFFF2F3FF)
val SurfaceContainer = Color(0xFFEAEDFF)
val SurfaceContainerHigh = Color(0xFFE2E7FF)
val SurfaceContainerHighest = Color(0xFFDAE2FD)

// Text & Outline
val TextOnSurface = Color(0xFF131B2E)
val TextOnSurfaceVariant = Color(0xFF3E4947)
val OutlineBorder = Color(0xFF6E7977)
val OutlineVariant = Color(0xFFBDC9C6)

// Error & Warnings
val CoralError = Color(0xFFBA1A1A)
val CoralErrorContainer = Color(0xFFFFDAD6)
val CoralOnError = Color(0xFFFFFFFF)
val CoralOnErrorContainer = Color(0xFF93000A)

// Centralized Semantic Status & Badge Tokens
val StatusSuccessBg = Color(0xFFE8F5E9)
val StatusSuccessText = Color(0xFF2E7D32)
val StatusWarningBg = Color(0xFFFFF3E0)
val StatusWarningText = Color(0xFFE65100)
val StatusErrorBg = Color(0xFFFFEBEE)
val StatusErrorText = Color(0xFFC62828)

// Semantic Role Aliases
val StatusActiveBg = StatusSuccessBg
val StatusActiveText = StatusSuccessText
val StatusInactiveBg = StatusErrorBg
val StatusInactiveText = StatusErrorText

// Backward Compatibility Aliases for existing code
val PrimaryBlue = EmeraldPrimary
val PrimaryBlueVariant = EmeraldPrimaryContainer
val SecondaryTeal = MintSecondary
val BackgroundLight = SurfaceBackground
val SurfaceLight = SurfaceLowest
val TextPrimary = TextOnSurface
val TextSecondary = TextOnSurfaceVariant
val SuccessGreen = MintSecondary
val WarningOrange = AmberTertiaryContainer
val ErrorRed = CoralError
val BorderLight = OutlineVariant

val OnSurfaceDark = TextOnSurface
val OnMintSecondaryContainer = MintOnSecondaryContainer
val SurfaceContainerLow = SurfaceLow
