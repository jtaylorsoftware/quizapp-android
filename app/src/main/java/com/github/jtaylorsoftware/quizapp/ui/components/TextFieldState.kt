package com.github.jtaylorsoftware.quizapp.ui.components

/**
 * Encompasses common varying state for Composables that accept text input
 * and display errors.
 * @param text The text to use as the TextField value
 * @param error A message to display when there are errors, or null if there are none.
 * @param dirty Flag indicating that the user has input data, determining if
 *              errors should display. This could also be set when a form is submitted
 *              with previously loaded data that has an error, but wasn't previously
 *              displayed.
 */
data class TextFieldState(
    val text: String = "",
    val error: String? = null,
    val dirty: Boolean = false
)
