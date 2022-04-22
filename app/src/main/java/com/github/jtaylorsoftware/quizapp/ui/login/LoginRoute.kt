package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.jtaylorsoftware.quizapp.ui.components.AppScaffold

/**
 * Controls rendering for the login screen.
 * When the user is already logged in, this redirects the user to the profile screen.
 *
 * @param onBackPressed Callback to invoke after user presses the back button on this screen.
 *
 * @param navigateToSignUp Callback invoked when the user should be redirected to the sign-up screen.
 */
@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onBackPressed: () -> Unit,
    navigateToSignUp: () -> Unit,
) {
    LoginRoute(
        uiState = viewModel.uiState,
        onUsernameChanged = viewModel::setUsername,
        onPasswordChanged = viewModel::setPassword,
        login = viewModel::login,
        navigateToSignUp = navigateToSignUp,
    )

    BackHandler {
        onBackPressed()
    }
}

@Composable
fun LoginRoute(
    uiState: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    login: () -> Unit,
    navigateToSignUp: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    AppScaffold(
        modifier = Modifier.testTag("LoginRoute"),
        scaffoldState = scaffoldState,
        uiState = uiState
    ) {
        LoginScreen(
            uiState = uiState,
            onUsernameChanged = onUsernameChanged,
            onPasswordChanged = onPasswordChanged,
            login = login,
            navigateToSignup = navigateToSignUp
        )
    }
}