package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wraps a composable in a [Column] that fills its parent and adds
 * some content indicative of a "no content" screen.
 *
 * The static content is separated from [content] by a [Spacer], whose height
 * can be changed with [spacerHeight].
 *
 * Text content should have a style at most as hierarchically important as `MaterialTheme.typography.h6`.
 */
@Composable
fun NoContentScreen(
    spacerHeight: Dp = 64.dp,
    content: @Composable () -> Unit,
) {
    NoContentScreenContent(spacerHeight = spacerHeight, content = content)
}

/**
 * Overload of [NoContentScreen] that uses swipe-to-refresh behavior.
 */
@Composable
fun NoContentScreen(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    spacerHeight: Dp = 64.dp,
    content: @Composable () -> Unit,
) {
    AppSwipeRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        NoContentScreenContent(spacerHeight = spacerHeight, content = content)
    }
}

@Composable
private fun NoContentScreenContent(
    spacerHeight: Dp = 64.dp,
    content: @Composable () -> Unit,
) {
    Surface {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                Modifier
                    .height(spacerHeight)
                    .fillMaxWidth()
            )
            content()
        }
    }
}