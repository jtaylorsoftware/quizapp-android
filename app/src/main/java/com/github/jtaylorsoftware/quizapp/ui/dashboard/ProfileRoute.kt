package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.OnSuccess
import com.github.jtaylorsoftware.quizapp.ui.components.AppScaffold
import com.github.jtaylorsoftware.quizapp.ui.components.AppSwipeRefresh
import com.github.jtaylorsoftware.quizapp.ui.components.ErrorScreen
import com.github.jtaylorsoftware.quizapp.ui.components.LoadingScreen
import com.github.jtaylorsoftware.quizapp.ui.rememberIsRefreshing

/**
 * Controls rendering for the profile screen and displays bottom navigation components
 * for the dashboard screens.
 * When the user is not logged in, this redirects to the login screen.
 *
 * @param viewModel The [ProfileViewModel] required for the screens.
 *
 * @param navigateToQuizScreen Function called when user taps the "created quizzes" section.
 * Should perform the same action as tapping the bottom navigation icon.
 *
 * @param navigateToResultScreen Function called when user taps the "your results" section.
 * Should perform the same action as tapping the bottom navigation icon.
 *
 * @param bottomNavigation The bottom navigation bar for the app.
 */
@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    navigateToQuizScreen: () -> Unit,
    navigateToResultScreen: () -> Unit,
    bottomNavigation: @Composable () -> Unit
) {
    val isRefreshing = rememberIsRefreshing(viewModel)

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    ProfileRoute(
        uiState = viewModel.uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onChangeEmail = viewModel::setEmail,
        onChangePassword = viewModel::setPassword,
        onSubmitEmail = viewModel::submitEmail,
        onSubmitPassword = viewModel::submitPassword,
        navigateToQuizScreen = navigateToQuizScreen,
        navigateToResultScreen = navigateToResultScreen,
        openEditor = viewModel::openEditor,
        closeEditor = viewModel::closeEditor,
        onLogOut = viewModel::logOut,
        bottomNavigation = bottomNavigation
    )
}


/**
 * Renders the correct screen based on [uiState].
 *
 * @param openEditor Called when the user taps "edit profile."
 *
 * @param closeEditor Called when the user closes out of the profile editor.
 *
 * @param bottomNavigation The bottom navigation bar for the app.
 */
@Composable
fun ProfileRoute(
    uiState: ProfileUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onChangeEmail: (String) -> Unit,
    onChangePassword: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onSubmitPassword: () -> Unit,
    navigateToQuizScreen: () -> Unit,
    navigateToResultScreen: () -> Unit,
    openEditor: () -> Unit,
    closeEditor: () -> Unit,
    onLogOut: () -> Unit,
    bottomNavigation: @Composable () -> Unit = {},
    scaffoldState: ScaffoldState = rememberScaffoldState(),
) {
    AppScaffold(
        modifier = Modifier.testTag("ProfileRoute"),
        scaffoldState = scaffoldState,
        uiState = uiState,
        topBar = {
            if (uiState is ProfileUiState.Editor) {
                EditorTopBar(closeEditor)
            }
        },
        bottomBar = { bottomNavigation() }
    ) {
        AppSwipeRefresh(isRefreshing = isRefreshing, onRefresh = onRefresh) {
            when (uiState) {
                is ProfileUiState.NoProfile -> {
                    NoProfileScreen(uiState)
                }
                is ProfileUiState.Profile -> {
                    ProfileScreen(
                        uiState = uiState,
                        navigateToQuizScreen = navigateToQuizScreen,
                        navigateToResultScreen = navigateToResultScreen,
                        navigateToProfileEditor = openEditor,
                    )
                }
                is ProfileUiState.Editor -> {
                    OnSuccess(uiState.submitPasswordStatus, uiState.submitEmailStatus) {
                        onRefresh()
                    }
                    ProfileEditorScreen(
                        uiState = uiState,
                        onChangeEmail = onChangeEmail,
                        onChangePassword = onChangePassword,
                        onSubmitEmail = onSubmitEmail,
                        onSubmitPassword = onSubmitPassword,
                        onClose = closeEditor,
                        onLogOut = onLogOut,
                    )
                }
            }
        }
    }
}

/**
 * Screen displayed when there is no profile data. Allows refresh attempts when the previous initial loads failed.
 */
@Composable
private fun NoProfileScreen(
    uiState: ProfileUiState.NoProfile,
) {
    when (uiState.loading) {
        is LoadingState.NotStarted, LoadingState.InProgress -> {
            LoadingScreen {
                Text("Loading your profile")
            }
        }
        else -> {
            val errorMessage = remember(uiState.loading) {
                (uiState.loading as? LoadingState.Error)?.let {
                    ": ${it.message.value}"
                } ?: ""
            }
            ErrorScreen {
                Text("Unable load your profile right now$errorMessage")
            }
        }
    }
}
