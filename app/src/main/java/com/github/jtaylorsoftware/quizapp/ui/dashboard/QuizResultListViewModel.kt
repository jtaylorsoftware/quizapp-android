package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.UserRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    /**
     * The user must sign in again to view this resource.
     */
    object RequireSignIn : QuizResultListUiState {
        override val loading: LoadingState = LoadingState.Error(ErrorStrings.UNAUTHORIZED.message)
    }

    companion object {
        internal fun fromViewModelState(state: QuizResultListViewModelState) =
            when {
                state.unauthorized -> RequireSignIn
                state.loading -> NoQuizResults(LoadingState.InProgress)
                state.data.isNullOrEmpty() -> NoQuizResults(state.loadingState, quizTitle = state.quizTitle)
                state.quizId != null -> {
                    ListForQuiz(
                        state.loadingState,
                        state.data,
                        // If for some reason couldn't load correct title, fall back to results
                        // (since they're all for the same quiz), and lastly give a sensible default.
                        state.quizTitle ?: state.data.firstOrNull()?.quizTitle ?: "Quiz",
                    )
                }
                else -> {
                    ListForUser(
                        state.loadingState,
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
    override val loading: Boolean = true,
    val unauthorized: Boolean = false,
    override val error: String? = null,

    // Used when requesting results for a user - quiz must be null
    // val userId: String? = null,

    // Used when requesting results for a quiz - user must be null
    val quizId: ObjectId? = null,
    val quizTitle: String? = null,
    val data: List<QuizResultListing>? = null,
) : ViewModelState

@HiltViewModel
class QuizResultListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,

    // Used to get signed-in user's results
    private val userRepository: UserRepository,

    // Used to get the data for a quiz, such as title
    private val quizRepository: QuizRepository,

    // Used to get results when requesting results for a quiz rather than a user
    private val quizResultRepository: QuizResultRepository
) : ViewModel() {
    // API currently only allows viewing the resultlist for a user
    // when they are that user (through /users/me/results),
    // private val userId: String? = savedStateHandle.get("user")

    // Id of the quiz to get a list of results for (null when getting profile results)
    private val quizId: ObjectId? = savedStateHandle.get<String>("quiz")?.let { ObjectId(it) }

    private var refreshJob: Job? = null
    private val state = MutableStateFlow(QuizResultListViewModelState(quizId = quizId))

    val uiState = state
        .map { QuizResultListUiState.fromViewModelState(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuizResultListUiState.fromViewModelState(state.value)
        )

    init {
        // For now because of API limitations, do not check given ids,
        // just assume null quizId means fetch profile
//        check(userId != null || quizId != null) {
//            "Must pass either a user or quiz id in SavedStateHandle"
//        }

        // Do initial load of data
        state.update {
            it.copy(loading = true)
        }
        getData()
    }

    /**
     * Refreshes the stored quiz result list data.
     *
     * Cannot be called when there is already loading happening.
     */
    fun refresh() {
        // Do not allow refresh while already loading or refreshing
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        getData()
    }

    private fun getData() {
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
                .onEach {
                    handleRefreshResult(it)
                }
                .catch {
                    handleRefreshException()
                }
                .collect()
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

    private suspend fun getQuizListingAsState(quizId: ObjectId) =
        quizRepository.getAsListing(quizId)
            .onEach {
                handleQuizListingResult(it)
            }
            .catch {}
            .collect()

    private fun handleQuizListingResult(result: Result<QuizListing, Any?>) {
        when (result) {
            is Result.Success -> state.update {
                it.copy(
                    quizTitle = result.value.title,
                    loading = false,
                )
            }
            is Result.Unauthorized -> state.update {
                QuizResultListViewModelState(unauthorized = true)
            }
            is Result.Forbidden -> state.update {
                QuizResultListViewModelState(
                    error = ErrorStrings.FORBIDDEN.message,
                    loading = false,
                )
            }
            else -> {
                // Ignore other errors because we only want some data that can
                // is acceptable to substitute with defaults (like title)
            }
        }
    }

    private suspend fun getQuizResultsAsState(quizId: ObjectId) =
        quizResultRepository.getAllListingsForQuiz(quizId)
            .onEach {
                handleRefreshResult(it)
            }
            .catch {
                handleRefreshException()
            }
            .collect()

    private fun handleRefreshResult(result: Result<List<QuizResultListing>, Any?>) {
        when (result) {
            is Result.Success -> state.update {
                it.copy(
                    data = result.value,
                    error = null,
                    loading = false,
                )
            }
            is Result.Unauthorized -> state.update {
                QuizResultListViewModelState(unauthorized = true)
            }
            is Result.Forbidden -> state.update {
                QuizResultListViewModelState(
                    error = ErrorStrings.FORBIDDEN.message,
                    loading = false,
                )
            }
            is Result.NotFound -> state.update {
                QuizResultListViewModelState(
                    error = ErrorStrings.NOT_FOUND.message,
                    loading = false,
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
}