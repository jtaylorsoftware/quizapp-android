package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * Wraps a composable in a [Column] that fills its parent and adds
 * some content indicative of a loading screen, including a [CircularProgressIndicator].
 *
 * The progress indicator is separated from [content] by a [Spacer], whose height
 * can be changed with [spacerHeight].
 *
 * Text content should have a style less hierarchically important than header
 * styles.
 */
@Composable
fun LoadingScreen(
    spacerHeight: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(Modifier.semantics {
                contentDescription = "Loading in progress"
            })
            Spacer(
                Modifier
                    .height(spacerHeight)
                    .fillMaxWidth()
            )
            content()
        }
    }
}


@Preview
@Composable
private fun LoadingScreenPreview() {
    QuizAppTheme {
        LoadingScreen {
            Text("Loading your content")
        }
    }
}