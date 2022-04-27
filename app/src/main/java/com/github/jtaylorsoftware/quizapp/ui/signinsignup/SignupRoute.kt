package com.github.jtaylorsoftware.quizapp.ui.signinsignup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    scaffoldState: ScaffoldState,
) {
    SignupRoute(
        uiState = viewModel.uiState,
        onUsernameChanged = viewModel::setUsername,
        onEmailChanged = viewModel::setEmail,
        onPasswordChanged = viewModel::setPassword,
        register = viewModel::register,
        navigateToLogin = navigateToLogin,
        scaffoldState = scaffoldState,
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
    AppScaffold(scaffoldState = scaffoldState) { paddingValues ->
        Row(
            Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
}