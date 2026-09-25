package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Curb is strictly LIGHT MODE ONLY with a clean monochrome identity
private val CurbLightColorScheme = lightColorScheme(
    primary = CurbBlack,
    onPrimary = CurbWhite,
    primaryContainer = CurbSurfaceVariant,
    onPrimaryContainer = CurbOnSurface,
    secondary = CurbBlack,
    onSecondary = CurbWhite,
    secondaryContainer = CurbSurfaceVariant,
    onSecondaryContainer = CurbOnSurface,
    tertiary = CurbNeutralDark,
    onTertiary = CurbWhite,
    background = CurbBackground,
    onBackground = CurbOnSurface,
    surface = CurbSurface,
    onSurface = CurbOnSurface,
    surfaceVariant = CurbSurfaceVariant,
    onSurfaceVariant = CurbOnSurfaceVariant,
    outline = CurbOutline,
    outlineVariant = CurbOutlineVariant,
    error = CurbError,
    onError = CurbOnError,
    errorContainer = CurbErrorContainer,
    onErrorContainer = CurbError
)

@Composable
fun CurbTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CurbLightColorScheme,
        typography = CurbTypography,
        shapes = CurbShapes,
        content = content
    )
}
