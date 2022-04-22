package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.isInProgress
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * Presents a screen for the user to input their login credentials.
 * After the user presses the "Sign In" button, the [login] callback is invoked.
 *
 * @param uiState State for the screen.
 * @param onUsernameChanged Callback invoked when the user inputs text to the "Username" field.
 * @param onPasswordChanged Callback invoked when the user inputs text to the "Password" field.
 * @param login Callback invoked when the user presses the "Sign In" button. Primary action on the screen.
 * @param navigateToSignup Callback invoked when the user presses the "Sign Up" text. Secondary action on the screen.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    login: () -> Unit,
    navigateToSignup: () -> Unit
) {
    val isInProgress = remember(uiState) { uiState.loginStatus.isInProgress }
    val signInButtonTextAlpha by derivedStateOf { if (isInProgress) 0.0f else 1.0f }
    Column {
        UsernameField(state = uiState.usernameState, onValueChange = onUsernameChanged, hint = "Username")
        PasswordField(state = uiState.passwordState, onValueChange = onPasswordChanged, hint = "Password")
        ButtonWithProgress(
            onClick = login,
            isInProgress = isInProgress,
            progressIndicator = {
                SmallCircularProgressIndicator(
                    Modifier.semantics { contentDescription = "Login in progress" }
                )
            }
        ) {
            Text("Sign In", modifier = Modifier.alpha(signInButtonTextAlpha))
        }
        Row {
            Text("Not registered?")
            Text(
                "Sign Up",
                color = MaterialTheme.colors.secondary,
                modifier = Modifier
                    .clickable { navigateToSignup() }
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    QuizAppTheme {
        Surface {
            LoginScreen(
                uiState = LoginUiState(
                    loginStatus = LoadingState.InProgress,
                    usernameState = TextFieldState(),
                    passwordState = TextFieldState()
                ),
                onUsernameChanged = {},
                onPasswordChanged = {},
                login = {},
                navigateToSignup = {}
            )
        }
    }
}