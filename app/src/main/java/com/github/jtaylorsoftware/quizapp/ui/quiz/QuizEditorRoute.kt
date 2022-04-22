package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
) {
    QuizEditorRoute(
        uiState = viewModel.uiState,
        onSubmit = viewModel::uploadQuiz,
        onUploaded = onUploaded,
    )
}

@Composable
fun QuizEditorRoute(
    uiState: QuizEditorUiState,
    onSubmit: () -> Unit,
    onUploaded: () -> Unit,
) {
    when (uiState) {
        is QuizEditorUiState.NoQuiz -> {
            NoQuizScreen(uiState)
        }
        is QuizEditorUiState.Editor -> {
            if (uiState.uploadStatus is LoadingState.Success) {
                Redirect(navigate = onUploaded) {
                    Text("Quiz created, returning to profile")
                }
            } else {
                QuizEditorScreen(
                    uiState = uiState,
                    onSubmit = onSubmit
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
                Text("Loading quiz")
            }
        }
        else -> {
            val errorMessage = remember(uiState.loading) {
                (uiState.loading as? LoadingState.Error)?.let {
                    ": ${it.message.value}"
                } ?: "Unable to load quiz right now"
            }
            ErrorScreen {
                Text(errorMessage)
            }
        }
    }
}