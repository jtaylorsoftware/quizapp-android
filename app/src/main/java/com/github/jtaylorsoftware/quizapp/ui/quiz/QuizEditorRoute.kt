package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.ErrorScreen
import com.github.jtaylorsoftware.quizapp.ui.components.LoadingScreen
import com.github.jtaylorsoftware.quizapp.ui.components.Redirect

/**
 * Controls rendering for the [QuizEditorScreen]. When the user expects to edit
 * a quiz, but there isn't one found, it renders  a "no content" error screen.
 *
 * @param onUploaded Called after the quiz is successfully uploaded.
 */
@Composable
fun QuizEditorRoute(
    viewModel: QuizEditorViewModel,
    onUploaded: () -> Unit,
    scaffoldState: ScaffoldState,
    maxWidthDp: Dp,
) {
    QuizEditorRoute(
        uiState = viewModel.uiState,
        onSubmit = viewModel::uploadQuiz,
        onUploaded = onUploaded,
        scaffoldState = scaffoldState,
        maxWidthDp = maxWidthDp,
    )
}

@Composable
fun QuizEditorRoute(
    uiState: QuizEditorUiState,
    onSubmit: () -> Unit,
    onUploaded: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,) {
    when (uiState) {
        is QuizEditorUiState.NoQuiz -> {
            NoQuizScreen(uiState)
        }
        is QuizEditorUiState.Editor -> {
            if (uiState.uploadStatus is LoadingState.Success) {
                Redirect(navigate = onUploaded) {
                    Text(
                        "Quiz uploaded, returning to profile",
                        color = MaterialTheme.colors.onBackground
                    )
                }
            } else {
                QuizEditorScreen(
                    uiState = uiState,
                    onSubmit = onSubmit,
                    scaffoldState = scaffoldState,
                    maxWidthDp = maxWidthDp
                )
            }
        }
    }
}

@Composable
private fun NoQuizScreen(
    uiState: QuizEditorUiState.NoQuiz,
) {
    when (uiState.loading) {
        is LoadingState.NotStarted, LoadingState.InProgress -> {
            LoadingScreen {
                Text("Loading quiz", color = MaterialTheme.colors.onBackground)
            }
        }
        else -> {
            val errorMessage = remember(uiState.loading) {
                (uiState.loading as? LoadingState.Error)?.let {
                    ": ${it.message.value}"
                } ?: "Unable to load quiz right now"
            }
            ErrorScreen {
                Text(errorMessage, color = MaterialTheme.colors.onBackground)
            }
        }
    }
}