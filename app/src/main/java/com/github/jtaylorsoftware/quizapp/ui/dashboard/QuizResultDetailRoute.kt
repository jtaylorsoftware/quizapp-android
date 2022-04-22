package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.rememberIsRefreshing

@Composable
fun QuizResultDetailRoute(
    viewModel: QuizResultDetailViewModel
) {
    val isRefreshing = rememberIsRefreshing(viewModel)

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    QuizResultDetailRoute(
        uiState = viewModel.uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh
    )
}

@Composable
fun QuizResultDetailRoute(
    uiState: QuizResultDetailUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
) {
    AppScaffold(
        scaffoldState = scaffoldState,
        uiState = uiState,
    ) {
        AppSwipeRefresh(isRefreshing = isRefreshing, onRefresh = onRefresh) {
            when (uiState) {
                is QuizResultDetailUiState.NoQuizResult -> {
                    NoQuizResultScreen(uiState)
                }
                is QuizResultDetailUiState.QuizResultDetail -> {
                    ResultDetail(
                        uiState.quizResult,
                        uiState.quizForm
                    )
                }
            }
        }
    }
}

@Composable
private fun NoQuizResultScreen(
    uiState: QuizResultDetailUiState.NoQuizResult
) {
    when (uiState.loading) {
        is LoadingState.Error -> {
            ErrorScreen {
                Text("Unable to load quiz result right now: ${uiState.loading.message.value}")
            }
        }
        is LoadingState.NotStarted, LoadingState.InProgress -> {
            LoadingScreen {
                Text("Loading quiz result")
            }
        }
        is LoadingState.Success -> {
            NoContentScreen {
                Text(
                    "No quiz result found.",
                    Modifier.padding(vertical = 32.dp),
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}