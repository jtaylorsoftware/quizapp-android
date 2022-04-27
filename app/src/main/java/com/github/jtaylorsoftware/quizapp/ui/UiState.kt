package com.github.jtaylorsoftware.quizapp.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.SuccessStrings
import kotlinx.coroutines.flow.*

/**
 * Defines the most basic structure of state passed
 * to the UI layer.
 */
interface UiState {
    /**
     * Indicates progress on the current screen load or refresh operation.
     */
    val loading: LoadingState
}

/**
 * Represents the progress state of an action taken by a ViewModel on behalf of a screen,
 * such as submitting a form or refreshing the screen.
 *
 * Typically ViewModels and their UiState will use one [LoadingState] for
 * loading data on the screen, with additional [LoadingState] for form actions.
 *
 * State flow:
 * ```
 *
 *                          - Success -> Success
 * NotStarted -> InProgress |
 *                          - Exception -> Error
 *
 * (If restarted at Success or Error, begins at InProgress)
 * ```
 */
sealed interface LoadingState {
    /**
     * An optional message describing the why the state transitioned.
     * TODO: Strongly type after converting FailureReason/SuccessStrings to use Res Id
     */
    val message: Any?

    /**
     * Nothing is in progress and not an error. Used before any action
     * is taken.
     */
    object NotStarted : LoadingState {
        override val message: Any? = null
    }

    /**
     * The action is being fulfilled. Restarts should begin here.
     */
    object InProgress : LoadingState {
        override val message: Any? = null
    }

    /**
     * The last action did not complete; it failed with an error.
     */
    data class Error(override val message: FailureReason = FailureReason.UNKNOWN) : LoadingState

    /**
     * The last action succeeded.
     */
    data class Success(override val message: SuccessStrings = SuccessStrings.DEFAULT) : LoadingState
}

/**
 * Returns `true` when the [LoadingState] holds [LoadingState.InProgress].
 */
val LoadingState.isInProgress: Boolean
    inline get() = this is LoadingState.InProgress

/**
 * Returns `true` when the [LoadingState] holds a completion state -
 * either [LoadingState.Error] or [LoadingState.Success].
 */
val LoadingState.isDone: Boolean
    inline get() = this is LoadingState.Error || this is LoadingState.Success

/**
 * Delay value to be used to make it seem like operations take longer than they actually do.
 */
const val LOAD_DELAY_MILLI = 250L

interface UiStateSource {
    val uiState: UiState
}

/**
 * Computes and remembers if the current [LoadingState] of the [UiState]
 * represents a refresh of the data, rather than the first load.
 */
@Composable
fun rememberIsRefreshing(uiStateSource: UiStateSource): Boolean {
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiStateSource) {
        snapshotFlow { uiStateSource.uiState }
            .filter { it.loading !is LoadingState.NotStarted } // Ignore any "NotStarted" state on first render
            .map { it.loading is LoadingState.InProgress } // Map down to a boolean value
            .distinctUntilChanged() // Only collect transitions
            .drop(1) // Drop the first InProgress state
            .collect {
                isRefreshing = it
            }
    }

    return isRefreshing
}

/**
 * Shows a Snackbar with [LoadingState.message] when [state] changes.
 */
@Composable
fun ShowSnackbarOnLoadingState(state: LoadingState, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(state, snackbarHostState) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message.toString())
        }
    }
}

/**
 * A convenience wrapper over [DisposableEffect] that runs an effect on each change to the [UiState]'s
 * `loading` state.
 */
@Composable
fun OnDisposableLoadingState(
    loadingState: LoadingState,
    effect: DisposableEffectScope.(LoadingState) -> DisposableEffectResult
) {
    DisposableEffect(loadingState) {
        effect(loadingState)
    }
}

/**
 * A convenience wrapper over [LaunchedEffect] that runs a block on each change to the [UiState]'s
 * `loading` state. It passes [loadingState] to [block].
 */
@Composable
fun OnLoadingState(loadingState: LoadingState, block: (LoadingState) -> Unit) {
    LaunchedEffect(loadingState) {
        block(loadingState)
    }
}

/**
 * A [LaunchedEffect] that runs [block] on when [loadingState] is [LoadingState.Success].
 */
@Composable
fun OnSuccess(loadingState: LoadingState, block: () -> Unit) {
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Success) {
            block()
        }
    }
}

/**
 * A [LaunchedEffect] that runs [block] on when any [LoadingState] is [LoadingState.Success].
 */
@Composable
fun OnSuccess(loadingState1: LoadingState, loadingState2: LoadingState, block: () -> Unit) {
    LaunchedEffect(loadingState1, loadingState2) {
        if (loadingState1 is LoadingState.Success || loadingState2 is LoadingState.Success) {
            block()
        }
    }
}