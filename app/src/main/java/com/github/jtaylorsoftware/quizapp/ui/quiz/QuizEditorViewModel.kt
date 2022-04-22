package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.UiState
import com.github.jtaylorsoftware.quizapp.ui.UiStateSource
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.isInProgress
import com.github.jtaylorsoftware.quizapp.util.AllowedUsersValidator
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import com.github.jtaylorsoftware.quizapp.util.anyAsync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

/**
 * [UiState] that holds the representation for the [QuizEditorScreen].
 */
sealed interface QuizEditorUiState : UiState {
    /**
     * There was no quiz requested, so a blank Quiz is provided.
     */
    data class Editor constructor(
        override val loading: LoadingState,

        /**
         * Progress for submitting the Quiz.
         */
        val uploadStatus: LoadingState,

        /**
         * Whether or not the Editor was opened with a pre-existing Quiz.
         */
        val isEditing: Boolean,

        val quizState: QuizState,
    ) : QuizEditorUiState

    /**
     * A previous Quiz was requested but could not be loaded.
     */
    data class NoQuiz constructor(
        override val loading: LoadingState,
    ) : QuizEditorUiState

    companion object {
        internal fun fromViewModelState(state: QuizEditorViewModelState): QuizEditorUiState = when {
            state.loading.isInProgress || state.loading is LoadingState.Error -> NoQuiz(state.loading)
            state.quizId != null -> Editor(
                loading = state.loading,
                uploadStatus = state.uploadStatus,
                isEditing = true,
                quizState = state.quizState
            )
            else -> Editor(
                loading = state.loading,
                uploadStatus = state.uploadStatus,
                isEditing = false,
                quizState = state.quizState
            )
        }
    }
}

/**
 * Internal state for the [QuizEditorViewModel]. In holds the general authorization
 * state, the screen-specific loading and upload status, and the user's current edits
 * for a Quiz.
 */
internal data class QuizEditorViewModelState(
    val loading: LoadingState = LoadingState.NotStarted,
    val uploadStatus: LoadingState = LoadingState.NotStarted,

    /**
     * The id of the Quiz loaded to edit. `null` if creating a new Quiz.
     */
    val quizId: String? = null,

    /**
     * The state for the editor screen (the user's current changes to the Quiz).
     */
    val quizState: QuizState,
) {
    val screenIsBusy: Boolean =
        loading.isInProgress || uploadStatus.isInProgress
}

@HiltViewModel
class QuizEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel(), UiStateSource {
    private val quizId: ObjectId? = savedStateHandle.get<String>("quiz")?.let { ObjectId(it) }
    private val editing = quizId != null

    // Original expiration for an edited quiz (it's allowed to submit edits with untouched expiration)
    private var originalExpiration: Instant? by mutableStateOf(null)

    // Used for waiting on validation to finish before submitting quiz
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    private var quizState by mutableStateOf(QuizStateHolder(), neverEqualPolicy())
    private var state by mutableStateOf(
        QuizEditorViewModelState(
            quizId = quizId?.value,
            quizState = quizState
        )
    )
    override val uiState by derivedStateOf { QuizEditorUiState.fromViewModelState(state) }

    init {
        if (editing) {
            loadQuiz()
        }
    }

    /**
     * Validates and then attempts to upload the new or edited Quiz.
     */
    fun uploadQuiz() {
        // If still loading initial data, or already submitting, don't try submit
        if (state.screenIsBusy) {
            return
        }

        state = state.copy(uploadStatus = LoadingState.InProgress)

        viewModelScope.launch(dispatcher) {
            // Start a validation of the entire Quiz.
            quizState.revalidate()

            // Try to wait for all validations to finish - because mutators don't run if
            // loading is `true` (which was set above), then waiting for all pending validations
            // should finish and let us know if there's errors
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            // If quiz has errors that we know of, don't submit
            if (quizState.hasErrors()) {
                state = state.copy(uploadStatus = LoadingState.Error(FailureReason.FORM_HAS_ERRORS))
                return@launch
            }

            val quiz = quizState.data

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
            handleUploadResult(result)
        }
    }

    private fun handleUploadResult(result: Result<Any, QuizValidationErrors>) {
        when (result) {
            is Result.Success -> {
                state =
                    state.copy(uploadStatus = LoadingState.Success(SuccessStrings.UPLOADED_QUIZ))
            }
            is Result.Failure -> {
                quizState.allowedUsersError = result.errors?.allowedUsers
                quizState.title = quizState.title.copy(error = result.errors?.title)
                quizState.questionsError = result.errors?.questions
                quizState.expirationError = result.errors?.expiration
                state = state.copy(uploadStatus = LoadingState.Error(result.reason))
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

    private fun handleLoadResult(result: ResultOrFailure<Quiz>) {
        when (result) {
            is Result.Success -> {
                originalExpiration = result.value.expiration

                quizState = QuizStateHolder(
                    title = result.value.title,
                    expiration = result.value.expiration,
                    isPublic = result.value.isPublic,
                    allowedUsers = result.value.allowedUsers.joinToString(", "),
                    questions = result.value.questions
                )

                state = state.copy(loading = LoadingState.Success(), quizState = quizState)
            }
            is Result.Failure -> {
                state = state.copy(loading = LoadingState.Error(result.reason))
            }
        }
    }

    /**
     * [QuizState] implementation suitable for this ViewModel and its related screens.
     */
    private inner class QuizStateHolder(
        title: String = "",
        expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
        isPublic: Boolean = true,
        allowedUsers: String = "",
        questions: List<Question> = emptyList(),
    ) : QuizState {
        override val data: Quiz
            get() = Quiz(
                date = Instant.now(),
                title = title.text,
                expiration = expiration,
                isPublic = isPublic,
                allowedUsers = AllowedUsersValidator.split(allowedUsers.trim()),
                questions = questions.map {
                    when (it) {
                        is QuestionState.Empty -> Question.Empty
                        is QuestionState.FillIn -> it.data
                        is QuestionState.MultipleChoice -> it.data
                    }
                }
            )

        override var title: TextFieldState by mutableStateOf(TextFieldState(text = title))

        override fun changeTitleText(value: String) {
            if (state.screenIsBusy) {
                return
            }

            title = title.copy(text = value, dirty = true)

            viewModelScope.launch(dispatcher) {
                waitGroup.add {
                    validateTitle()
                }
            }
        }

        private var _expiration by mutableStateOf(expiration)
        override var expiration: Instant
            get() = _expiration
            set(value) {
                if (state.screenIsBusy) {
                    return
                }

                _expiration = value

                viewModelScope.launch(dispatcher) {
                    waitGroup.add {
                        quizState.validateExpiration()
                    }
                }
            }

        override var expirationError: String? by mutableStateOf(null)

        private var _isPublic by mutableStateOf(isPublic)
        override var isPublic: Boolean
            get() = _isPublic
            set(value) {
                if (state.screenIsBusy) {
                    return
                }

                _isPublic = value
            }

        private var _allowedUsers by mutableStateOf(allowedUsers)
        override var allowedUsers: String
            get() = _allowedUsers
            set(value) {
                if (state.screenIsBusy) {
                    return
                }

                _allowedUsers = value
                viewModelScope.launch(dispatcher) {
                    waitGroup.add {
                        quizState.validateAllowedUsers()
                    }
                }
            }

        override var allowedUsersError: String? by mutableStateOf(null)

        private val _questions =
            mutableStateListOf<ValidatedQuestionState>().apply {
                questions.forEach {
                    add(fromQuestion(it))
                }
            }

        override val questions: List<QuestionState>
            get() = _questions.map { it.question }

        override var questionsError: String? by mutableStateOf(null)

        override fun addQuestion() {
            // Don't allow changes while initially loading or attempting to submit quiz
            if (editing || state.screenIsBusy) {
                return
            }

            // Don't allow concurrent modifications to quiz while question structure is updated
            _questions.add(EmptyHolder())
        }

        override fun changeQuestionType(index: Int, newType: QuestionType) {
            if (editing || state.screenIsBusy) {
                return
            }

            if (index !in _questions.indices) {
                return
            }

            _questions[index] = when (newType) {
                QuestionType.Empty -> throw IllegalArgumentException("Cannot change QuestionType to Empty")
                QuestionType.FillIn -> FillInHolder()
                QuestionType.MultipleChoice -> MultipleChoiceHolder()
            }
        }

        override fun deleteQuestion(index: Int) {
            if (editing || state.screenIsBusy) {
                return
            }

            if (_questions.isEmpty() || index !in _questions.indices) {
                return
            }

            _questions.removeAt(index)

            viewModelScope.launch(dispatcher) {
                waitGroup.add {
                    validateQuestionsSize()
                }
            }
        }

        /**
         * Determines if the current [QuizState] has errors. Does not perform validation, it only
         * checks if anything has errors.
         */
        suspend fun hasErrors(): Boolean = allowedUsersError != null || expirationError != null
                || questionsError != null || title.error != null || _questions.anyAsync { it.hasErrors() }

        /**
         * Adds a full validation action on this [QuizState] to the ViewModel's [waitGroup].
         */
        suspend fun revalidate() {
            waitGroup.add {
                supervisorScope {
                    launch { validateQuestionsSize() }
                    launch { validateQuestions() }
                    launch { validateAllowedUsers() }
                    validateTitle()
                    validateExpiration()
                }
            }
        }

        private fun validateTitle() {
            title = title.text.let { text ->
                if (text.isBlank()) {
                    TextFieldState(text = text, error = "Title must not be blank.", dirty = true)
                } else TextFieldState(text = text, error = null, dirty = true)
            }
        }

        private fun validateExpiration() {
            // Allow only the original expiration if editing, or any future value if creating new quiz
            expirationError = if (
                editing && expiration != originalExpiration ||
                !editing && expiration.isBefore(Instant.now())
            ) {
                "Expiration must be in the future."
            } else null
        }

        private fun validateAllowedUsers() {
            allowedUsersError =
                if (allowedUsers.isNotBlank() && !AllowedUsersValidator.validate(allowedUsers)) {
                    "Enter a comma-separated list of valid usernames."
                } else null
        }

        private fun validateQuestionsSize() {
            questionsError = if (_questions.isEmpty()) {
                "Add at least one question."
            } else null
        }

        private suspend fun validateQuestions() {
            supervisorScope {
                _questions.map {
                    async { it.revalidate() }
                }.awaitAll()
            }
        }
    }

    private sealed interface ValidatedQuestionState {
        /**
         * The held [QuestionState] being validated.
         */
        val question: QuestionState

        /**
         * Runs revalidation of this [QuestionState].
         */
        suspend fun revalidate()

        /**
         * Returns `true` if this [QuestionState] currently has errors.
         */
        suspend fun hasErrors(): Boolean
    }

    private class EmptyHolder : QuestionState.Empty("Must select question type"),
        ValidatedQuestionState {
        override val question: QuestionState = this

        private var validated by mutableStateOf(false)

        override suspend fun revalidate() {
            validated = true
        }

        override suspend fun hasErrors(): Boolean = validated
    }

    private inner class MultipleChoiceHolder(
        questionText: String = "",
        correctAnswer: Int? = null,
        answers: List<Question.MultipleChoice.Answer> = emptyList(),
    ) : QuestionState.MultipleChoice, ValidatedQuestionState {
        override val key: String = UUID.randomUUID().toString()

        override val question: QuestionState = this

        override val data: Question.MultipleChoice
            get() = Question.MultipleChoice(
                text = prompt.text,
                correctAnswer = correctAnswer,
                answers = _answers.map { Question.MultipleChoice.Answer(text = it.text.text) }
            )

        override val error: String?
            get() = answersError ?: correctAnswerError

        override var prompt by mutableStateOf(TextFieldState(text = questionText))
            private set

        override var correctAnswer by mutableStateOf(correctAnswer)
            private set

        override var correctAnswerError: String? by mutableStateOf(null)
            private set

        private val _answers = mutableStateListOf<AnswerHolder>().apply {
            answers.forEach {
                add(AnswerHolder(it.text))
            }
        }
        override val answers: List<QuestionState.MultipleChoice.Answer>
            get() = _answers

        private var answersError: String? by mutableStateOf(null)

        override fun changePrompt(text: String) {
            if (state.screenIsBusy) {
                return
            }
            prompt = prompt.copy(text = text, dirty = true)
            validatePrompt()
        }

        override fun addAnswer() {
            if (state.screenIsBusy) {
                return
            }

            _answers += AnswerHolder()
        }

        override fun changeCorrectAnswer(
            index: Int,
        ) {
            if (state.screenIsBusy) {
                return
            }

            if (index !in _answers.indices) {
                return
            }

            correctAnswer = index
        }

        override fun removeAnswer(
            index: Int,
        ) {
            if (state.screenIsBusy) {
                return
            }

            _answers.removeAt(index)

            val currentCorrectAnswer = correctAnswer
            if (currentCorrectAnswer != null && index == currentCorrectAnswer) {
                correctAnswer = max(0, currentCorrectAnswer - 1)
            }
        }

        override suspend fun revalidate() {
            validatePrompt()
            validateAnswersSize()
            validateCorrectAnswer()
            supervisorScope {
                _answers.map {
                    async { it.revalidate() }
                }.awaitAll()
            }
        }

        override suspend fun hasErrors(): Boolean =
            error != null || answersError != null || correctAnswerError != null ||
                    prompt.error != null || _answers.anyAsync { it.hasErrors() }

        private fun validatePrompt() {
            prompt = if (prompt.text.isEmpty()) {
                prompt.copy(error = "Empty question text", dirty = true)
            } else {
                prompt.copy(error = null)
            }
        }

        private fun validateAnswersSize() {
            answersError = if (answers.size < 2) {
                "Must add at least two answers."
            } else null
        }

        private fun validateCorrectAnswer() {
            correctAnswerError = when (correctAnswer) {
                null -> "Select a correct answer"
                !in answers.indices -> "Correct answer must be within the number of answers."
                else -> null
            }
        }

        private inner class AnswerHolder(text: String = "") : QuestionState.MultipleChoice.Answer {
            override var text by mutableStateOf(TextFieldState(text = text))

            override fun changeText(value: String) {
                if (state.screenIsBusy) {
                    return
                }

                text = text.copy(text = value, dirty = true)
                validateText()
            }

            fun revalidate() {
                validateText()
            }

            fun hasErrors(): Boolean = text.error != null

            private fun validateText() {
                text = if (text.text.isBlank()) {
                    text.copy(error = "Error blank", dirty = true)
                } else {
                    text.copy(error = null)
                }
            }
        }
    }

    /**
     * State holder for a FillIn Question.
     */
    private inner class FillInHolder(
        questionText: String = "",
        correctAnswer: String = "",
    ) : QuestionState.FillIn, ValidatedQuestionState {
        override val key: String = UUID.randomUUID().toString()

        override val question: QuestionState = this

        override val data: Question.FillIn
            get() = Question.FillIn(
                text = prompt.text,
                correctAnswer = correctAnswer.text
            )

        override val error: String? = null

        override var prompt by mutableStateOf(TextFieldState(text = questionText))
            private set

        override var correctAnswer by mutableStateOf(TextFieldState(text = correctAnswer))
            private set

        val correctAnswerError: String?
            get() = correctAnswer.error

        override fun changePrompt(text: String) {
            if (state.screenIsBusy) {
                return
            }

            prompt = prompt.copy(text = text, dirty = true)
            validatePrompt()
        }

        override fun changeCorrectAnswer(text: String) {
            if (state.screenIsBusy) {
                return
            }

            correctAnswer = correctAnswer.copy(text = text, dirty = true)
            validateCorrectAnswer()
        }

        override suspend fun revalidate() {
            validatePrompt()
            validateCorrectAnswer()
        }

        override suspend fun hasErrors(): Boolean =
            error != null || prompt.error != null || correctAnswerError != null

        private fun validatePrompt() {
            prompt = if (prompt.text.isBlank()) {
                prompt.copy(error = "Question prompt can't be empty", dirty = true)
            } else {
                prompt.copy(error = null)
            }
        }

        private fun validateCorrectAnswer() {
            correctAnswer = if (correctAnswer.text.isBlank()) {
                correctAnswer.copy(error = "Answer text can't be empty", dirty = true)
            } else {
                correctAnswer.copy(error = null)
            }
        }
    }

    private fun fromQuestion(question: Question): ValidatedQuestionState = when (question) {
        is Question.Empty -> EmptyHolder()
        is Question.FillIn -> fromQuestion(question)
        is Question.MultipleChoice -> fromQuestion(question)
    }

    private fun fromQuestion(question: Question.MultipleChoice): MultipleChoiceHolder =
        MultipleChoiceHolder(
            questionText = question.text,
            correctAnswer = question.correctAnswer,
            answers = question.answers,
        )

    private fun fromQuestion(question: Question.FillIn): FillInHolder = FillInHolder(
        questionText = question.text,
        correctAnswer = question.correctAnswer ?: "",
    )
}