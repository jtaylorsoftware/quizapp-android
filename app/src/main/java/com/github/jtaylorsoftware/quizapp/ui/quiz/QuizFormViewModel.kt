package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.UiState
import com.github.jtaylorsoftware.quizapp.ui.UiStateSource
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.isInProgress
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import com.github.jtaylorsoftware.quizapp.util.anyAsync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
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
        /**
         * Progress for uploading the responses.
         */
        val uploadStatus: LoadingState,
        val responses: List<FormResponseState>
    ) : QuizFormUiState

    /**
     * The Quiz for the form is not loading or there was an error getting it, other than
     * authentication.
     */
    data class NoQuiz(
        override val loading: LoadingState
    ) : QuizFormUiState

    companion object {
        internal fun fromViewModelState(state: QuizFormViewModelState) = when {
            state.quiz == null -> NoQuiz(state.loading)
            else -> Form(
                state.loading,
                state.quiz,
                state.uploadStatus,
                state.responses,
            )
        }
    }
}

internal data class QuizFormViewModelState(
    val loading: LoadingState = LoadingState.NotStarted,
    val uploadStatus: LoadingState = LoadingState.NotStarted,
    val quiz: QuizForm? = null,
    val responses: List<FormResponseState> = emptyList(),
) {
    val screenIsBusy: Boolean = loading.isInProgress || uploadStatus.isInProgress
}

@HiltViewModel
class QuizFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    private val quizResultRepository: QuizResultRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel(), UiStateSource {
    private val quizId: ObjectId =
        ObjectId(requireNotNull(savedStateHandle.get<String>("quiz")) {
            "QuizFormViewModel must be given a quiz id"
        })

    // Used for waiting on validation to finish before submitting responses
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    private val responses = mutableStateListOf<ValidatedFormResponseState>()

    private var state by mutableStateOf(QuizFormViewModelState())
    override val uiState by derivedStateOf { QuizFormUiState.fromViewModelState(state) }

    init {
        loadQuiz()
    }

    /**
     * Validates and then attempts to submit the quiz responses.
     */
    fun uploadResponses() {
        if (state.screenIsBusy) {
            return
        }

        state = state.copy(uploadStatus = LoadingState.InProgress)

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                validateResponses()
            }

            // Try to wait for all validations to finish - because mutators don't run if
            // loading is `true` (which was set above), then waiting for all pending validations
            // should finish and let us know if there's errors
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            // Check for errors
            if (responsesHaveErrors()) {
                state = state.copy(uploadStatus = LoadingState.Error(FailureReason.FORM_HAS_ERRORS))
                return@launch
            }

            val result = quizResultRepository.createResponseForQuiz(
                responses.map { it.response.data },
                quizId
            )
            handleSubmitResult(result)
        }
    }

    private fun handleSubmitResult(
        result: Result<ObjectId, ResponseValidationErrors>
    ) {
        state = when (result) {
            is Result.Success -> {
                state.copy(
                    uploadStatus = LoadingState.Success(SuccessStrings.SUBMITTED_QUIZ_RESPONSE)
                )
            }
            is Result.Failure -> {
                mergeErrors(result.errors?.answerErrors ?: emptyList())
                state.copy(uploadStatus = LoadingState.Error(result.reason))
            }
        }
    }

    private fun loadQuiz() {
        state = state.copy(loading = LoadingState.InProgress)

        viewModelScope.launch {
            val result = quizRepository.getFormForQuiz(quizId)
            handleLoadResult(result)
        }
    }

    private suspend fun handleLoadResult(result: ResultOrFailure<QuizForm>) =
        withContext(dispatcher) {
            state = when (result) {
                is Result.Success -> {
                    createResponses(result.value)
                    state.copy(
                        quiz = result.value,
                        responses = responses.map { it.response },
                        loading = LoadingState.Success()
                    )
                }
                is Result.Failure -> {
                    state.copy(loading = LoadingState.Error(result.reason))
                }
            }
        }

    /**
     * Creates the appropriate types of [FormResponseState] for each question of the quiz.
     */
    private fun createResponses(quiz: QuizForm) {
        quiz.questions.forEach { question ->
            responses.add(
                when (question) {
                    is Question.FillIn -> FillInHolder()
                    is Question.MultipleChoice -> MultipleChoiceHolder(question.answers.size)
                    is Question.Empty -> throw IllegalStateException("Can not create QuestionResponse for Question.Empty")
                }
            )
        }
    }

    private suspend fun responsesHaveErrors(): Boolean = responses.anyAsync { it.hasErrors() }

    /**
     * Merges the current responses' errors with a list of those from the repository (network API).
     */
    private fun mergeErrors(
        errors: List<String?>
    ) {
        errors.forEachIndexed { index, err ->
            if (index in responses.indices) {
                val response = responses[index]
                responses[index] = when (response) {
                    is FillInHolder -> FillInHolder(response.answer.text, err)
                    is MultipleChoiceHolder -> MultipleChoiceHolder(
                        response.numAnswers,
                        response.choice,
                        err
                    )
                }
            }
        }
    }

    private suspend fun validateResponses() {
        supervisorScope {
            responses.map {
                async { it.revalidate() }
            }.awaitAll()
        }
    }

    /**
     * A [FormResponseState] that can be revalidated on demand.
     */
    private sealed interface ValidatedFormResponseState {
        /**
         * The held [FormResponseState] being validated.
         */
        val response: FormResponseState

        /**
         * Runs revalidation of this [FormResponseState].
         */
        suspend fun revalidate()

        /**
         * Returns `true` if this [FormResponseState] currently has errors.
         */
        suspend fun hasErrors(): Boolean
    }

    private inner class MultipleChoiceHolder(
        val numAnswers: Int,
        choice: Int = -1,
        error: String? = null,
    ) : ValidatedFormResponseState, FormResponseState.MultipleChoice {
        override val response: FormResponseState = this

        private var _choice by mutableStateOf(choice)
        override var choice: Int
            get() = _choice
            set(value) {
                if (state.screenIsBusy || value !in (0..numAnswers)) {
                    return
                }
                _choice = value
            }

        override var error: String? by mutableStateOf(error)

        override val data: QuestionResponse
            get() = QuestionResponse.MultipleChoice(choice)

        override suspend fun revalidate() {
            error = if (choice == -1) {
                // Never selected an answer
                "Select an answer"
            } else {
                null
            }
        }

        override suspend fun hasErrors(): Boolean = error != null
    }

    private inner class FillInHolder(
        answer: String = "",
        error: String? = null,
    ) : ValidatedFormResponseState, FormResponseState.FillIn {
        override val response: FormResponseState = this

        override var answer by mutableStateOf(TextFieldState(text = answer))
            private set

        override var error: String? by mutableStateOf(error)

        override val data: QuestionResponse
            get() = QuestionResponse.FillIn(answer = answer.text)

        override fun changeAnswer(text: String) {
            if (state.screenIsBusy) {
                return
            }

            answer = answer.copy(text = text)
            validateAnswer()
        }

        override suspend fun revalidate() {
            validateAnswer()
        }

        override suspend fun hasErrors(): Boolean = answer.error != null

        private fun validateAnswer() {
            if (answer.text.isBlank()) {
                answer = answer.copy(error = "Enter an answer", dirty = true)
            }
        }
    }
}