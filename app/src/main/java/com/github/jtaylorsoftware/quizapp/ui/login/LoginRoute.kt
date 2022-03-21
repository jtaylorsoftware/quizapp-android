package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.compose.runtime.Composable

/**
 * Wraps the [LoginScreen] and forwards the [LoginViewModel] properties and methods as
 * arguments to the [LoginScreen]. To be used as the top-level component rendered in
 * the navigation graph.
 */
@Composable
fun LoginRoute(viewModel: LoginViewModel) {
    LoginScreen(
        usernameState = viewModel.usernameState,
        onUsernameChanged = viewModel::setUsername,
        passwordState = viewModel.passwordState,
        onPasswordChanged = viewModel::setPassword,
        login = { viewModel.login {} },
        navigateToRegister = {}
    )
}