package com.github.jtaylorsoftware.quizapp.ui.signinsignup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
fun SignInRoute(
    viewModel: SignInViewModel,
    onBackPressed: () -> Unit,
    navigateToSignUp: () -> Unit,
    scaffoldState: ScaffoldState,
) {
    SignInRoute(
        uiState = viewModel.uiState,
        onUsernameChanged = viewModel::setUsername,
        onPasswordChanged = viewModel::setPassword,
        login = viewModel::login,
        navigateToSignUp = navigateToSignUp,
        scaffoldState = scaffoldState,
    )

    BackHandler(onBack = onBackPressed)
}

@Composable
fun SignInRoute(
    uiState: SignInUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    login: () -> Unit,
    navigateToSignUp: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    AppScaffold(
        modifier = Modifier.testTag("LoginRoute"),
        scaffoldState = scaffoldState,
    ) { paddingValues ->
        Row(
            Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignInScreen(
                uiState = uiState,
                onUsernameChanged = onUsernameChanged,
                onPasswordChanged = onPasswordChanged,
                login = login,
                navigateToSignup = navigateToSignUp
            )
        }
    }
}