package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.github.jtaylorsoftware.quizapp.ui.components.EmailField
import com.github.jtaylorsoftware.quizapp.ui.components.PasswordField
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState

@Composable
fun ProfileScreen(
    email: String,
    emailState: TextFieldState,
    onChangeEmail: (String) -> Unit,
    passwordState: TextFieldState,
    onChangePassword: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onSubmitPassword: () -> Unit,
) {
    Column {
        Text("Edit Profile")
        EmailForm(email, emailState, onChangeEmail, onSubmitEmail)
        PasswordForm(passwordState, onChangePassword, onSubmitPassword)
        Text("Account deletion available when signed into the web app.")
    }
}

@Composable
private fun EmailForm(
    email: String,
    emailState: TextFieldState,
    onChangeEmail: (String) -> Unit,
    onSubmitEmail: () -> Unit
) {
    var open: Boolean by remember { mutableStateOf(false) }

    // Swap between Text and TextField depending on if user has clicked "Change"
    if (open) {
        EmailField(state = emailState, onValueChange = onChangeEmail)

        Row {
            Button(onClick = { open = false }, modifier = Modifier.semantics {
                contentDescription = "Cancel email changes"
            }) {
                Text("Cancel")
            }
            Button(onClick = onSubmitEmail, modifier = Modifier.semantics {
                contentDescription = "Submit email changes"
            }) {
                Text("Submit")
            }
        }
    } else {
        Text("Email: $email")
        Button(onClick = { open = true }, modifier = Modifier.semantics {
            contentDescription = "Open email form"
        }) {
            Text("Change Email")
        }
    }
}

/**
 * Displays a form for the user to input a new password. The first input password
 * is error checked externally, whereas the confirmation password only checks itself
 * against the first password.
 *
 * @param passwordState The state for the to-be-submitted password that can be error checked
 *                      according to business logic.
 * @param onChangePassword Callback invoked when the first password input changes.
 * @param onSubmitPassword Callback invoked when the user presses "Submit."
 */
@Composable
private fun PasswordForm(
    passwordState: TextFieldState,
    onChangePassword: (String) -> Unit,
    onSubmitPassword: () -> Unit
) {
    var open: Boolean by remember { mutableStateOf(false) }

    var confirmPasswordState by remember { mutableStateOf(TextFieldState()) }
    val onConfirmPasswordChanged = { confirmPassword: String ->
        val error = if (confirmPassword != passwordState.text) {
            "Passwords do not match."
        } else null
        confirmPasswordState = TextFieldState(text = confirmPassword, error = error, dirty = true)
    }

    // Swap between Button and dual-TextField form depending on if user has clicked "Change"
    if (open) {
        PasswordField(
            state = passwordState,
            onValueChange = onChangePassword,
            hint = "Password"
        )

        PasswordField(
            state = confirmPasswordState,
            onValueChange = onConfirmPasswordChanged,
            fieldContentDescription = "Confirm password",
            hint = "Confirm password hint",
            hintContentDescription = "Confirm password hint",
        )
        Row {
            Button(onClick = { open = false }, modifier = Modifier.semantics {
                contentDescription = "Cancel password changes"
            }) {
                Text("Cancel")
            }
            Button(onClick = onSubmitPassword, modifier = Modifier.semantics {
                contentDescription = "Submit password changes"
            }) {
                Text("Submit")
            }
        }
    } else {
        Button(onClick = { open = true }, modifier = Modifier.semantics {
            contentDescription = "Open password form"
        }) {
            Text("Change Password")
        }
    }
}