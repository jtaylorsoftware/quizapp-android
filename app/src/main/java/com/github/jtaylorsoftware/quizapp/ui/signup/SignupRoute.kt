package com.github.jtaylorsoftware.quizapp.ui.signup

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import com.github.jtaylorsoftware.quizapp.ui.components.AppScaffold

/**
 * Controls rendering for the sign-up screen.
 * When the user is already signed in, this redirects the user to the profile screen.
 *
 * @param navigateToLogin Callback invoked when the user should be redirected to the login screen.
 */
@Composable
fun SignupRoute(
    viewModel: SignupViewModel,
    navigateToLogin: () -> Unit,
) {
    SignupRoute(
        uiState = viewModel.uiState,
        onUsernameChanged = viewModel::setUsername,
        onEmailChanged = viewModel::setEmail,
        onPasswordChanged = viewModel::setPassword,
        register = viewModel::register,
        navigateToLogin = navigateToLogin
    )
}

@Composable
fun SignupRoute(
    uiState: SignupUiState,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    register: () -> Unit,
    navigateToLogin: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    AppScaffold(scaffoldState = scaffoldState, uiState = uiState) {
        SignupScreen(
            uiState = uiState,
            onUsernameChanged = onUsernameChanged,
            onPasswordChanged = onPasswordChanged,
            onEmailChanged = onEmailChanged,
            navigateToLogin = navigateToLogin,
            register = register
        )
    }
}