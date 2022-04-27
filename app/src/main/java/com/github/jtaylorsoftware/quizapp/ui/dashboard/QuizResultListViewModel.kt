package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [UiState] that holds the representation for the [QuizResultListScreen].
 *
 * It may instead hold neither representation if data is loading or there is a critical error.
 */
sealed interface QuizResultListUiState : UiState {
    /**
     * The list contains results all belonging to the same user.
     */
    data class ListForUser(
        override val loading: LoadingState,
        val data: List<QuizResultListing>,
        // API currently only allows viewing the result list for a user
        // when they are that user (through /users/me/results),
        // so the username would just be the signed-in user ("Your Results")
        // val username: String,
    ) : QuizResultListUiState

    /**
     * The list contains results all for one quiz.
     */
    data class ListForQuiz(
        override val loading: LoadingState,
        val data: List<QuizResultListing>,
        val quizTitle: String,
    ) : QuizResultListUiState

    /**
     * The screen data is either empty, or could not be loaded for an error other than authentication.
     */
    class NoQuizResults(
        override val loading: LoadingState,
        val quizTitle: String? = null,
    ) : QuizResultListUiState

    companion object {
        internal fun fromViewModelState(state: QuizResultListViewModelState) =
            when {
                state.data.isNullOrEmpty() -> NoQuizResults(
                    state.loading,
                    quizTitle = state.quizTitle,
                )
                state.quizId != null -> {
                    ListForQuiz(
                        state.loading,
                        state.data,
                        // If for some reason couldn't load correct title, fall back to results
                        // (since they're all for the same quiz), and lastly give a sensible default.
                        state.quizTitle ?: state.data.firstOrNull()?.quizTitle ?: "Quiz",
                    )
                }
                else -> {
                    ListForUser(
                        state.loading,
                        state.data,
                    )
                }
            }
    }
}

/**
 * Internal state representation for the [QuizResultListScreen].
 */
internal data class QuizResultListViewModelState(
    val loading: LoadingState = LoadingState.NotStarted,
    // Used when requesting results for a user - quiz must be null
    // val userId: String? = null,

    // Used when requesting results for a quiz - user must be null
    val quizId: ObjectId? = null,
    val quizTitle: String? = null,
    val data: List<QuizResultListing>? = null,
) {
    val screenIsBusy = loading.isInProgress
}

@HiltViewModel
class QuizResultListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,

    // Used to get signed-in user's results
    private val userRepository: UserRepository,

    // Used to get the data for a quiz, such as title
    private val quizRepository: QuizRepository,

    // Used to get results when requesting results for a quiz rather than a user
    private val quizResultRepository: QuizResultRepository
) : ViewModel(), UiStateSource {
    // API currently only allows viewing the resultlist for a user
    // when they are that user (through /users/me/results),
    // private val userId: String? = savedStateHandle.get("user")

    // Id of the quiz to get a list of results for (null when getting profile results)
    private val quizId: ObjectId? = savedStateHandle.get<String>("quiz")?.let { ObjectId(it) }

    private var refreshJob: Job? = null

    private var state by mutableStateOf(QuizResultListViewModelState(quizId = quizId))

    override val uiState by derivedStateOf { QuizResultListUiState.fromViewModelState(state) }

//    init {
    // For now because of API limitations, do not check given ids,
    // just assume null quizId means fetch profile
//        check(userId != null || quizId != null) {
//            "Must pass either a user or quiz id in SavedStateHandle"
//        }
//    }

    init {
        refresh()
    }

    /**
     * Refreshes the stored quiz result list data.
     *
     * Cannot be called when there is already loading happening.
     */
    fun refresh() {
        if (state.screenIsBusy) {
            return
        }

        loadQuizResults()
    }

    private fun loadQuizResults() {
        state = state.copy(loading = LoadingState.InProgress)

        refreshJob?.cancel()

        if (quizId == null) {
            // Assume it's for a user (right now only the signed-in one)
            getResultsForUser()
        } else {
            getResultsForQuiz()
        }
    }

    private fun getResultsForUser() {
        refreshJob = viewModelScope.launch {
            userRepository.getResults()
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

    private fun getResultsForQuiz() {
        check(quizId != null)

        refreshJob = viewModelScope.launch {
            // Get also the quiz title by loading the listing for that quiz,
            // so that the title can be displayed even when there's no results.
            //
            // Note this requires the signed in user to own the quiz too, or else
            // result is Forbidden.
            launch {
                getQuizListingAsState(quizId)
            }

            launch {
                getQuizResultsAsState(quizId)
            }
        }
    }

    private suspend fun getQuizListingAsState(quizId: ObjectId) = coroutineScope {
        quizRepository.getAsListing(quizId)
            .catch { emit(handleLoadException(it)) }
            .map { handleQuizListingResult(it) }
            .collect { nextState ->
                state = nextState
                if (nextState.loading is LoadingState.Error) {
                    cancel()
                }
            }
    }

    private fun handleQuizListingResult(result: ResultOrFailure<QuizListing>): QuizResultListViewModelState {
        // Check result and update state with title when success.
        // We don't set loading or refreshing because this isn't the primary data.
        return when (result) {
            is Result.Success -> state.copy(quizTitle = result.value.title)
            is Result.Failure ->
                // Only care about errors that indicate the rest of the data would also fail
                // to load (such as Unauthorized, Forbidden)
                if (result.reason == FailureReason.FORBIDDEN) {
                    QuizResultListViewModelState(loading = LoadingState.Error(FailureReason.FORBIDDEN))
                } else state
        }
    }

    private suspend fun getQuizResultsAsState(quizId: ObjectId) = coroutineScope {
        quizResultRepository.getAllListingsForQuiz(quizId)
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

    private fun handleLoadResult(result: ResultOrFailure<List<QuizResultListing>>): QuizResultListViewModelState =
        when (result) {
            is Result.Success -> state.copy(data = result.value)
            is Result.Failure -> state.copy(loading = LoadingState.Error(result.reason))
        }

    private fun handleLoadException(throwable: Throwable): Result.Failure<Nothing> {
        // For now just always return a Result.Failure with UNKNOWN
        return Result.Failure(FailureReason.UNKNOWN)
    }

}