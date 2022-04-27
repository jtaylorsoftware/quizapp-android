package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color


/**
 * Displays a [FloatingActionButton] that shows the [progressIndicator] when [isInProgress] is true, to indicate that
 * the action for the button press is being completed. [content] should usually have its alpha
 * set to `0f` when [isInProgress] is `true` so that [progressIndicator] is the only composable
 * visible.
 */
@Composable
fun FABWithProgress(
    onClick: () -> Unit,
    isInProgress: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    progressIndicator: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = contentColor
    ) {
        Box {
            if (isInProgress) {
                Box(Modifier.align(Alignment.Center)) {
                    progressIndicator()
                }
            }
            content()
        }
    }
}