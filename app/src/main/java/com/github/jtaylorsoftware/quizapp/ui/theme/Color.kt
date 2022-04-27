package com.github.jtaylorsoftware.quizapp.ui.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

// "Sea blue" / "Dark cyan"
val SeaBlue200 = Color(0xFF64BDE1)
val SeaBlue500 = Color(0xFF288DAF)
val SeaBlue700 = Color(0xFF00607F)

// Only used for surfaces and background
val GrayBlue50 = Color(0xFFEBF4FA)
val GrayBlue900 = Color(0xFF031B29)

// "Sea green" / "Bright cyan" / "Lime green"
val SeaGreen200 = Color(0xFF7EFFE8)
val SeaGreen500 = Color(0xFF3FE8B6)
val SeaGreen700 = Color(0xFF00B586)

val Colors.bottomAppBar: Color
    get() = if (isLight) {
        SeaBlue500
    } else {
        SeaBlue700
    }

val Colors.onBottomAppBar: Color
    get() = if (isLight) {
        Color.Black
    } else {
        Color.White
    }

val Colors.topAppBar: Color
    get() = background

val Colors.onTopAppBar: Color
    get() = onBackground

val Colors.correct: Color
    get() = SeaGreen500

val Colors.success: Color
    get() = SeaGreen500