package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * A large banner that displays the app logo.
 */
@Composable
fun AppLogoLarge() {
    Text(
        "QuizNow",
        modifier = Modifier.padding(horizontal = 12.dp),
        style = MaterialTheme.typography.h3,
        color = MaterialTheme.colors.onBackground
    )
}

@Preview
@Composable
private fun AppLogoLargePreview() {
    QuizAppTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppLogoLarge()
            }
        }
    }
}