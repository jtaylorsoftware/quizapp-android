package com.github.jtaylorsoftware.quizapp.ui.signinsignup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.AppLogoLarge
import com.github.jtaylorsoftware.quizapp.ui.components.PasswordField
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.components.UsernameField
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    login: () -> Unit,
    navigateToSignup: () -> Unit
) {
    val isInProgress = remember(uiState) { uiState.loginStatus.isInProgress }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

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
            PasswordField(
                state = uiState.passwordState,
                onTextChange = onPasswordChanged,
                modifier = Modifier.requiredWidth(300.dp),
                imeAction = ImeAction.Done,
                onImeAction = {
                    keyboardController?.hide()
                }
            )
            SignInSignUpButton(
                text = "Sign In",
                onClick = login,
                isInProgress = isInProgress,
                contentDescription = "Sign-in in progress"
            )
            Row {
                TextButton(navigateToSignup) {
                    Text("Sign Up", color = MaterialTheme.colors.secondaryVariant)
                }
            }
        }
    }
}

@Preview(widthDp = 800)
@Composable
private fun SignInScreenPreview() {
    QuizAppTheme {
        Surface(color = MaterialTheme.colors.background) {
            SignInScreen(
                uiState = SignInUiState(
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