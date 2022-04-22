package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * Displays a user's information and provides forms to update
 * their profile data.
 *
 * @param onChangeEmail Callback invoked when user inputs text in the form.
 *
 * @param onChangePassword Callback invoked when the first password input changes.
 *
 * @param onSubmitPassword Callback invoked when the user presses "Submit" in the Password form.
 *
 * @param onSubmitEmail Callback invoked when the user presses "Submit" in the Email form.
 *
 * @param onClose Callback invoked when the user taps to close the editor.
 *
 */
@Composable
fun ProfileEditorScreen(
    uiState: ProfileUiState.Editor,
    onLogOut: () -> Unit,
    onChangeEmail: (String) -> Unit,
    onChangePassword: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onSubmitPassword: () -> Unit,
    onClose: () -> Unit,
) {
    val initialEmail = remember { uiState.data.email }
    var emailFormIsOpen: Boolean by remember { mutableStateOf(false) }
    var passwordFormIsOpen: Boolean by remember { mutableStateOf(false) }


    Column(Modifier.verticalScroll(rememberScrollState()).testTag("ProfileEditorScreen")) {
        EmailForm(
            initialEmail,
            uiState.emailState,
            emailFormIsOpen,
            { emailFormIsOpen = !emailFormIsOpen },
            uiState.submitEmailStatus,
            onChangeEmail,
            onSubmitEmail
        )
        PasswordForm(
            uiState.passwordState,
            passwordFormIsOpen,
            { passwordFormIsOpen = !passwordFormIsOpen },
            uiState.submitPasswordStatus,
            onChangePassword,
            onSubmitPassword
        )
        Button(onClick = onLogOut, modifier = Modifier.testTag("LogOut")) {
            Text("Log out")
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Icon(
                painterResource(R.drawable.ic_logout_24),
                "Log out",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
        Text("Account deletion available when signed into the web app.")
    }

    BackHandler {
        onClose()
    }
}

@Preview
@Composable
private fun ProfileEditorScreenPreview() {
    QuizAppTheme {
        Surface {
            ProfileEditorScreen(
                uiState = ProfileUiState.Editor(
                    LoadingState.NotStarted,
                    User(username = "Username", email = "email@example.com"),
                    TextFieldState(),
                    TextFieldState(),
                    LoadingState.NotStarted,
                    LoadingState.NotStarted,
                ),
                onChangeEmail = {},
                onChangePassword = {},
                onSubmitEmail = {},
                onSubmitPassword = {},
                onClose = {},
                onLogOut = {},
            )
        }
    }
}

/**
 * Displays a TopAppBar styled like it's inline with the form.
 */
@Composable
fun EditorTopBar(onClose: () -> Unit) {
    TopAppBar(
        title = { Text("Edit Profile") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close profile editor")
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    )
}

/**
 * Displays the user's current email, with a button to open a form to change the email.
 *
 * @param email The user's current email before any changes.
 * @param emailState State containing any changes to the email.
 * @param isOpen `true` when form should be "open" (display TextFields).
 * @param toggleOpen Function to change open/close state of form.
 * @param submitEmailStatus Status of email form submission.
 * @param onChangeEmail Callback invoked when user inputs text in the form.
 * @param onSubmitEmail Callback invoked when user wants to submit their changes.
 */
@Composable
private fun EmailForm(
    email: String,
    emailState: TextFieldState,
    isOpen: Boolean,
    toggleOpen: () -> Unit,
    submitEmailStatus: LoadingState,
    onChangeEmail: (String) -> Unit,
    onSubmitEmail: () -> Unit
) {
    val isInProgress = remember(submitEmailStatus) { submitEmailStatus is LoadingState.InProgress }
    val submitButtonAlpha by derivedStateOf { if (isInProgress) 0.0f else 1.0f }

    // Swap between Text and TextField depending on if user has clicked "Change"
    if (isOpen) {
        EmailField(state = emailState, onValueChange = onChangeEmail)

        Row {
            Button(
                onClick = toggleOpen,
                enabled = !isInProgress,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel email changes"
                }) {
                Text("Cancel")
            }

            ButtonWithProgress(
                onClick = onSubmitEmail,
                isInProgress = isInProgress,
                progressIndicator = {
                    SmallCircularProgressIndicator(
                        Modifier.semantics { contentDescription = "Change email in progress" }
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = "Submit email changes"
                }
            ) {
                Text("Submit", modifier = Modifier.alpha(submitButtonAlpha))
            }
        }
    } else {
        Text("Email: $email")
        Button(onClick = toggleOpen, modifier = Modifier.semantics {
            contentDescription = "Open email form"
        }) {
            Text("Change Email")
        }
    }
}

@Preview
@Composable
private fun EmailFormPreview() {
    QuizAppTheme {
        Surface {
            Column {
                EmailForm(
                    email = "example@email.com",
                    emailState = TextFieldState(),
                    isOpen = true,
                    toggleOpen = {},
                    submitEmailStatus = LoadingState.InProgress,
                    onChangeEmail = {},
                    onSubmitEmail = {}
                )
            }
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
 * @param isOpen `true` when form should be "open" (display TextFields).
 * @param toggleOpen Function to change open/close state of form.
 * @param submitPasswordStatus Status of email form submission.
 * @param onChangePassword Callback invoked when the first password input changes.
 * @param onSubmitPassword Callback invoked when the user presses "Submit."
 */
@Composable
private fun PasswordForm(
    passwordState: TextFieldState,
    isOpen: Boolean,
    toggleOpen: () -> Unit,
    submitPasswordStatus: LoadingState,
    onChangePassword: (String) -> Unit,
    onSubmitPassword: () -> Unit
) {
    val isInProgress =
        remember(submitPasswordStatus) { submitPasswordStatus is LoadingState.InProgress }
    val submitButtonAlpha by derivedStateOf { if (isInProgress) 0.0f else 1.0f }

    var confirmPasswordState by remember { mutableStateOf(TextFieldState()) }
    val onConfirmPasswordChanged = { confirmPassword: String ->
        val error = if (confirmPassword != passwordState.text) {
            "Passwords do not match."
        } else null
        confirmPasswordState = TextFieldState(text = confirmPassword, error = error, dirty = true)
    }

    // Swap between Button and dual-TextField form depending on if user has clicked "Change"
    if (isOpen) {
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
            Button(onClick = toggleOpen, enabled = !isInProgress, modifier = Modifier.semantics {
                contentDescription = "Cancel password changes"
            }) {
                Text("Cancel")
            }

            ButtonWithProgress(
                onClick = onSubmitPassword,
                isInProgress = isInProgress,
                progressIndicator = {
                    SmallCircularProgressIndicator(
                        Modifier.semantics { contentDescription = "Change password in progress" }
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = "Submit password changes"
                }
            ) {
                Text("Submit", modifier = Modifier.alpha(submitButtonAlpha))
            }
        }
    } else {
        Button(onClick = toggleOpen, modifier = Modifier.semantics {
            contentDescription = "Open password form"
        }) {
            Text("Change Password")
        }
    }
}

@Preview
@Composable
private fun PasswordFormPreview() {
    QuizAppTheme {
        Surface {
            Column {
                PasswordForm(
                    passwordState = TextFieldState(text = "password"),
                    isOpen = true,
                    toggleOpen = {},
                    submitPasswordStatus = LoadingState.InProgress,
                    onChangePassword = {},
                    onSubmitPassword = {}
                )
            }
        }
    }
}