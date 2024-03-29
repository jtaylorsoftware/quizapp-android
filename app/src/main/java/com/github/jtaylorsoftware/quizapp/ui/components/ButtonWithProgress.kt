package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * Displays a [Button] that shows the [progressIndicator] when [isInProgress] is true, to indicate that
 * the action for the button press is being completed. [content] should usually have its alpha
 * set to `0f` when [isInProgress] is true, to both hide it and keep the size of the [Button] the same as
 * when it is displayed.
 */
@Composable
fun ButtonWithProgress(
    onClick: () -> Unit,
    isInProgress: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    progressIndicator: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Button(onClick = onClick, modifier = modifier, shape = shape) {
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