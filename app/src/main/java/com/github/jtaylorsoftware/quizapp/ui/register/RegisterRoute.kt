package com.github.jtaylorsoftware.quizapp.ui.register

import androidx.compose.runtime.Composable

/**
 * Wraps the [RegisterScreen] and forwards the [RegisterViewModel] properties and methods as
 * arguments to the [RegisterScreen]. To be used as the top-level component rendered in
 * the navigation graph.
 */
@Composable
fun RegisterRoute(registerViewModel: RegisterViewModel) {
    RegisterScreen(
        usernameState = registerViewModel.usernameState,
        onUsernameChanged = registerViewModel::setUsername,
        passwordState = registerViewModel.passwordState,
        onPasswordChanged = registerViewModel::setPassword,
        navigateToLogin = {},
        register = { registerViewModel.register {}}
    )
}