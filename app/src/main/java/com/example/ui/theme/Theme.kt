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
    primary = FocusPrimaryLight,
    onPrimary = Slate900,
    primaryContainer = FocusPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = FocusSecondaryLight,
    onSecondary = Slate900,
    tertiary = FocusAccent,
    background = Color(0xFF0B1120),
    surface = Slate900,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Slate800,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = FocusPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = FocusPrimaryDark,
    secondary = FocusSecondary,
    onSecondary = Color.White,
    tertiary = FocusAccent,
    background = Slate50,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200
)

@Composable
fun FocusFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        content = content
    )
}
