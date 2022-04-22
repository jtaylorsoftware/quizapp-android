package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.OnSuccess
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.rememberIsRefreshing

/**
 * Controls rendering for the quiz list screen and displays bottom navigation components
 * for the dashboard screens.
 * When the user is not logged in, this redirects to the login screen.
 *
 * @param viewModel The [QuizListViewModel] required for the screens.
 *
 * @param navigateToEditor Called when the user taps the "Edit" button for a Quiz, or taps the
 * FAB to create a Quiz. Receives the id of the Quiz as its argument, or null when tapping the FAB.
 *
 * @param navigateToResults Called when the user taps the "Results" button for a Quiz.
 * Receives the id of the Quiz as its argument.
 *
 * @param bottomNavigation The bottom navigation bar for the app.
 */
@Composable
fun QuizListRoute(
    viewModel: QuizListViewModel,
    navigateToEditor: (ObjectId?) -> Unit,
    navigateToResults: (ObjectId) -> Unit,
    bottomNavigation: @Composable () -> Unit,
) {
    val isRefreshing = rememberIsRefreshing(viewModel)

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    QuizListRoute(
        uiState = viewModel.uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onDeleteQuiz = viewModel::deleteQuiz,
        navigateToEditor = navigateToEditor,
        navigateToResults = navigateToResults,
        bottomNavigation = bottomNavigation
    )
}

/**
 * Renders the correct screen based on [uiState].
 *
 * @param onRefresh Called when the user swipes to refresh.
 *
 * @param onDeleteQuiz Called when the user taps the delete button on a quiz.
 */
@Composable
fun QuizListRoute(
    uiState: QuizListUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDeleteQuiz: (ObjectId) -> Unit,
    navigateToEditor: (ObjectId?) -> Unit,
    navigateToResults: (ObjectId) -> Unit,
    bottomNavigation: @Composable () -> Unit = {},
    scaffoldState: ScaffoldState = rememberScaffoldState(),
) {
    AppScaffold(
        modifier = Modifier.testTag("QuizListRoute"),
        scaffoldState = scaffoldState,
        uiState = uiState,
        floatingActionButton = {
            FloatingActionButton(onClick = { navigateToEditor(null) }) {
                Icon(Icons.Default.Add, "Create quiz")
            }
        },
        bottomBar = { bottomNavigation() }
    ) {
        AppSwipeRefresh(isRefreshing = isRefreshing, onRefresh = onRefresh) {
            when (uiState) {
                is QuizListUiState.NoQuizzes -> {
                    NoQuizzesScreen(uiState)
                }
                is QuizListUiState.QuizList -> {
                    OnSuccess(uiState.deleteQuizStatus) {
                        onRefresh()
                    }
                    QuizListScreen(
                        uiState = uiState,
                        onDeleteQuiz = onDeleteQuiz,
                        navigateToEditor = navigateToEditor,
                        navigateToResults = navigateToResults,
                    )
                }
            }
        }
    }
}

/**
 * Screen displayed when there are no quizzes. Allows refresh attempts when the previous initial loads failed.
 */
@Composable
private fun NoQuizzesScreen(
    uiState: QuizListUiState.NoQuizzes,
) {
    when (uiState.loading) {
        is LoadingState.Error -> {
            ErrorScreen {
                Text("Unable to load your quizzes right now: ${uiState.loading.message.value}")
            }
        }
        is LoadingState.NotStarted, LoadingState.InProgress -> {
            LoadingScreen {
                Text("Loading quizzes")
            }
        }
        is LoadingState.Success -> {
            NoContentScreen {
                Text(
                    "You have no quizzes",
                    Modifier.padding(vertical = 32.dp),
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}