package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizValidationErrors
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.*
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.quiz.QuizEditorUiState.*
import com.github.jtaylorsoftware.quizapp.util.AllowedUsersValidator
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.whileSelect
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * [UiState] that holds the representation for the [QuizEditorScreen].
 *
 * If creating a new Quiz, it will initially be [Creator].
 * When editing a Quiz, it will be [Editor] unless the given quizId cannot be used to find an
 * existing Quiz, in which case it will be [NoQuiz].
 */
sealed interface QuizEditorUiState : UiState {
    /**
     * `true` when we editing a previous quiz.
     */
    val editing: Boolean

    /**
     * There was no quiz requested, so a blank Quiz is provided.
     */
    data class Creator internal constructor(
        override val loading: LoadingState,
        internal val viewModelState: QuizEditorViewModelState,
    ) : QuizEditorUiState, QuizState by viewModelState {
        override val editing: Boolean = false
    }

    /**
     * The Editor is opened with a previous Quiz.
     */
    data class Editor internal constructor(
        override val loading: LoadingState,
        internal val viewModelState: QuizEditorViewModelState,
    ) : QuizEditorUiState, QuizState by viewModelState {
        override val editing: Boolean = true
    }

    /**
     * A previous Quiz was requested but could not be loaded.
     */
    data class NoQuiz constructor(
        override val loading: LoadingState,
    ) : QuizEditorUiState {
        override val editing: Boolean = false
    }

    /**
     * The user must sign in again to view this resource.
     */
    object RequireSignIn : QuizEditorUiState {
        override val editing: Boolean = false
        override val loading: LoadingState = LoadingState.Error(ErrorStrings.UNAUTHORIZED.message)
    }

    companion object {
        internal fun fromViewModelState(state: QuizEditorViewModelState): QuizEditorUiState = when {
            state.unauthorized -> RequireSignIn
            state.failedToLoad -> NoQuiz(state.loadingState)
            state.quizId != null -> Editor(state.loadingState, state)
            else -> Creator(state.loadingState, state)
        }
    }
}

/**
 * Internal state for the QuizEditor's forms and fields. It implements [QuizState] methods by
 * delegating to the ViewModel.
 */
internal class QuizEditorViewModelState(
    internal val viewModel: QuizEditorViewModel,
    val quizId: String? = null,
    val failedToLoad: Boolean = false,
    override val loading: Boolean = true,
    val unauthorized: Boolean = false,
    override val error: String? = null,
    override val title: TextFieldState = TextFieldState(),
    expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    isPublic: Boolean = true,
    allowedUsers: String = "",
    override val expirationError: String? = null,
    override val allowedUsersError: String? = null,
    override val questions: List<QuestionState> = emptyList(),
    override val questionsError: String? = null, // Error for when there aren't enough questions
) : ViewModelState, QuizState {
    override var expiration: Instant = expiration
        set(value) = viewModel.setExpiration(value)

    override var isPublic: Boolean = isPublic
        set(value) = viewModel.setIsPublic(value)

    override var allowedUsers: String = allowedUsers
        set(value) = viewModel.setAllowedUsers(value)

    override fun setTitle(value: String) = viewModel.setTitle(value)

    override fun addQuestion() = viewModel.addQuestion()

    override fun changeQuestionType(index: Int, newType: QuestionType) =
        viewModel.changeQuestionType(index, newType)

    override fun changeQuestion(index: Int, newState: QuestionState) =
        viewModel.changeQuestion(index, newState)

    override fun deleteQuestion(index: Int) = viewModel.deleteQuestion(index)
}

@HiltViewModel
class QuizEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val quizId: ObjectId? = savedStateHandle.get<String>("quiz")?.let { ObjectId(it) }
    private val editing = quizId != null

    // Original expiration for an edited quiz (it's allowed to submit edits with untouched expiration)
    private val originalExpiration = MutableStateFlow<Instant?>(null)

    // Used for waiting on validation to finish before submitting quiz
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    private val state =
        MutableStateFlow(QuizEditorViewModelState(viewModel = this, quizId = quizId?.value))

    val uiState = state
        .map {
            QuizEditorUiState.fromViewModelState(it)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuizEditorUiState.fromViewModelState(
                QuizEditorViewModelState(
                    viewModel = this,
                    quizId = quizId?.value
                )
            )
        )

    init {
        if (editing) {
            loadQuiz()
        } else {
            state.update {
                it.copy(loading = false)
            }
        }
    }

    /**
     * Validates and then attempts to upload the new or edited Quiz. When successful,
     * this function immediately invokes [onSuccess] within the context of [Dispatchers.Main]
     * without emitting a new value of [uiState].
     */
    fun submitQuiz(onSuccess: () -> Unit) {
        // If still loading initial data, or already submitting, don't try submit
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch(dispatcher) {
            // Start a validation of the entire Quiz.
            validateQuiz(state.value)

            // Try to wait for all validations to finish - because mutators don't run if
            // loading is `true` (which was set above), then waiting for all pending validations
            // should finish and let us know if there's errors
            try {
                waitGroup.wait(1000.milliseconds)
            } catch(e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            // Read validated state
            val currentState = state.value

            // If quiz has errors that we know of, don't submit
            if (quizHasErrors(currentState)) {
                state.update {
                    it.copy(loading = false)
                }
                return@launch
            }

            val quiz = currentState.run {
                Quiz(
                    date = Instant.now(),
                    title = title.text,
                    expiration = expiration,
                    isPublic = isPublic,
                    allowedUsers = AllowedUsersValidator.split(allowedUsers),
                    questions = questions.map { when (it) {
                        is QuestionState.Empty -> Question.Empty
                        is QuestionState.FillIn -> it.data
                        is QuestionState.MultipleChoice -> it.data
                    } }
                )
            }

            // Upload our quiz
            val result = if (editing) {
                check(quizId != null) {
                    "Cannot call submitQuiz when editing and null quizId"
                }
                quizRepository.editQuiz(quizId, quiz)
            } else {
                quizRepository.createQuiz(quiz)
            }

            // Check for success or failure
            handleSubmitResult(result, onSuccess)
        }
    }

    private suspend fun handleSubmitResult(result: Result<Any, QuizValidationErrors>, onSuccess: () -> Unit) {
        when (result) {
            is Result.Success -> {
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
            is Result.BadRequest -> state.update {
                it.copy(
                    loading = false,
                    error = "Please fix form errors.",
                    allowedUsersError = result.error.allowedUsers,
                    title = it.title.copy(error = result.error.title),
                    questionsError = result.error.questions,
                    expirationError = result.error.expiration
                )
            }
            is Result.Unauthorized -> state.update {
                it.copy(
                    loading = false,
                    unauthorized = true
                )
            }
            is Result.Forbidden -> state.update {
                it.copy(
                    loading = false,
                    failedToLoad = true,
                    error = ErrorStrings.FORBIDDEN.message
                )
            }
            else -> state.update {
                it.copy(
                    loading = false,
                    failedToLoad = true,
                    error = ErrorStrings.UNKNOWN.message
                )
            }
        }
    }

    private fun loadQuiz() {
        check(quizId != null) {
            "loadQuiz requires that a quiz id is retrieved from SavedState"
        }

        viewModelScope.launch {
            val result = quizRepository.getQuiz(quizId)
            withContext(dispatcher) {
                handleLoadResult(result)
            }
        }
    }

    private fun handleLoadResult(result: Result<Quiz, Any?>) {
        when (result) {
            is Result.Success -> {
                originalExpiration.update { result.value.expiration }
                state.update {
                    it.copy(
                        loading = false,
                        title = TextFieldState(text = result.value.title),
                        expiration = result.value.expiration,
                        isPublic = result.value.isPublic,
                        allowedUsers = result.value.allowedUsers.joinToString(", "),
                        questions = result.value.questions.map { question ->
                            QuestionState.fromQuestion(question)
                        }
                    )
                }
            }
            is Result.NotFound -> state.update {
                it.copy(
                    loading = false,
                    failedToLoad = true,
                    error = ErrorStrings.NOT_FOUND.message
                )
            }
            is Result.Unauthorized -> state.update {
                it.copy(
                    loading = false,
                    unauthorized = true
                )
            }
            is Result.Forbidden -> state.update {
                it.copy(
                    loading = false,
                    failedToLoad = true,
                    error = ErrorStrings.FORBIDDEN.message
                )
            }
            else -> state.update {
                it.copy(
                    loading = false,
                    failedToLoad = true,
                    error = ErrorStrings.UNKNOWN.message
                )
            }
        }
    }

    internal fun addQuestion() {
        // Don't allow changes while initially loading or attempting to submit quiz
        if (state.value.loading) {
            return
        }

        // Don't allow concurrent modifications to quiz while question structure is updated
        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch(dispatcher) {
            state.update {
                it.copy(questions = it.questions + QuestionState.Empty(), loading = false)
            }
        }
    }

    internal fun changeQuestionType(index: Int, newType: QuestionType) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        if (index < 0 || index >= state.value.questions.size) {
            state.update {
                it.copy(loading = false)
            }
            return
        }

        val newQuestion = when (newType) {
            QuestionType.FillIn -> QuestionState.FillIn()
            QuestionType.MultipleChoice -> QuestionState.MultipleChoice()
            else -> throw IllegalArgumentException("Cannot change to QuestionType.Empty")
        }

        viewModelScope.launch(dispatcher) {
            state.update { currentState ->
                currentState.questions.toMutableList().apply {
                    this[index] = newQuestion
                }.let {
                    currentState.copy(questions = it, loading = false)
                }
            }
        }
    }

    internal fun changeQuestion(index: Int, newState: QuestionState) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        if (index < 0 || index >= state.value.questions.size) {
            state.update {
                it.copy(loading = false)
            }
            return
        }

        viewModelScope.launch(dispatcher) {
            state.update { currentState ->
                currentState.questions.toMutableList().apply {
                    this[index] = newState
                }.let {
                    currentState.copy(questions = it, loading = false)
                }
            }

            // Run validation after updating with user's changes to question
            waitGroup.add {
                state.update {
                    it.copy(questions = validateQuestions(it.questions))
                }
            }
        }
    }

    internal fun deleteQuestion(index: Int) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        val size = state.value.questions.size
        if (size == 0 || index < 0 || index >= size) {
            state.update {
                it.copy(loading = false)
            }
            return
        }

        viewModelScope.launch(dispatcher) {
            state.update { currentState ->
                currentState.questions.toMutableList().apply {
                    removeAt(index)
                }.let {
                    currentState.copy(questions = it, loading = false)
                }
            }

            waitGroup.add {
                state.update {
                    it.copy(questionsError = validateQuestionsSize(it.questions.size))
                }
            }
        }
    }

    internal fun setTitle(titleText: String) {
        if (state.value.loading) {
            return
        }

        // Set input value immediately
        state.update {
            it.copy(title = it.title.copy(text = titleText, dirty = true))
        }

        viewModelScope.launch(dispatcher) {
            // Do validation
            waitGroup.add {
                state.update {
                    it.copy(title = validateTitle(it.title.text))
                }
            }
        }
    }

    internal fun setExpiration(expiration: Instant) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(expiration = expiration)
        }

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                state.update {
                    it.copy(expirationError = validateExpiration(it.expiration))
                }
            }
        }
    }

    internal fun setIsPublic(isPublic: Boolean) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(isPublic = isPublic)
        }
    }

    internal fun setAllowedUsers(allowedUsers: String) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(allowedUsers = allowedUsers)
        }

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                state.update {
                    it.copy(allowedUsersError = validateAllowedUsers(it.allowedUsers))
                }
            }
        }
    }

    /**
     * Determines if the current [QuizState] has errors. Does not perform validation, it only
     * checks if anything has errors.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun quizHasErrors(stateToCheck: QuizState): Boolean = coroutineScope {
        if (stateToCheck.allowedUsersError != null || stateToCheck.expirationError != null
            || stateToCheck.questionsError != null || stateToCheck.title.error != null
        ) {
            true
        } else run {
            stateToCheck.questions.map { question ->
                async {
                    question.error != null || question.questionTextError != null || question.correctAnswerError != null || when (question) {
                        is QuestionState.Empty -> true
                        is QuestionState.MultipleChoice -> question.answerErrors.any { it != null }
                        is QuestionState.FillIn -> false // already covered by correctAnswer and questionText
                    }
                }
            }.let { list ->
                // Wait for the first question that has errors
                var first: Int = -1
                var completed = 0
                whileSelect {
                    list.forEachIndexed { i, deferred ->
                        deferred.onAwait { hasError ->
                            if (hasError) {
                                first = i
                                return@onAwait false
                            }
                            completed++
                            completed != list.size
                        }
                    }
                }
                list.forEach {
                    it.cancel()
                }
                first != -1
            }
        }
    }

    /**
     * Runs validation on the [QuizState] portion of the current [state]. This does
     * attempt to modify [state], even when there are no new errors.
     *
     * This should be called only from [submitQuiz].
     */
    private suspend fun validateQuiz(currentState: QuizEditorViewModelState) {
        waitGroup.add {
            coroutineScope {
                val questionsError = async { validateQuestionsSize(currentState.questions.size) }
                val validatedQuestions = async { validateQuestions(currentState.questions) }
                val allowedUsersError = async { validateAllowedUsers(currentState.allowedUsers) }
                val validatedTitle = validateTitle(currentState.title.text)
                val expirationError = validateExpiration(currentState.expiration)
                state.update {
                    it.copy(
                        questionsError = questionsError.await(),
                        questions = validatedQuestions.await(),
                        title = validatedTitle,
                        expirationError = expirationError,
                        allowedUsersError = allowedUsersError.await()
                    )
                }
            }
        }
    }

    private fun validateTitle(title: String) =
        if (title.isBlank()) {
            TextFieldState(text = title, error = "Title must not be blank.", dirty = true)
        } else TextFieldState(text = title, dirty = true)

    private fun validateExpiration(expiration: Instant) =
        // Allow the original expiration if editing
        if (!(editing && expiration == originalExpiration.value) || !editing && expiration < Instant.now()) {
            "Expiration must be in the future."
        } else null

    private fun validateAllowedUsers(allowedUsers: String) =
        if (!AllowedUsersValidator.validate(allowedUsers)) {
            "Enter a comma-separated list of valid usernames."
        } else null

    private fun validateQuestionsSize(size: Int): String? = if (size == 0) {
        "Add at least one question."
    } else null

    private suspend fun validateQuestions(questions: List<QuestionState>) = coroutineScope {
        questions.map { async { validateQuestion(it) } }.awaitAll()
    }

    /**
     * Validates a [QuestionState] and returns it updated with validation applied.
     */
    private fun validateQuestion(question: QuestionState): QuestionState {
        val questionTextError = if (question.data.text.isBlank()) {
            "Question prompt must not be blank."
        } else null

        return when (question) {
            is QuestionState.Empty -> question.copy("Must select a Question type.")
            is QuestionState.MultipleChoice -> {
                validateMultipleChoice(question).copy(questionTextError = questionTextError)
            }
            is QuestionState.FillIn -> {
                validateFillIn(question).copy(questionTextError = questionTextError)
            }
        }
    }

    private fun validateMultipleChoice(question: QuestionState.MultipleChoice): QuestionState.MultipleChoice {
        val answersError = if (question.data.answers.size < 2) {
            "Must add at least two answers."
        } else null
        val answerErrors = question.data.answers.map {
            if (it.text.isBlank()) {
                "Answer text must not be blank."
            } else null
        }
        // intentional use of let on nullable without safe call - only want the outer `else`
        // when the original value was null, not when the result of outer `if` is null
        val correctAnswerError = question.data.correctAnswer.let {
            if (it != null) {
                if (it < 0 || it >= question.data.answers.size) {
                    "Correct answer must be within the number of answers."
                } else null
            } else {
                "Must select a correct answer."
            }
        }
        return question.copy(
            error = answersError,
            correctAnswerError = correctAnswerError,
            answerErrors = answerErrors
        )
    }

    private fun validateFillIn(question: QuestionState.FillIn): QuestionState.FillIn {
        val correctAnswerError = question.data.correctAnswer.let {
            if (it.isNullOrBlank()) {
                "Must input a correct answer."
            } else null
        }
        return question.copy(correctAnswerError = correctAnswerError)
    }
}

internal fun QuizEditorViewModelState.copy(
    quizId: String? = this.quizId,
    failedToLoad: Boolean = this.failedToLoad,
    loading: Boolean = this.loading,
    unauthorized: Boolean = this.unauthorized,
    error: String? = this.error,
    title: TextFieldState = this.title,
    expiration: Instant = this.expiration,
    isPublic: Boolean = this.isPublic,
    allowedUsers: String = this.allowedUsers,
    expirationError: String? = this.expirationError,
    allowedUsersError: String? = this.allowedUsersError,
    questions: List<QuestionState> = this.questions,
    questionsError: String? = this.questionsError,
) = QuizEditorViewModelState(
    viewModel = viewModel,
    loading = loading,
    quizId = quizId,
    failedToLoad = failedToLoad,
    unauthorized = unauthorized,
    error = error,
    title = title,
    expiration = expiration,
    isPublic = isPublic,
    allowedUsers = allowedUsers,
    expirationError = expirationError,
    allowedUsersError = allowedUsersError,
    questions = questions,
    questionsError = questionsError,
)