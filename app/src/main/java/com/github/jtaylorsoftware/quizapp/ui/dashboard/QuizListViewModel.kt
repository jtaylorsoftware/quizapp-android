package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [UiState] that holds the representation for the [QuizListScreen].
 *
 * Since the API currently only allows users to get their own created quizzes,
 * this list always displays the current user's quizzes.
 *
 * It may instead hold neither representation if data is loading or there is a critical error.
 */
sealed interface QuizListUiState : UiState {
    /**
     * The user has a list of created quizzes.
     */
    data class QuizList(
        override val loading: LoadingState,
        /**
         * Progress on deleting a quiz from the list.
         */
        val deleteQuizStatus: LoadingState,
        val data: List<QuizListing>
    ) : QuizListUiState

    /**
     * The screen data is either empty, or could not be loaded for an error other than authentication.
     */
    class NoQuizzes(
        override val loading: LoadingState,
    ) : QuizListUiState

    companion object {
        internal fun fromViewModelState(state: QuizListViewModelState): QuizListUiState =
            when {
                state.data.isNullOrEmpty() -> NoQuizzes(
                    state.loading,
                )
                else -> {
                    QuizList(
                        state.loading,
                        state.deleteQuizStatus,
                        state.data,
                    )
                }
            }
    }
}

/**
 * Internal state representation for the [QuizListScreen].
 */
internal data class QuizListViewModelState(
    val loading: LoadingState = LoadingState.NotStarted,
    val deleteQuizStatus: LoadingState = LoadingState.NotStarted,
    val data: List<QuizListing>? = null,
) {
    val screenIsBusy: Boolean = loading.isInProgress || deleteQuizStatus.isInProgress
}

@HiltViewModel
class QuizListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository
) : ViewModel(), UiStateSource {
    private var refreshJob: Job? = null

    private var state by mutableStateOf(QuizListViewModelState())

    override val uiState by derivedStateOf { QuizListUiState.fromViewModelState(state) }

    init {
        refresh()
    }

    /**
     * Refreshes the stored quiz list data.
     *
     * Cannot be called when there is already loading happening.
     */
    fun refresh() {
        // Do not allow refresh while loading deletion or refreshing data.
        if (state.screenIsBusy) {
            return
        }
        loadQuizzes()
    }

    /**
     * Loads quiz list data.
     */
    private fun loadQuizzes() {
        refreshJob?.cancel()

        state = state.copy(
            loading = LoadingState.InProgress,
            deleteQuizStatus = LoadingState.NotStarted
        )

        refreshJob = viewModelScope.launch {
            userRepository.getQuizzes()
                .catch { emit(handleLoadException(it)) }
                .map { handleLoadResult(it) }
                .collect { nextState ->
                    delay(LOAD_DELAY_MILLI)
                    state = nextState
                    if (nextState.loading is LoadingState.Error) {
                        cancel()
                    }
                }

            ensureActive()
            state = state.copy(loading = LoadingState.Success())
        }
    }

    private fun handleLoadResult(result: ResultOrFailure<List<QuizListing>>): QuizListViewModelState =
        when (result) {
            is Result.Success -> state.copy(data = result.value)
            is Result.Failure -> state.copy(loading = LoadingState.Error(result.reason))
        }

    private fun handleLoadException(throwable: Throwable): Result.Failure<Nothing> {
        // For now just always return a Result.Failure with UNKNOWN
        return Result.Failure(FailureReason.UNKNOWN)
    }

    /**
     * Deletes one of the user's quizzes (when the user taps delete on a listing).
     */
    fun deleteQuiz(id: ObjectId) {
        // Do not allow deletion with no data, or while loading deletion or refreshing list.
        if (state.data.isNullOrEmpty() || state.screenIsBusy) {
            return
        }

        state = state.copy(deleteQuizStatus = LoadingState.InProgress)

        viewModelScope.launch {
            handleDeleteQuizResult(quizRepository.deleteQuiz(id))
        }
    }

    private fun handleDeleteQuizResult(result: ResultOrFailure<Unit>) {
        state = when (result) {
            is Result.Success -> {
                state.copy(deleteQuizStatus = LoadingState.Success(SuccessStrings.DELETED_QUIZ))
            }
            is Result.Failure -> {
                state.copy(deleteQuizStatus = LoadingState.Error(result.reason))
            }
        }
    }
}