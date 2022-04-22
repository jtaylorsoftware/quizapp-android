package com.github.jtaylorsoftware.quizapp.ui.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.isInProgress

/**
 * Presents a screen for the user to input their credentials for a new account.
 *
 * @param usernameState State containing the input username and state.
 * @param onUsernameChanged Callback invoked when the user inputs text to the "Username" field.
 * @param passwordState State containing the input password and state.
 * @param onPasswordChanged Callback invoked when the user inputs text to the "Password" field.
 * @param register Callback invoked when the user presses the "Sign Up" button. Primary action on the screen.
 * @param navigateToLogin Callback invoked when the user presses the "Sign In" text. Secondary action on the screen.
 */
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
    val signUpButtonTextAlpha by derivedStateOf { if (isInProgress) 0.0f else 1.0f }

    var confirmPasswordState by remember { mutableStateOf(TextFieldState()) }
    val onConfirmPasswordChanged = { confirmPassword: String ->
        val error = if (confirmPassword != uiState.passwordState.text) {
            "Passwords do not match."
        } else null
        confirmPasswordState = TextFieldState(text = confirmPassword, error = error, dirty = true)
    }

    Column {
        UsernameField(state = uiState.usernameState, onValueChange = onUsernameChanged, hint = "Username")
        EmailField(state = uiState.emailState, onValueChange = onEmailChanged, hint = "Email")
        PasswordField(state = uiState.passwordState, onValueChange = onPasswordChanged, hint = "Password")
        PasswordField(
            state = confirmPasswordState,
            onValueChange = onConfirmPasswordChanged,
            fieldContentDescription = "Confirm password",
            hint = "Confirm password hint",
            hintContentDescription = "Confirm password hint",
        )

        ButtonWithProgress(
            onClick = register,
            isInProgress = isInProgress,
            progressIndicator = {
                SmallCircularProgressIndicator(
                    Modifier.semantics { contentDescription = "Sign-up in progress" }
                )
            }
        ) {
            Text("Sign Up", modifier = Modifier.alpha(signUpButtonTextAlpha))
        }
        Row {
            Text("Already registered?")
            Text(
                "Sign In",
                color = MaterialTheme.colors.secondary,
                modifier = Modifier
                    .clickable { navigateToLogin() }
                    .padding(horizontal = 2.dp)
            )
        }
    }
}