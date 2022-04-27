package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * An opinionated [TextField] composable for the app. It assumes some [TextField] composable
 * parameters like `label` will always display a simple [Text], so this accepts those parameters as [String].
 *
 * @param state Container for the state of the [AppTextField]
 *
 * @param onTextChange Callback invoked when the content of [TextField] changes
 *
 * @param modifier Modifier applied to the internal [TextField].
 *
 * @param label Required name of the property that this [AppTextField] is for, such as
 * "Username."
 *
 * @param hint A concise string to display below the internal [TextField] when it is not in error.
 * When it is in error, [state]'s error value will be used as the hint instead.
 *
 * @param containerModifier Modifier applied to the container so that one could, as an example,
 * make the [AppTextField] match its parent width. By default, a Modifier with `width` [IntrinsicSize.Min]
 * is used.
 */
@Composable
fun AppTextField(
    state: TextFieldState,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    hint: String? = null,
    containerModifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val isError = remember(state) { state.dirty && state.error != null }
    val hintText by derivedStateOf { if (isError) state.error else hint }
    val hintColor = if (isError) MaterialTheme.colors.error else Color.Unspecified

    Column(containerModifier.width(IntrinsicSize.Min)) {
        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            modifier = modifier,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions,
            label = { Text(label) },
        )
        // Provide a minimum amount of space between this and the next composable
        // even when there is no hint
        val lineHeight = 14.sp
        val paddingFromBaselineSp = 16.sp
        val paddingFromBaselineDp = with(LocalDensity.current) { paddingFromBaselineSp.toDp() }
        val boxHeight =
            with(LocalDensity.current) {
                lineHeight.toDp() + paddingFromBaselineSp.toDp()
            }

        Box(
            Modifier
                .semantics(mergeDescendants = true) {}
                .requiredHeight(boxHeight)
                .padding(bottom = 4.dp)
        ) {
            hintText?.let { text ->
                Row(Modifier.paddingFromBaseline(top = paddingFromBaselineDp, bottom = 0.dp)) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        // Hint Text - a caption that is red on error
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = 16.dp),
                            color = hintColor,
                            lineHeight = lineHeight,
                            style = MaterialTheme.typography.caption,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


/**
 * Encompasses common varying state for Composables that accept text input
 * and display errors.
 *
 * @param text The text to use as the TextField value
 *
 * @param error A message to display when there are errors, or null if there are none.
 * A maximum of 2 lines of this error text will ever be displayed.
 *
 * @param dirty Flag indicating that the user has input data, determining if
 * errors should display. This could also be set when a form is submitted
 * with previously loaded data that has an error, but wasn't previously
 * displayed.
 */
data class TextFieldState(
    val text: String = "",
    val error: String? = null,
    val dirty: Boolean = false
) {
    companion object {
        val Saver by lazy {
            val textKey = "Text"
            val errorKey = "Error"
            val dirtyKey = "Dirty"
            mapSaver(
                save = {
                    mutableMapOf(
                        textKey to it.text,
                        dirtyKey to it.dirty
                    ).apply {
                        if (it.error != null) {
                            this[errorKey] = it.error
                        }
                    }
                },
                restore = {
                    TextFieldState(
                        it[textKey] as String,
                        it[errorKey] as String?,
                        it[dirtyKey] as Boolean
                    )
                }
            )
        }
    }
}

@Preview(widthDp = 600)
@Composable
private fun AppTextFieldPreview() {
    val state = TextFieldState(
        text = "My text",
        error = "An error ".repeat(10),
        dirty = true,
    )
    QuizAppTheme {
        Surface {
            Column(Modifier.fillMaxWidth()) {
                AppTextField(
                    state = state,
                    onTextChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = "Text field"
                )
            }
        }
    }
}