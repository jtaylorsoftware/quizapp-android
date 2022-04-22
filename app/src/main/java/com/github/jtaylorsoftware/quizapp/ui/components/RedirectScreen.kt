package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay

/**
 * Displays a [LoadingScreen] before redirecting by calling [navigate].
 *
 * @param navigate Function that will perform the navigation when invoked.
 *
 * @param content Content to pass to [LoadingScreen].
 */
@Composable
fun Redirect(
    delayMillis: Long = REDIRECT_DELAY_MILLIS,
    navigate: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentNavigate by rememberUpdatedState(navigate)

    LaunchedEffect(Unit) {
        delay(delayMillis)
        currentNavigate()
    }

    LoadingScreen {
        content()
    }
}

private const val REDIRECT_DELAY_MILLIS = 1000L