package com.example.macro_tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalDarkTheme = compositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECFDF5),
    onPrimaryContainer = BrandGreenDark,
    secondary = Color(0xFF0C241B),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAF8),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF0F4F1),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE5E9E6)
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenAccent,
    onPrimary = Color(0xFF06150E),
    primaryContainer = Color(0xFF132B20),
    onPrimaryContainer = BrandGreenAccent,
    secondary = BrandGreenAccent,
    onSecondary = Color(0xFF06150E),
    background = Color(0xFF09120E),
    onBackground = Color(0xFFF2F6F3),
    surface = Color(0xFF111E18),
    onSurface = Color(0xFFF2F6F3),
    surfaceVariant = Color(0xFF162720),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1B3127)
)

@Composable
fun Macro_trackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}