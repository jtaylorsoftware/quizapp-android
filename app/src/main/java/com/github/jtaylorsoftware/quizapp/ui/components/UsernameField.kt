package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Wraps a [TextField] for inputting a username.
 *
 * @param state Contains the input text to use as the value of the wrapped TextField and related state.
 * @param onValueChange Callback invoked when the username changes.
 * @param fieldContentDescription The content description used on the wrapped TextField.
 * @param hint Helper text to display when there is no error.
 * @param hintContentDescription The content description used on the [Text] containing [hint] as its value.
 */
@Composable
fun UsernameField(
    state: TextFieldState,
    onValueChange: (String) -> Unit,
    fieldContentDescription: String = "Username",
    hint: String = "",
    hintContentDescription: String = "Username hint",
) {
    Column {
        TextField(
            value = state.text,
            onValueChange = onValueChange,
            modifier = Modifier.semantics {
                contentDescription = fieldContentDescription
            }
        )
        Text(
            text = if (state.dirty) state.error ?: hint else hint,
            modifier = Modifier.semantics {
                contentDescription = hintContentDescription
            }
        )
    }
}