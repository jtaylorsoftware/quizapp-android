package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Wraps a composable in a scrollable [Column] that fills its parent and adds
 * some content indicative of an error screen.
 *
 * The error content is separated from [content] by a [Spacer], whose height
 * can be changed with [spacerHeight].
 *
 * Text content should have a style at most as hierarchically important as `MaterialTheme.typography.h6`.
 */
@Composable
fun ErrorScreen(
    spacerHeight: Dp = 64.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ErrorContent(spacerHeight = spacerHeight, content = content)
}

/**
 * Overload of [ErrorScreen] that uses swipe-to-refresh behavior.
 */
@Composable
fun ErrorScreen(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    spacerHeight: Dp = 64.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    AppSwipeRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        ErrorContent(spacerHeight = spacerHeight) {
            content()
        }
    }
}

@Composable
private fun ErrorContent(spacerHeight: Dp, content: @Composable (ColumnScope.() -> Unit)) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 64.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Something went wrong...",
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


@Preview
@Composable
private fun ErrorScreenWithRefreshPreview() {
    var refreshing by remember { mutableStateOf(false) }
    val onRefresh = suspend {
        refreshing = true
        delay(1500)
        refreshing = false
    }
    val scope = rememberCoroutineScope()
    QuizAppTheme {
        Surface(color = MaterialTheme.colors.background) {
            ErrorScreen(refreshing, { scope.launch { onRefresh() } }) {
                Text("You can't access this content.")
            }
        }
    }
}