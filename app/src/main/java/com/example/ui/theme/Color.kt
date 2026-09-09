package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Meskot Brand Palette (Ethiopian Heritage & Earth Tones)
val Ink = Color(0xFF1B2A22)
val InkDark = Color(0xFF121D17)
val Paper = Color(0xFFEEF2ED)
val Paper2 = Color(0xFFE3E9DF)
val CardBg = Color(0xFFFBFCF9)
val Gold = Color(0xFFB8863A)
val GoldDeep = Color(0xFF8F6524)
val GoldLight = Color(0xFFE8B94F)
val CrossRed = Color(0xFF8C2F39)
val LineBorder = Color(0xFFD8DECF)
val MutedText = Color(0xFF5C6B5F)
val ActiveGreen = Color(0xFF31A24C)
val GreenAccent = Color(0xFF31A24C)

// Post Background Gradients
val PostGradient1 = Brush.linearGradient(listOf(Color(0xFF8C2F39), Color(0xFFB8863A)))
val PostGradient2 = Brush.linearGradient(listOf(Color(0xFF1B2A22), Color(0xFF2A4838)))
val PostGradient3 = Brush.linearGradient(listOf(Color(0xFF335577), Color(0xFF5A8FBE)))
val PostGradient4 = Brush.linearGradient(listOf(Color(0xFF4A3B5C), Color(0xFF8C5CA8)))
val PostGradient5 = Brush.linearGradient(listOf(Color(0xFFB8863A), Color(0xFFE8B94F)))

val PostGradientList = listOf(
    null, // Standard card
    PostGradient1,
    PostGradient2,
    PostGradient3,
    PostGradient4,
    PostGradient5
)
