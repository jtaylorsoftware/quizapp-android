package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.QuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [UiState] for a screen with a single [ResultDetail].
 */
sealed interface QuizResultDetailUiState : UiState {
    /**
     * There is a viewable [QuizResult]. Subsequent refreshes affect
     * [processing] but will not clear previously loaded data.
     */
    data class QuizResultDetail(
        override val loading: LoadingState,
        val data: QuizResult
    ) : QuizResultDetailUiState

    /**
     * The screen data is either loading, or could not be loaded for an error other than authentication.
     * The error may be because the user it not authorized (forbidden) from viewing
     * the result because they did not submit either the associated quiz or result.
     */
    data class NoQuizResult(
        override val loading: LoadingState,
    ) : QuizResultDetailUiState

    /**
     * The user must sign in again to view this resource.
     */
    object RequireSignIn : QuizResultDetailUiState {
        override val loading: LoadingState = LoadingState.Error(ErrorStrings.UNAUTHORIZED.message)
    }

    companion object {
        internal fun fromViewModelState(state: QuizResultDetailViewModelState) = when {
            state.unauthorized -> RequireSignIn
            state.data == null -> NoQuizResult(state.loadingState)
            else -> QuizResultDetail(
                state.loadingState,
                state.data
            )
        }
    }
}

internal data class QuizResultDetailViewModelState(
    override val loading: Boolean = false,
    override val error: String? = null,
    val unauthorized: Boolean = false,
    val data: QuizResult? = null,
) : ViewModelState

@HiltViewModel
class QuizResultDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizResultRepository: QuizResultRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val quizId: ObjectId =
        ObjectId(requireNotNull(savedStateHandle.get<String>("quiz")) {
            "QuizResultViewModel must be given a quiz id"
        })
    private val userId: ObjectId =
        ObjectId(requireNotNull(savedStateHandle.get<String>("user")) {
            "QuizResultViewModel must be given a user id"
        })

    private var refreshJob: Job? = null
    private val state = MutableStateFlow(QuizResultDetailViewModelState())

    val uiState = state
        .map { QuizResultDetailUiState.fromViewModelState(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuizResultDetailUiState.fromViewModelState(state.value)
        )

    init {
        refresh()
    }

    /**
     * Refreshes the stored [QuizResult] data.
     *
     * Cannot be called when there is already loading happening.
     */
    fun refresh() {
        if (state.value.loading) {
            return
        }

        refreshJob?.cancel()

        state.update {
            it.copy(loading = true)
        }

        refreshJob = viewModelScope.launch {
            handleResult(quizResultRepository.getForQuizByUser(quizId, userId))
        }
    }

    private suspend fun handleResult(result: Result<QuizResult, Any?>) = withContext(dispatcher) {
        when (result) {
            is Result.Success -> state.update {
                it.copy(
                    data = result.value,
                    error = null,
                    loading = false,
                )
            }
            is Result.NotFound -> state.update {
                it.copy(
                    error = ErrorStrings.NOT_FOUND.message,
                    loading = false,
                )
            }
            is Result.Unauthorized -> state.update {
                QuizResultDetailViewModelState(unauthorized = true)
            }
            is Result.Forbidden -> state.update {
                it.copy(
                    loading = false,
                    error = ErrorStrings.FORBIDDEN.message
                )
            }
            is Result.NetworkError -> state.update {
                it.copy(
                    loading = false,
                    error = ErrorStrings.NETWORK.message
                )
            }
            else -> state.update {
                it.copy(
                    loading = false,
                    error = ErrorStrings.UNKNOWN.message
                )
            }
        }
    }
}