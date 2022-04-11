package com.github.jtaylorsoftware.quizapp.ui.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.jtaylorsoftware.quizapp.ui.components.EmailField
import com.github.jtaylorsoftware.quizapp.ui.components.PasswordField
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.components.UsernameField

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
    usernameState: TextFieldState,
    onUsernameChanged: (String) -> Unit,
    passwordState: TextFieldState,
    onPasswordChanged: (String) -> Unit,
    emailState: TextFieldState,
    onEmailChanged: (String) -> Unit,
    register: () -> Unit,
    navigateToLogin: () -> Unit,
) {
    var confirmPasswordState by remember { mutableStateOf(TextFieldState()) }
    val onConfirmPasswordChanged = { confirmPassword: String ->
        val error = if (confirmPassword != passwordState.text) {
            "Passwords do not match."
        } else null
        confirmPasswordState = TextFieldState(text = confirmPassword, error = error, dirty = true)
    }

    Column {
        UsernameField(state = usernameState, onValueChange = onUsernameChanged, hint = "Username")
        EmailField(state = emailState, onValueChange = onEmailChanged, hint = "Email")
        PasswordField(state = passwordState, onValueChange = onPasswordChanged, hint = "Password")
        PasswordField(
            state = confirmPasswordState,
            onValueChange = onConfirmPasswordChanged,
            fieldContentDescription = "Confirm password",
            hint = "Confirm password hint",
            hintContentDescription = "Confirm password hint",
        )
        Button(onClick = register) {
            Text("Sign Up")
        }
        Row {
            Text("Already registered?")
            Text("Sign In", modifier = Modifier.clickable { navigateToLogin() })
        }
    }
}