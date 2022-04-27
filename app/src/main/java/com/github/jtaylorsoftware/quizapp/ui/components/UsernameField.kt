package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Presents an input for the user's username, which will be used to log in.
 *
 * @param state The state of the input, including the current text value.
 *
 * @param onTextChange Callback invoked when the email changes.
 *
 * @param showHint Whether to use the hint text while there are no errors. Default `false`.
 */
@Composable
fun UsernameField(
    state: TextFieldState,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showHint: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (KeyboardActionScope.() -> Unit)? = null,
) {
    AppTextField(
        state = state,
        onTextChange = onTextChange,
        label = "Username",
        hint = if (showHint) {
            "Your username will be used to log in."
        } else null,
        modifier = modifier,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
            autoCorrect = false
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onImeAction) else KeyboardActions.Default,
    )
}