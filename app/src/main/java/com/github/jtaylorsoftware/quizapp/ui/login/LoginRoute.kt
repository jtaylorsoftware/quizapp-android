package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import com.github.jtaylorsoftware.quizapp.ui.LoadingState

/**
 * Controls top-level rendering for the sign-up navigation route, including the display
 * of SnackBars when appropriate. When the user
 * is not already signed in, this displays the [LoginScreen].
 *
 * @param navigateToProfile Callback invoked after successful login.
 * @param navigateToSignUp Callback invoked when the user should be redirected to the sign-up screen.
 */
@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    navigateToProfile: () -> Unit,
    navigateToSignUp: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    val uiState by viewModel.uiState.collectAsState()

    when(uiState) {
        is LoginUiState.SignedIn -> navigateToProfile()
        is LoginUiState.Form -> (uiState as LoginUiState.Form).let {
            ScaffoldedLoginScreen(
                uiState = it,
                viewModel = viewModel,
                navigateToSignUp = navigateToSignUp,
                scaffoldState = scaffoldState
            )
        }
    }
}

/**
 * Wraps the [LoginScreen] in a Scaffold that will show a SnackBar depending
 * on the current error.
 */
@Composable
private fun ScaffoldedLoginScreen(
    uiState: LoginUiState.Form,
    viewModel: LoginViewModel,
    navigateToSignUp: () -> Unit,
    scaffoldState: ScaffoldState
) {
    if (uiState.loading is LoadingState.Error){
        LaunchedEffect(scaffoldState.snackbarHostState) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = uiState.loading.message
            )
        }
    }

    Scaffold(
        scaffoldState = scaffoldState
    ) {
        LoginScreen(
            usernameState = uiState.usernameState,
            onUsernameChanged = viewModel::setUsername,
            passwordState = uiState.passwordState,
            onPasswordChanged = viewModel::setPassword,
            login = { viewModel.login() },
            navigateToSignup = navigateToSignUp
        )
    }
}