package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.github.jtaylorsoftware.quizapp.R

/**
 * Presents an input for the user's password.
 *
 * @param state The state of the input, including the current text value.
 *
 * @param onTextChange Callback invoked when the email changes.
 */
@Composable
fun PasswordField(
    state: TextFieldState,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (KeyboardActionScope.() -> Unit)? = null,
) = PasswordField(
    state = state,
    onTextChange = onTextChange,
    label = "Password",
    modifier = modifier,
    imeAction = imeAction,
    onImeAction = onImeAction
)

/**
 * Presents an input for the user to confirm their password, such as when on the signup screen.
 *
 * @param state The state of the input, including the current text value.
 *
 * @param onTextChange Callback invoked when the email changes.
 */
@Composable
fun ConfirmPasswordField(
    state: TextFieldState,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction,
    onImeAction: (KeyboardActionScope.() -> Unit)? = null,
) = PasswordField(
    state = state,
    onTextChange = onTextChange,
    label = "Confirm password",
    modifier = modifier,
    imeAction = imeAction,
    onImeAction = onImeAction
)


@Composable
private fun PasswordField(
    state: TextFieldState,
    onTextChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction,
    onImeAction: (KeyboardActionScope.() -> Unit)? = null,
) {
    var passwordVisible: Boolean by rememberSaveable { mutableStateOf(false) }

    AppTextField(
        state = state,
        onTextChange = onTextChange,
        label = label,
        modifier = modifier,
        singleLine = true,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_key_24),
                contentDescription = null
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrect = false,
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onImeAction) else KeyboardActions.Default,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconToggleButton(
                checked = passwordVisible,
                onCheckedChange = { passwordVisible = it }) {
                if (passwordVisible) {
                    // Password currently shown - draw "closed" eye action icon
                    Icon(
                        painter = painterResource(id = R.drawable.ic_visibility_off_24),
                        contentDescription = "Hide password"
                    )
                } else {
                    // Password currently hidden - draw "open" eye action icon
                    Icon(
                        painter = painterResource(id = R.drawable.ic_visibility_24),
                        contentDescription = "Show password"
                    )
                }
            }
        },
    )
}