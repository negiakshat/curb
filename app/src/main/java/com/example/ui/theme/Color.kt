package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Bento Grid Design System Palette (Warm Terracotta, Espresso, Sand & Cream)
val BentoCanvas = Color(0xFFFDF8F6)          // Warm creamy off-white background
val BentoPrimary = Color(0xFF8F4C38)         // Rich terracotta / rust clay accent
val BentoPrimaryDark = Color(0xFF3A0B01)     // Deep roasted espresso umber
val BentoPeach = Color(0xFFFFDAD1)           // Soft warm peach hero tint
val BentoSand = Color(0xFFF3E9E5)            // Warm sand card container
val BentoBeige = Color(0xFFF9EEE8)           // Warm light biscuit container
val BentoTextPrimary = Color(0xFF1F1B1A)     // Deep charcoal-umber text
val BentoTextSecondary = Color(0xFF85736E)   // Warm muted brown-gray
val BentoTextDark = Color(0xFF51433F)        // Mid brown-charcoal
val BentoBorder = Color(0xFFEDE0DC)          // Subtle warm divider/border
val BentoBorderStrong = Color(0xFFD6C2BC)    // Structured border / chart bar
val BentoWhite = Color(0xFFFFFFFF)           // Pure white elements & cards

// Compatibility aliases for Curb components
val CurbBlack = BentoPrimaryDark
val CurbWhite = BentoWhite
val CurbBackground = BentoCanvas
val CurbSurface = BentoWhite
val CurbSurfaceVariant = BentoSand
val CurbSurfaceBeige = BentoBeige
val CurbSurfacePeach = BentoPeach
val CurbOnSurface = BentoTextPrimary
val CurbOnSurfaceVariant = BentoTextSecondary
val CurbOutline = BentoBorderStrong
val CurbOutlineVariant = BentoBorder

// Bento Semantic Status Badges
val CurbSuccess = Color(0xFF2E7D32)
val CurbSuccessContainer = Color(0xFFE8F5E9)
val CurbOnSuccess = Color(0xFFFFFFFF)

val CurbError = Color(0xFFBA1A1A)
val CurbErrorContainer = Color(0xFFFFDAD6)
val CurbOnError = Color(0xFFFFFFFF)

val CurbWarning = Color(0xFF9E4800)
val CurbWarningContainer = Color(0xFFFFDCBE)
val CurbOnWarning = Color(0xFFFFFFFF)

val CurbNeutralDark = BentoPrimaryDark
val CurbNeutralLight = BentoSand
val CurbDivider = BentoBorder

