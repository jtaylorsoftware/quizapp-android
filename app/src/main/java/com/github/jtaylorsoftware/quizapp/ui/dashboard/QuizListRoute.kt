package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.OnSuccess
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.rememberIsRefreshing

/**
 * Controls rendering for the [QuizListScreen] and displays a bottom navigation bar common
 * to the profile/dashboard screens.
 *
 * @param viewModel The [QuizListViewModel] required for the screens.
 *
 * @param navigateToEditor Called when the user taps the "Edit" button for a Quiz, or taps the
 * FAB to create a Quiz. Receives the id of the Quiz as its argument, or null when tapping the FAB.
 *
 * @param navigateToResultsForQuiz Called when the user taps the "Results" button for a Quiz.
 * Receives the id of the Quiz as its argument.
 *
 * @param bottomNavigation The bottom navigation bar for the app.
 */
@Composable
fun QuizListRoute(
    viewModel: QuizListViewModel,
    navigateToEditor: (ObjectId?) -> Unit,
    navigateToResultsForQuiz: (ObjectId) -> Unit,
    bottomNavigation: @Composable () -> Unit,
    scaffoldState: ScaffoldState,
    maxWidthDp: Dp,
) {
    val isRefreshing = rememberIsRefreshing(viewModel)

    QuizListRoute(
        uiState = viewModel.uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onDeleteQuiz = viewModel::deleteQuiz,
        navigateToEditor = navigateToEditor,
        navigateToResults = navigateToResultsForQuiz,
        bottomNavigation = bottomNavigation,
        scaffoldState = scaffoldState,
        maxWidthDp = maxWidthDp,
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
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    AppScaffold(
        modifier = Modifier.testTag("QuizListRoute"),
        scaffoldState = scaffoldState,
        floatingActionButton = {
            FloatingActionButton(onClick = { navigateToEditor(null) }) {
                Icon(Icons.Default.Add, "Create quiz")
            }
        },
        bottomBar = { bottomNavigation() }
    ) { paddingValues ->
        AppSwipeRefresh(isRefreshing = isRefreshing, onRefresh = onRefresh) {
            Box(Modifier.padding(paddingValues)) {
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
                            scaffoldState = scaffoldState,
                            maxWidthDp = maxWidthDp,
                        )
                    }
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