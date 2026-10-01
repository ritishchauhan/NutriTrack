package com.example.macro_tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = NutritrackSurface,
    primaryContainer = BrandGreenPill,
    onPrimaryContainer = BrandGreenDark,
    secondary = NutritrackDark,
    onSecondary = NutritrackSurface,
    background = NutritrackBg,
    onBackground = TextPrimary,
    surface = NutritrackSurface,
    onSurface = TextPrimary,
    surfaceVariant = NutritrackBorderLight,
    onSurfaceVariant = TextSecondary,
    outline = NutritrackBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenAccent,
    onPrimary = NutritrackSurface,
    primaryContainer = NutritrackDark,
    onPrimaryContainer = BrandGreenPill,
    secondary = BrandGreenAccent,
    onSecondary = NutritrackDark,
    background = NutritrackDark,
    onBackground = NutritrackSurface,
    surface = Color(0xFF132D23),
    onSurface = NutritrackSurface,
    surfaceVariant = Color(0xFF1D3E32),
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF2A5042)
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}