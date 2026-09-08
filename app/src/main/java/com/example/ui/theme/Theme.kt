package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Paper2,
    onPrimaryContainer = Ink,
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = GoldLight.copy(alpha = 0.2f),
    onSecondaryContainer = GoldDeep,
    tertiary = CrossRed,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = CardBg,
    onSurface = Ink,
    surfaceVariant = Paper2,
    onSurfaceVariant = MutedText,
    outline = LineBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = InkDark,
    primaryContainer = Ink,
    onPrimaryContainer = Paper,
    secondary = GoldLight,
    onSecondary = InkDark,
    tertiary = CrossRed,
    background = InkDark,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = Color(0xFF22352B),
    onSurfaceVariant = Color(0xFF9EAEA1),
    outline = Color(0xFF2E4639)
)

@Composable
fun MeskotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MyApplicationTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
