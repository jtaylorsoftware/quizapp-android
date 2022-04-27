package com.github.jtaylorsoftware.quizapp.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Represents opinionated breakpoints for responsive UI.
 */
enum class WindowSizeClass { Compact, Medium, Expanded }

/**
 * Calculates a [WindowSizeClass] based on the width given to [activity].
 */
@Composable
fun calculateWindowSizeClass(activity: Activity): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return remember(activity) {
        val widthDp = configuration.screenWidthDp
        val heightDp = configuration.screenHeightDp
        when {
            widthDp < 600 || heightDp < 480 -> WindowSizeClass.Compact
            widthDp < 840 || heightDp < 900 -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        }
    }
}