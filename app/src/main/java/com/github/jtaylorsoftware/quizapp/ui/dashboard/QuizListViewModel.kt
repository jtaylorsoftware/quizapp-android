package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.UserRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        val data: List<QuizListing>
    ) : QuizListUiState

    /**
     * The screen data is either empty, or could not be loaded for an error other than authentication.
     */
    class NoQuizzes(
        override val loading: LoadingState,
    ) : QuizListUiState

    /**
     * The user must sign in again to view this resource.
     */
    object RequireSignIn : QuizListUiState {
        override val loading: LoadingState = LoadingState.Error(ErrorStrings.UNAUTHORIZED.message)
    }

    companion object {
        internal fun fromViewModelState(state: QuizListViewModelState): QuizListUiState =
            when {
                state.unauthorized -> RequireSignIn
                state.data.isNullOrEmpty() -> NoQuizzes(state.loadingState)
                else -> {
                    QuizList(
                        state.loadingState,
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
    override val loading: Boolean = false,
    val unauthorized: Boolean = false,
    override val error: String? = null,
    val data: List<QuizListing>? = null,
) : ViewModelState

@HiltViewModel
class QuizListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository
) : ViewModel() {
    private var refreshJob: Job? = null
    private val state = MutableStateFlow(QuizListViewModelState())

    val uiState = state
        .map { QuizListUiState.fromViewModelState(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuizListUiState.fromViewModelState(state.value)
        )

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
        if (state.value.loading) {
            return
        }

        refreshJob?.cancel()

        state.update {
            it.copy(loading = true)
        }

        refreshJob = viewModelScope.launch {
            userRepository.getQuizzes()
                .onEach {
                    handleRefreshResult(it)
                }
                .catch {
                   handleRefreshException()
                }
                .collect()
        }
    }

    private fun handleRefreshResult(result: Result<List<QuizListing>, Any?>) {
        when (result) {
            is Result.Success -> state.update {
                it.copy(
                    data = result.value,
                    error = null,
                    loading = false,
                )
            }
            is Result.Unauthorized -> state.update {
                QuizListViewModelState(
                    unauthorized = true,
                )
            }
            else -> state.update {
                it.copy(
                    error = ErrorStrings.NETWORK.message,
                    loading = false,
                )
            }
        }
    }

    private fun handleRefreshException() {
        state.update {
            it.copy(
                error = ErrorStrings.UNKNOWN.message,
                loading = false,
            )
        }
    }

    /**
     * Deletes one of the user's quizzes (when the user taps delete on a listing).
     */
    fun deleteQuiz(id: ObjectId) {
        val currentState = state.value
        // Do not allow deletion with no data, or while loading deletion or refreshing list.
        if (currentState.data.isNullOrEmpty() || currentState.loading || currentState.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch {
            handleDeleteQuizResult(quizRepository.deleteQuiz(id))
        }
    }

    private fun handleDeleteQuizResult(result: Result<Unit, Any?>){
        when (result) {
            is Result.Success -> {
                state.update {
                    it.copy(loading = false)
                }
            }
            is Result.Unauthorized -> {
                state.update {
                    QuizListViewModelState(
                        unauthorized = true,
                    )
                }
            }
            // Right now, it only displays the signed-in user's quizzes,
            // so it will never get forbidden if the user is signed-in
            // is Result.Forbidden -> {}
            else -> {
                state.update {
                    it.copy(
                        loading = false,
                        error = ErrorStrings.NETWORK.message
                    )
                }
            }
        }
    }
}