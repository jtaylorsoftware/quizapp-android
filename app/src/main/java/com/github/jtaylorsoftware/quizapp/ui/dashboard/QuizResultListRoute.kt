package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.rememberIsRefreshing

/**
 * Controls rendering for the quiz result list screen and displays bottom navigation components
 * for the dashboard screens.
 * When the user is not logged in, this redirects to the login screen.
 *
 * @param viewModel The [QuizResultListViewModel] required for the screens.
 *
 * @param navigateToDetailScreen Function called when user taps the to see details for an individual
 * result. Called with the id of the quiz result to view details of.
 *
 * @param bottomNavigation The bottom navigation bar for the app.
 */
@Composable
fun QuizResultListRoute(
    viewModel: QuizResultListViewModel,
    navigateToDetailScreen: (ObjectId, ObjectId) -> Unit,
    bottomNavigation: @Composable () -> Unit,
) {
    val isRefreshing = rememberIsRefreshing(viewModel)
    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    QuizResultListRoute(
        uiState = viewModel.uiState,
        navigateToDetailScreen = navigateToDetailScreen,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        bottomNavigation = bottomNavigation
    )
}

/**
 * Renders the correct screen based on [uiState].
 */
@Composable
fun QuizResultListRoute(
    uiState: QuizResultListUiState,
    navigateToDetailScreen: (ObjectId, ObjectId) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    bottomNavigation: @Composable () -> Unit = {},
    scaffoldState: ScaffoldState = rememberScaffoldState(),
) {
    AppScaffold(
        modifier = Modifier.testTag("QuizResultListRoute"),
        scaffoldState = scaffoldState,
        uiState = uiState,
        bottomBar = { bottomNavigation() }
    ) {
        AppSwipeRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            when (uiState) {
                is QuizResultListUiState.NoQuizResults -> {
                    NoQuizResultsScreen(uiState = uiState)
                }
                is QuizResultListUiState.ListForUser -> {
                    QuizResultListScreen(
                        uiState = uiState,
                        navigateToDetails = navigateToDetailScreen,
                    )
                }
                is QuizResultListUiState.ListForQuiz -> {
                    QuizResultListScreen(
                        uiState = uiState,
                        navigateToDetails = navigateToDetailScreen,
                    )
                }
            }
        }
    }
}

/**
 * Screen displayed when there are no quiz results. Allows refresh attempts when the previous initial loads failed.
 */
@Composable
private fun NoQuizResultsScreen(
    uiState: QuizResultListUiState.NoQuizResults,
) {
    when (uiState.loading) {
        is LoadingState.Error -> {
            ErrorScreen {
                Text("Unable to load quiz results right now: ${uiState.loading.message.value}")
            }
        }
        is LoadingState.NotStarted, LoadingState.InProgress -> {
            LoadingScreen {
                Text("Loading quiz results")
            }
        }
        is LoadingState.Success -> {
            NoContentScreen {
                Text(
                    "There are no quiz results yet.",
                    Modifier.padding(vertical = 32.dp),
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}