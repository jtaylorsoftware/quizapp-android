package com.github.jtaylorsoftware.quizapp.ui.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Wraps the [SignupScreen] and forwards the [SignupViewModel] properties and methods as
 * arguments to the [SignupScreen]. To be used as the top-level component rendered in
 * the navigation graph.
 */
@Composable
fun SignupRoute(signupViewModel: SignupViewModel) {
//    val uiState by signupViewModel.uiState.collectAsState()
//
//    SignupScreen(
//        usernameState = uiState.usernameState,
//        onUsernameChanged = signupViewModel::setUsername,
//        passwordState = uiState.passwordState,
//        onPasswordChanged = signupViewModel::setPassword,
//        emailState = uiState.emailState,
//        onEmailChanged = signupViewModel::setEmail,
//        navigateToLogin = {},
//        register = { signupViewModel.register() }
//    )
}