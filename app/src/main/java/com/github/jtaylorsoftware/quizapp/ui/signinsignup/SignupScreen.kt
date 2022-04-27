package com.github.jtaylorsoftware.quizapp.ui.signinsignup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.isInProgress

/**
 * Presents a screen for the user to input their credentials for a new account.
 *
 * @param uiState The state required for this screen.
 * @param onUsernameChanged Callback invoked when the user inputs text to the "Username" field.
 * @param onPasswordChanged Callback invoked when the user inputs text to the "Password" field.
 * @param register Callback invoked when the user presses the "Sign Up" button. Primary action on the screen.
 * @param navigateToLogin Callback invoked when the user presses the "Sign In" text. Secondary action on the screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignupScreen(
    uiState: SignupUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    register: () -> Unit,
    navigateToLogin: () -> Unit,
) {
    val isInProgress = remember(uiState) { uiState.registerStatus.isInProgress }

    var confirmPasswordState by rememberSaveable(stateSaver = TextFieldState.Saver) {
        mutableStateOf(
            TextFieldState()
        )
    }
    val onConfirmPasswordChanged = { confirmPassword: String ->
        val error = if (confirmPassword != uiState.passwordState.text) {
            "Passwords do not match."
        } else null
        confirmPasswordState = TextFieldState(text = confirmPassword, error = error, dirty = true)
    }

    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        Modifier
            .verticalScroll(scrollState)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(top = 12.dp, bottom = 12.dp, start = 32.dp, end = 32.dp)
                .width(IntrinsicSize.Min),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogoLarge()

            UsernameField(
                state = uiState.usernameState,
                onTextChange = onUsernameChanged,
                modifier = Modifier.requiredWidth(300.dp),
                imeAction = ImeAction.Next
            )
            EmailField(
                state = uiState.emailState,
                onTextChange = onEmailChanged,
                modifier = Modifier.requiredWidth(300.dp),
                imeAction = ImeAction.Next,
            )
            PasswordField(
                state = uiState.passwordState,
                onTextChange = onPasswordChanged,
                modifier = Modifier.requiredWidth(300.dp),
                imeAction = ImeAction.Next,
            )
            ConfirmPasswordField(
                state = confirmPasswordState,
                onTextChange = onConfirmPasswordChanged,
                modifier = Modifier.requiredWidth(300.dp),
                imeAction = ImeAction.Done,
                onImeAction = {
                    keyboardController?.hide()
                }
            )
            SignInSignUpButton(
                text = "Sign Up",
                onClick = register,
                isInProgress = isInProgress,
                contentDescription = "Sign-up in progress"
            )
            Row {
                TextButton(navigateToLogin) {
                    Text("Sign In", color = MaterialTheme.colors.secondaryVariant)
                }
            }
        }
    }
}