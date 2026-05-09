package com.alertanumero.mx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF54545A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5E5EA),
    onPrimary = Color(0xFF111111),
    background = Color(0xFF111111),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFC7C7CC)
)

@Composable
fun AlertaNumeroMXTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
