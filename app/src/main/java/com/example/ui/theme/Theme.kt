package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisObsidianDark,
    primaryContainer = JarvisSurfaceElevated,
    onPrimaryContainer = JarvisCyan,
    secondary = JarvisElectricBlue,
    onSecondary = JarvisWhite,
    secondaryContainer = JarvisSurfaceDark,
    onSecondaryContainer = JarvisCyan,
    tertiary = JarvisGold,
    onTertiary = JarvisObsidianDark,
    background = JarvisObsidianDark,
    onBackground = JarvisWhite,
    surface = JarvisSurfaceDark,
    onSurface = JarvisWhite,
    surfaceVariant = JarvisSurfaceElevated,
    onSurfaceVariant = JarvisMuted,
    outline = JarvisBorder,
    error = JarvisRed,
    onError = JarvisWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
