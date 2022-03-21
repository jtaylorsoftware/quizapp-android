package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.github.jtaylorsoftware.quizapp.R

/**
 * Wraps a [TextField] for inputting a password. It can also toggle visibility of the user's input.
 * When the password is hidden, it is replaced by the default mask of [PasswordVisualTransformation].
 *
 * @param state Contains the input text to use as the value of the wrapped TextField and related state.
 * @param onValueChange Callback invoked when the password changes.
 * @param fieldContentDescription The content description used on the wrapped TextField.
 * @param hint Helper text to display when there is no error.
 * @param hintContentDescription The content description used on the [Text] containing [hint] as its value.
 */
@Composable
fun PasswordField(
    state: TextFieldState,
    onValueChange: (String) -> Unit,
    fieldContentDescription: String = "Password",
    hint: String = "",
    hintContentDescription: String = "Password hint",
) {
    var passwordVisible: Boolean by remember { mutableStateOf(false) }
    Column {
        TextField(
            value = state.text,
            onValueChange = onValueChange,
            modifier = Modifier.semantics {
                contentDescription = fieldContentDescription
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconToggleButton(checked = passwordVisible, onCheckedChange = { passwordVisible = it }) {
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