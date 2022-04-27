package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

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
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 64.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "There's nothing here...",
            style = MaterialTheme.typography.h5
        )

        Spacer(
            Modifier
                .height(spacerHeight)
                .fillMaxWidth()
        )
        content()
    }
}

@Preview(widthDp = 600, heightDp = 1000)
@Composable
private fun NoContentScreenPreview() {
    QuizAppTheme {
        Surface(color = MaterialTheme.colors.background) {
            NoContentScreen {
                Text("You have no quizzes", style = MaterialTheme.typography.h6)
            }
        }
    }
}