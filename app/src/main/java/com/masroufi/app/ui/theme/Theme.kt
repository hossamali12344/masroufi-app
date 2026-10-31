package com.masroufi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    secondary = DarkGreen,
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onSurface = TextPrimary,
    error = ExpenseRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    secondary = LightGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = Color(0xFFECECEC),
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun MasroufiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
