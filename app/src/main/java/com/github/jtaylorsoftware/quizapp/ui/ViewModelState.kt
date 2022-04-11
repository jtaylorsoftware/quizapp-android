package com.github.jtaylorsoftware.quizapp.ui

/**
 * Defines the most basic structure of state stored by ViewModels
 * before being transformed to a [UiState].
 *
 * (Optional to use, but provides reusable convenience methods and structure.)
 */
interface ViewModelState {
    /**
     * An action, such as refreshing the screen or submitting a form,
     * is being processed.
     */
    val loading: Boolean

    /**
     * Error for most recent load or processing action.
     */
    val error: String?
}

val ViewModelState.loadingState: LoadingState
    get() {
        return when {
            loading -> LoadingState.InProgress
            error != null -> LoadingState.Error(error ?: ErrorStrings.UNKNOWN.message)
            else -> LoadingState.AwaitingAction
        }
    }