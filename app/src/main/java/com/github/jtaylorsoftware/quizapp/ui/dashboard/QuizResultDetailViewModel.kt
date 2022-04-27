package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.ResultOrFailure
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.UiState
import com.github.jtaylorsoftware.quizapp.ui.UiStateSource
import com.github.jtaylorsoftware.quizapp.ui.isInProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [UiState] for a screen with a single [QuizResultDetailScreen].
 */
sealed interface QuizResultDetailUiState : UiState {
    /**
     * There is a viewable [QuizResult]. State includes the [QuizResult] and the
     * [QuizForm] for the associated Quiz, so that the results can be shown with
     * their associated questions.
     */
    data class QuizResultDetail(
        override val loading: LoadingState,
        val quizResult: QuizResult,
        val quizForm: QuizForm,
    ) : QuizResultDetailUiState

    /**
     * The screen data is either loading, or could not be loaded for an error other than authentication.
     * The error may be because the user it not authorized (forbidden) from viewing
     * the result because they did not submit either the associated quiz or result.
     */
    data class NoQuizResult(
        override val loading: LoadingState,
    ) : QuizResultDetailUiState


    companion object {
        internal fun fromViewModelState(state: QuizResultDetailViewModelState) =
            if (state.quizResult == null || state.quizForm == null) NoQuizResult(state.loading)
            else QuizResultDetail(
                state.loading,
                state.quizResult,
                state.quizForm,
            )
    }
}

internal data class QuizResultDetailViewModelState(
    val loading: LoadingState = LoadingState.NotStarted,
    val quizResult: QuizResult? = null,
    val quizForm: QuizForm? = null,
) {
    val screenIsBusy: Boolean = loading.isInProgress
}

@HiltViewModel
class QuizResultDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    private val quizResultRepository: QuizResultRepository,
) : ViewModel(), UiStateSource {
    private val quizId: ObjectId =
        ObjectId(requireNotNull(savedStateHandle.get<String>("quiz")) {
            "QuizResultViewModel must be given a quiz id"
        })
    private val userId: ObjectId =
        ObjectId(requireNotNull(savedStateHandle.get<String>("user")) {
            "QuizResultViewModel must be given a user id"
        })

    private var refreshJob: Job? = null
    private var state by mutableStateOf(QuizResultDetailViewModelState())

    override val uiState by derivedStateOf { QuizResultDetailUiState.fromViewModelState(state) }

    init {
        refresh()
    }

    /**
     * Refreshes the stored [QuizResult] data.
     *
     * Cannot be called when there is already loading happening.
     */
    fun refresh() {
        if (state.screenIsBusy) {
            return
        }

        loadData()
    }

    private fun loadData() {
        state = state.copy(loading = LoadingState.InProgress)

        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {

            // Get both the necessary form and result, cancelling as soon as either fails to load
            var nextState = handleLoadFormResult(state, quizRepository.getFormForQuiz(quizId))
            if (nextState.loading is LoadingState.Error) {
                state = nextState
                cancel()
            }

            nextState =
                handleLoadQuizResult(
                    nextState,
                    quizResultRepository.getForQuizByUser(quizId, userId)
                )
            if (nextState.loading is LoadingState.Error) {
                state = nextState
                cancel()
            }

            ensureActive()
            state = nextState.copy(loading = LoadingState.Success())
        }
    }

    private fun handleLoadFormResult(
        prevState: QuizResultDetailViewModelState,
        result: ResultOrFailure<QuizForm>
    ): QuizResultDetailViewModelState =
        when (result) {
            is Result.Success -> {
                prevState.copy(quizForm = result.value)
            }
            is Result.Failure -> {
                prevState.copy(loading = LoadingState.Error(result.reason))
            }
        }

    private fun handleLoadQuizResult(
        prevState: QuizResultDetailViewModelState,
        result: ResultOrFailure<QuizResult>
    ): QuizResultDetailViewModelState =
        when (result) {
            is Result.Success -> {
                prevState.copy(quizResult = result.value)
            }
            is Result.Failure -> {
                prevState.copy(loading = LoadingState.Error(result.reason))
            }
        }
}