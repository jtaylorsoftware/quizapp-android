package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.WindowSizeClass
import com.github.jtaylorsoftware.quizapp.ui.components.AppScaffold
import com.github.jtaylorsoftware.quizapp.ui.components.AppSwipeRefresh
import com.github.jtaylorsoftware.quizapp.ui.components.ErrorScreen
import com.github.jtaylorsoftware.quizapp.ui.components.LoadingScreen
import com.github.jtaylorsoftware.quizapp.ui.rememberIsRefreshing

/**
 * Controls rendering of the [ProfileScreen] and displays a bottom navigation bar common
 * to the profile/dashboard screens.
 *
 * @param viewModel The [ProfileViewModel] required for the screens.
 *
 * @param navigateToQuizCreator Function called when user taps the "Create Quiz" button. Should
 * be the same action as tapping the FloatingActionButton on the QuizList screen.
 *
 * @param navigateToQuizResults Called when the user taps the "View results" button. Should
 * perform the same navigation as tapping the BottomNavigation icon.
 *
 * @param bottomNavigation The bottom navigation bar for the Profile screens.
 */
@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    navigateToQuizCreator: () -> Unit,
    navigateToQuizResults: () -> Unit,
    bottomNavigation: @Composable () -> Unit,
    scaffoldState: ScaffoldState,
    maxWidthDp: Dp,
    windowSizeClass: WindowSizeClass,
) {
    val isRefreshing = rememberIsRefreshing(viewModel)

    ProfileRoute(
        uiState = viewModel.uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onChangeEmail = viewModel::setEmail,
        onChangePassword = viewModel::setPassword,
        onSubmitEmail = viewModel::submitEmail,
        onSubmitPassword = viewModel::submitPassword,
        navigateToQuizCreator = navigateToQuizCreator,
        navigateToQuizResults = navigateToQuizResults,
        openSettings = viewModel::openSettings,
        closeSettings = viewModel::closeSettings,
        onLogOut = viewModel::logOut,
        bottomNavigation = bottomNavigation,
        scaffoldState = scaffoldState,
        maxWidthDp = maxWidthDp,
        windowSizeClass = windowSizeClass
    )
}


/**
 * Renders the correct screen based on [uiState].
 *
 * @param isRefreshing `true` when the screen data is being refreshed.
 *
 * @param onRefresh Called to refresh the screen data.
 *
 * @param onChangeEmail Called when the user types into the Settings email text field.
 *
 * @param onChangePassword Called when the user types into the Settings password text field.
 *
 * @param onSubmitEmail Called when submitting the email changes.
 *
 * @param onSubmitPassword Called when submitting the password changes.
 *
 * @param openSettings Called when the user taps "edit profile."
 *
 * @param closeSettings Called when the user closes out of the profile editor.
 *
 * @param onLogOut Called when the user taps the "log out" icon button.
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
    navigateToQuizCreator: () -> Unit,
    navigateToQuizResults: () -> Unit,
    openSettings: () -> Unit,
    closeSettings: () -> Unit,
    onLogOut: () -> Unit,
    bottomNavigation: @Composable () -> Unit = {},
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
    windowSizeClass: WindowSizeClass = WindowSizeClass.Compact,
) {
    val closeAndRefresh = {
        closeSettings()
        onRefresh()
    }

    AppScaffold(
        modifier = Modifier.testTag("ProfileRoute"),
        scaffoldState = scaffoldState,
        topBar = {
            if (uiState !is ProfileUiState.Profile || !uiState.settingsOpen) {
                ProfileTopBar(openSettings = openSettings, onLogOut = onLogOut)
            }
        },
        bottomBar = {
            if (uiState !is ProfileUiState.Profile || !uiState.settingsOpen) {
                bottomNavigation()
            }
        }
    ) { paddingValues ->
        AppSwipeRefresh(isRefreshing = isRefreshing, onRefresh = onRefresh) {
            Row(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.requiredWidthIn(max = maxWidthDp)) {
                    when (uiState) {
                        is ProfileUiState.NoProfile -> {
                            NoProfileScreen(uiState)
                        }
                        is ProfileUiState.Profile -> {
                            ProfileScreen(
                                uiState = uiState,
                                navigateToQuizCreator = navigateToQuizCreator,
                                navigateToQuizResults = navigateToQuizResults,
                                maxWidthDp = maxWidthDp
                            )

                            if (uiState.settingsOpen) {

                                ProfileSettingsDialog(
                                    uiState = uiState,
                                    onClose = closeAndRefresh,
                                    onChangePassword = onChangePassword,
                                    onChangeEmail = onChangeEmail,
                                    onSubmitPassword = onSubmitPassword,
                                    onSubmitEmail = onSubmitEmail,
                                    maxWidthDp = maxWidthDp,
                                    windowSizeClass = windowSizeClass
                                )

                                BackHandler(onBack = closeAndRefresh)
                            }
                        }
                    }
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