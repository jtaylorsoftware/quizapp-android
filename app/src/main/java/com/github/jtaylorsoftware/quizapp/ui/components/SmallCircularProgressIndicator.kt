package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Displays a [CircularProgressIndicator] at a small size appropriate for use in
 * buttons or parts of the UI with default text size.
 */
@Composable
fun SmallCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onPrimary
) {
    CircularProgressIndicator(
        modifier.size(20.dp),
        color = color,
        strokeWidth = 2.dp
    )
}