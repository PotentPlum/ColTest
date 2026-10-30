package com.potentplum.dextracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Monospace "screen" text, used for anything shown on the Pokédex displays. */
val ScreenFont = FontFamily.Monospace

object DexText {
    val Title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        letterSpacing = 1.5.sp,
        color = Color.White,
    )
    val Label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    )
    val Screen = TextStyle(
        fontFamily = ScreenFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ScreenText,
    )
    val ScreenSmall = Screen.copy(fontSize = 11.sp, lineHeight = 14.sp)
    val ScreenLarge = Screen.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
}

private val colors = darkColorScheme(
    primary = DexYellow,
    onPrimary = Ink,
    secondary = ScreenGlow,
    onSecondary = Ink,
    background = DexRed,
    onBackground = Color.White,
    surface = Screen,
    onSurface = ScreenText,
    surfaceVariant = ScreenDeep,
    onSurfaceVariant = ScreenTextDim,
    surfaceContainer = ScreenDeep,
    error = LedRed,
)

@Composable
fun DexTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
