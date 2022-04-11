package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.ResponseValidationErrors
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.*
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * [UiState] for [QuizFormScreen].
 */
sealed interface QuizFormUiState : UiState {
    /**
     * The Quiz for the form was found and the user can submit their responses.
     */
    data class Form(
        override val loading: LoadingState,
        val quiz: QuizForm,
        val responses: List<FormResponseState>
    ) : QuizFormUiState

    /**
     * The Quiz for the form is not loading or there was an error getting it, other than
     * authentication.
     */
    data class NoQuiz(
        override val loading: LoadingState
    ) : QuizFormUiState

    /**
     * The user must sign in again to view this resource.
     */
    object RequireSignIn : QuizFormUiState {
        override val loading: LoadingState = LoadingState.Error(ErrorStrings.UNAUTHORIZED.message)
    }

    companion object {
        internal fun fromViewModelState(state: QuizFormViewModelState) = when {
            state.unauthorized -> RequireSignIn
            state.quiz == null -> NoQuiz(state.loadingState)
            else -> Form(
                state.loadingState,
                state.quiz,
                state.responses,
            )
        }
    }
}

internal data class QuizFormViewModelState(
    override val loading: Boolean = false,
    override val error: String? = null,
    val unauthorized: Boolean = false,
    val quiz: QuizForm? = null,
    val responses: List<FormResponseState> = emptyList(),
) : ViewModelState

@HiltViewModel
class QuizFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    private val quizResultRepository: QuizResultRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val quizId: ObjectId =
        ObjectId(requireNotNull(savedStateHandle.get<String>("quiz")) {
            "QuizFormViewModel must be given a quiz id"
        })

    // Used for waiting on validation to finish before submitting responses
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    private val state = MutableStateFlow(QuizFormViewModelState())

    val uiState = state
        .map { QuizFormUiState.fromViewModelState(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuizFormUiState.fromViewModelState(state.value)
        )

    init {
        loadQuiz()
    }

    fun changeResponse(index: Int, newResponse: QuestionResponse) {
        if (state.value.loading) {
            return
        }

        if (index < 0 || index >= state.value.responses.size) {
            throw IndexOutOfBoundsException()
        }

        val response = state.value.responses[index]
        require(response.response.type == newResponse.type) {
            "Cannot change QuestionResponse type"
        }

        viewModelScope.launch(dispatcher) {
            // Immediately apply updates
            state.update {
                it.copy(
                    responses = it.responses.toMutableList().apply {
                        this[index] = this[index].copy(response = newResponse)
                    }
                )
            }
            // Add a validation job
            waitGroup.add {
                state.update {
                    it.copy(
                        responses = validateResponses(
                            requireNotNull(it.quiz).questions,
                            it.responses
                        )
                    )
                }
            }
        }
    }

    /**
     * Validates and then attempts to submit the quiz responses. When successful,
     * this function immediately invokes [onSuccess] within the context of [Dispatchers.Main]
     * without emitting a new value of [uiState].
     */
    fun submit(onSuccess: () -> Unit) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                state.update {
                    it.copy(
                        responses = validateResponses(
                            requireNotNull(it.quiz).questions,
                            it.responses
                        )
                    )
                }
            }

            // Try to wait for all validations to finish - because mutators don't run if
            // loading is `true` (which was set above), then waiting for all pending validations
            // should finish and let us know if there's errors
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            val currentResponses = state.value.responses

            // Check for errors
            if (responsesHaveErrors(currentResponses)) {
                state.update {
                    it.copy(loading = false, error = "Please fix form errors.")
                }
                return@launch
            }

            val result = quizResultRepository.createResponseForQuiz(
                currentResponses.map { it.response },
                quizId
            )
            handleSubmitResult(result, onSuccess)
        }
    }

    private suspend fun handleSubmitResult(
        result: Result<ObjectId, ResponseValidationErrors>,
        onSuccess: () -> Unit
    ) = when (result) {
        is Result.Success -> {
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
        is Result.BadRequest -> state.update {
            it.copy(
                loading = false,
                error = "Please fix form errors.",
                responses = mergeErrors(it.responses, result.error.answerErrors)
            )
        }
        is Result.Unauthorized -> state.update {
            it.copy(
                loading = false,
                unauthorized = true
            )
        }
        is Result.Expired -> state.update {
            it.copy(
                loading = false,
                error = "This Quiz has expired."
            )
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

    private fun loadQuiz() {
        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch {
            val result = quizRepository.getFormForQuiz(quizId)
            handleLoadResult(result)
        }
    }

    private suspend fun handleLoadResult(result: Result<QuizForm, Any?>) = withContext(dispatcher) {
        when (result) {
            is Result.Success -> state.update {
                it.copy(
                    quiz = result.value,
                    responses = createResponses(result.value),
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
                QuizFormViewModelState(unauthorized = true)
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

    /**
     * Creates the appropriate types of [FormResponseState] for each question of the quiz.
     */
    private fun createResponses(quiz: QuizForm): List<FormResponseState> =
        quiz.questions.map { question ->
            when (question) {
                is Question.FillIn -> FormResponseState(response = QuestionResponse.FillIn())
                is Question.MultipleChoice -> FormResponseState(response = QuestionResponse.MultipleChoice())
                is Question.Empty -> throw IllegalStateException("Can not create QuestionResponse for Question.Empty")
            }
        }

    private fun responsesHaveErrors(responses: List<FormResponseState>): Boolean = responses.any {
        it.error != null
    }

    /**
     * Merges the current responses' errors with a list of those from the repository.
     */
    private fun mergeErrors(responses: List<FormResponseState>, errors: List<String?>): List<FormResponseState> {
        val merged = responses.toMutableList()
        errors.forEachIndexed { index, err ->
            if (index in merged.indices) {
                merged[index] = merged[index].copy(error = err)
            }
        }
        return merged
    }

    private suspend fun validateResponses(
        questions: List<Question>,
        responses: List<FormResponseState>
    ) = coroutineScope {
        responses.mapIndexed { index, responseState ->
            async { validateResponse(questions[index], responseState.response) }
        }.awaitAll()
    }

    private fun validateResponse(
        question: Question,
        response: QuestionResponse
    ): FormResponseState =
        when (response) {
            is QuestionResponse.MultipleChoice -> FormResponseState(
                response = response,
                error = if (response.choice == -1 || response.choice >= (question as Question.MultipleChoice).answers.size) {
                    "Please select an answer."
                } else null
            )
            is QuestionResponse.FillIn -> FormResponseState(
                response = response,
                error = if (response.answer.isBlank()) {
                    "Please input an answer."
                } else null
            )
        }
}