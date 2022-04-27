package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Presents an input for the user's email.
 *
 * @param state The state of the input, including the current text value.
 *
 * @param onTextChange Callback invoked when the email changes.
 */
@Composable
fun EmailField(
    state: TextFieldState,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (KeyboardActionScope.() -> Unit)? = null,
) {
    AppTextField(
        state = state,
        onTextChange = onTextChange,
        label = "Email",
        modifier = modifier,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction,
            autoCorrect = false
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onImeAction) else KeyboardActions.Default,
    )
}