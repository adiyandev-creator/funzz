package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ColorPlayer2Blue,
    onPrimary = Color.White,
    primaryContainer = CardNavyBackground,
    onPrimaryContainer = Color.White,
    background = AppDarkBackground,
    onBackground = TextLightPrimary,
    surface = AppDarkSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = AppDarkSurfaceVariant,
    onSurfaceVariant = TextLightSecondary,
    outline = CardNavyBorder,
    outlineVariant = CardNavyBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ColorPlayer2Blue,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = TextDarkPrimary,
    background = AppLightBackground,
    onBackground = TextDarkPrimary,
    surface = AppLightSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = AppLightSurfaceVariant,
    onSurfaceVariant = TextDarkSecondary,
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFCBD5E1)
)

object PlayerPalette {
    val player1 = ColorPlayer1Red
    val player2 = ColorPlayer2Blue
    val player3 = ColorPlayer3Green
    val player4 = ColorPlayer4Yellow
    val teamPurple = ColorTeamPurple

    fun getColor(playerIndex: Int): Color = when (playerIndex) {
        0 -> player1
        1 -> player2
        2 -> player3
        3 -> player4
        else -> player1
    }
}

@Composable
fun PartyGridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
