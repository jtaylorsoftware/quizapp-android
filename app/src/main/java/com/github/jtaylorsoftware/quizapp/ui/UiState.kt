package com.github.jtaylorsoftware.quizapp.ui

/**
 * Defines the most basic structure of state passed
 * to the UI layer.
 */
interface UiState {
    /**
     * Indicates progress on the current operation, such as when the data to render
     * the screen is loading or a form is being submitted.
     */
    val loading: LoadingState
}

/**
 * Represents the progress state of the primary actions on a screen,
 * such as submitting a form or refreshing the screen.
 * Typically screens will use one [LoadingState]
 * for the entire screen and only allow one action at a time.
 */
sealed interface LoadingState {
    /**
     * Nothing is in progress and not an error. Used before any action
     * is taken and after a successful action is fulfilled.
     */
    object AwaitingAction : LoadingState

    /**
     * The action is being fulfilled.
     */
    object InProgress : LoadingState

    /**
     * The last action did not complete, it failed with an error.
     */
    data class Error(val message: String) : LoadingState

    companion object {
        /**
         * Beginning state for [LoadingState].
         */
        val Default = AwaitingAction
    }
}