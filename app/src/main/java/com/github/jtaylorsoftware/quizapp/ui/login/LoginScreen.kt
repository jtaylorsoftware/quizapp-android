package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.jtaylorsoftware.quizapp.ui.components.PasswordField
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.components.UsernameField

/**
 * Presents a screen for the user to input their login credentials.
 * After the user presses the "Sign In" button, the [login] callback is invoked.
 *
 * @param usernameState State containing the input username and state.
 * @param onUsernameChanged Callback invoked when the user inputs text to the "Username" field.
 * @param passwordState State containing the input password and state.
 * @param onPasswordChanged Callback invoked when the user inputs text to the "Password" field.
 * @param login Callback invoked when the user presses the "Sign In" button. Primary action on the screen.
 * @param navigateToRegister Callback invoked when the user presses the "Sign Up" text. Secondary action on the screen.
 */
@Composable
fun LoginScreen(
    usernameState: TextFieldState,
    onUsernameChanged: (String) -> Unit,
    passwordState: TextFieldState,
    onPasswordChanged: (String) -> Unit,
    login: () -> Unit,
    navigateToRegister: () -> Unit
) {
    Column {
        UsernameField(state = usernameState, onValueChange = onUsernameChanged, hint = "Username")
        PasswordField(state = passwordState, onValueChange = onPasswordChanged, hint = "Password")
        Button(onClick = login) {
            Text("Sign In")
        }
        Row {
            Text("Not registered?")
            Text("Sign Up", modifier = Modifier.clickable { navigateToRegister() })
        }
    }
}