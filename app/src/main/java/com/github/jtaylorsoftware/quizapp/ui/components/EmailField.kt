package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType

/**
 * Wraps a [TextField] for inputting an email.
 *
 * @param state Contains the input text to use as the value of the wrapped TextField and related state.
 * @param onValueChange Callback invoked when the email changes.
 * @param fieldContentDescription The content description used on the wrapped TextField.
 * @param hint Helper text to display when there is no error.
 * @param hintContentDescription The content description used on the [Text] containing [hint] as its value.
 */
@Composable
fun EmailField(
    state: TextFieldState,
    onValueChange: (String) -> Unit,
    fieldContentDescription: String = "Email",
    hint: String = "",
    hintContentDescription: String = "Email hint",
) {
    Column {
        TextField(
            value = state.text,
            onValueChange = onValueChange,
            modifier = Modifier.semantics {
                contentDescription = fieldContentDescription
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.dirty && state.error != null
        )
        Text(
            text = if (state.dirty) state.error ?: hint else hint,
            modifier = Modifier.semantics {
                contentDescription = hintContentDescription
            },
            color = if (state.dirty && state.error != null) MaterialTheme.colors.error else Color.Unspecified
        )
    }
}