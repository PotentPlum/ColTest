package com.potentplum.dextracker.ui.theme

import androidx.compose.ui.graphics.Color

// Device shell
val DexRed = Color(0xFFC8202B)
val DexRedLight = Color(0xFFE0434C)
val DexRedDark = Color(0xFF8E1219)
val DexRedDeep = Color(0xFF5C0A10)

// Bezels and hardware
val Bezel = Color(0xFF26292E)
val BezelLight = Color(0xFF4A4F57)
val BezelSilver = Color(0xFFA9AEB5)
val BezelSilverDark = Color(0xFF6E737B)

// Screens
val Screen = Color(0xFF0F2C35)
val ScreenDeep = Color(0xFF091E25)
val ScreenLine = Color(0xFF1E4C59)
val ScreenGlow = Color(0xFF5ED8EE)
val ScreenText = Color(0xFFDDF6FB)
val ScreenTextDim = Color(0xFF8DB8C2)

// Accents
val DexYellow = Color(0xFFFFCB05)
val DexYellowDark = Color(0xFFB98F00)
val TabGray = Color(0xFFC3C6CB)
val MissingTile = Color(0xFF34373C)
val LensBlue = Color(0xFF1E88E5)
val LedRed = Color(0xFFFF4D4D)
val LedYellow = Color(0xFFFFD84D)
val LedGreen = Color(0xFF5BE36B)
val Ink = Color(0xFF15171A)

/** Official-ish type colours used on the type chips. */
fun typeColor(type: String): Color = when (type.lowercase()) {
    "normal" -> Color(0xFFA8A77A)
    "fire" -> Color(0xFFEE8130)
    "water" -> Color(0xFF6390F0)
    "electric" -> Color(0xFFF7D02C)
    "grass" -> Color(0xFF7AC74C)
    "ice" -> Color(0xFF96D9D6)
    "fighting" -> Color(0xFFC22E28)
    "poison" -> Color(0xFFA33EA1)
    "ground" -> Color(0xFFE2BF65)
    "flying" -> Color(0xFFA98FF3)
    "psychic" -> Color(0xFFF95587)
    "bug" -> Color(0xFFA6B91A)
    "rock" -> Color(0xFFB6A136)
    "ghost" -> Color(0xFF735797)
    "dragon" -> Color(0xFF6F35FC)
    "dark" -> Color(0xFF705746)
    "steel" -> Color(0xFFB7B7CE)
    "fairy" -> Color(0xFFD685AD)
    else -> BezelSilver
}
