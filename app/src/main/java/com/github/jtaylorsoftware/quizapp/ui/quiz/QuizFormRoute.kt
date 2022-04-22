package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.ErrorScreen
import com.github.jtaylorsoftware.quizapp.ui.components.LoadingScreen
import com.github.jtaylorsoftware.quizapp.ui.components.Redirect

/**
 * Controls rendering for the [QuizFormScreen]. When the user expects to edit
 * a quiz, but there isn't one found, it renders  a "no content" error screen.
 *
 * @param onUploaded Called after the form is successfully uploaded.
 */
@Composable
fun QuizFormRoute(
    viewModel: QuizFormViewModel,
    onUploaded: () -> Unit,
) {
    QuizFormRoute(
        uiState = viewModel.uiState,
        onSubmit = viewModel::uploadResponses,
        onUploaded = onUploaded
    )
}

@Composable
fun QuizFormRoute(
    uiState: QuizFormUiState,
    onSubmit: () -> Unit,
    onUploaded: () -> Unit,
) {
    when(uiState) {
        is QuizFormUiState.NoQuiz -> {
            NoQuizScreen(uiState)
        }
        is QuizFormUiState.Form -> {
            if (uiState.uploadStatus is LoadingState.Success) {
                Redirect(navigate = onUploaded) {
                    Text("Responses submitted, returning to profile")
                }
            } else {
                QuizFormScreen(
                    quiz = uiState.quiz,
                    responses = uiState.responses,
                    onSubmit = onSubmit
                )
            }
        }
    }
}

@Composable
private fun NoQuizScreen(
    uiState: QuizFormUiState.NoQuiz
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
                } ?: "Unable to load quiz form right now"
            }
            ErrorScreen {
                Text(errorMessage)
            }
        }
    }
}