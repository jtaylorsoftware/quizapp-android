package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.*
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.max

/**
 * A [QuizState] where setter methods are delegated to [MutableState]
 * instances.
 */
class TestQuizStateHolder(
    title: String = "",
    expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    isPublic: Boolean = true,
    allowedUsers: String = "",
    override val expirationError: String? = null,
    override val allowedUsersError: String? = null,
    questions: List<QuestionState> = emptyList(),
    override val questionsError: String? = null,
) : QuizState {
    override val data: Quiz
        get() = Quiz(
            date = Instant.now(),
            title = title.text,
            expiration = expiration,
            isPublic = isPublic,
            allowedUsers = allowedUsers.split(","),
            questions = questions.map {
                when (it) {
                    is QuestionState.Empty -> it.data
                    is QuestionState.FillIn -> it.data
                    is QuestionState.MultipleChoice -> it.data
                    else -> throw IllegalStateException("Cannot store unknown subclass of ValidatedQuestionState")
                }
            }
        )

    private var _title by mutableStateOf(TextFieldState(text = title))
    override val title: TextFieldState
        get() = _title

    override fun changeTitleText(value: String) {
        _title = _title.copy(text = value)
    }

    override var expiration by mutableStateOf(expiration)
    override var isPublic by mutableStateOf(isPublic)
    override var allowedUsers by mutableStateOf(allowedUsers)

    private var _questions = mutableStateListOf<QuestionState>().apply { addAll(questions) }
    override val questions: List<QuestionState>
        get() = _questions

    override fun addQuestion() {
        _questions.add(TestEmptyHolder())
    }

    override fun changeQuestionType(index: Int, newType: QuestionType) {
        _questions[index] = when (newType) {
            QuestionType.Empty -> throw IllegalArgumentException("Cannot change QuestionType to Empty")
            QuestionType.FillIn -> TestFillInHolder()
            QuestionType.MultipleChoice -> TestMultipleChoiceHolder()
        }
    }

    override fun deleteQuestion(index: Int) {
        _questions.removeAt(index)
    }
}

class TestEmptyHolder(
   error: String? = null
) : QuestionState.Empty(error)

class TestMultipleChoiceHolder(
    error: String? = null,
    questionText: String = "",
    questionTextError: String? = null,
    correctAnswer: Int? = null,
    correctAnswerError: String? = null,
    answers: List<Question.MultipleChoice.Answer> = emptyList(),
    answerErrors: List<String?> = answers.map { null },
) : QuestionState.MultipleChoice {
    override val key: String = UUID.randomUUID().toString()

    override val data: Question.MultipleChoice
        get() = Question.MultipleChoice(
            text = prompt.text,
            correctAnswer = correctAnswer,
            answers = _answers.map { Question.MultipleChoice.Answer(text = it.text.text) }
        )

    override var error by mutableStateOf(error)
        private set

    override var prompt by mutableStateOf(TextFieldState(text = questionText, error = questionTextError))
        private set

    override var correctAnswer by mutableStateOf(correctAnswer)
        private set

    override var correctAnswerError by mutableStateOf(correctAnswerError)
        private set

    private val _answers = mutableStateListOf<TestAnswerHolder>().apply {
        answers.forEach {
            add(TestAnswerHolder(it.text))
        }
    }
    override val answers: List<TestAnswerHolder> = _answers

    init {
        require(answerErrors.isEmpty() || answerErrors.size == answers.size) {
            "Must have same number of answer errors as answers, if errors are given"
        }

        answerErrors.forEachIndexed { i, err ->
            _answers[i].text = _answers[i].text.copy(error = err)
        }
    }

    override fun changePrompt(text: String) {
        prompt = prompt.copy(text = text)
        if (text.isEmpty()) {
            prompt = prompt.copy(error = "Empty question text")
        }
    }

    override fun addAnswer() {
        _answers += TestAnswerHolder()
    }

    override fun changeCorrectAnswer(
        index: Int,
    ) {
        correctAnswer = index
    }

    override fun removeAnswer(
        index: Int,
    ) {
        _answers.removeAt(index)

        val currentCorrectAnswer = correctAnswer
        if (currentCorrectAnswer != null && index == currentCorrectAnswer) {
            correctAnswer = max(0, currentCorrectAnswer - 1)
        }
    }

    class TestAnswerHolder(text: String = "") : QuestionState.MultipleChoice.Answer {
        override var text by mutableStateOf(TextFieldState(text = text))

        override fun changeText(value: String) {
            text = text.copy(text = value)
            if (value.isBlank()) {
                text = text.copy(error = "Error blank")
            }
        }
    }
}

class TestFillInHolder(
    error: String? = null,
    questionText: String = "",
    questionTextError: String? = null,
    correctAnswer: String = "",
    correctAnswerError: String? = null,
) : QuestionState.FillIn {
    override val key: String = UUID.randomUUID().toString()

    override val data: Question.FillIn
        get() = Question.FillIn(
            text = prompt.text,
            correctAnswer = correctAnswer.text
        )

    override var error by mutableStateOf(error)
        private set

    override var prompt by mutableStateOf(TextFieldState(text = questionText, error = questionTextError))
        private set

    override var correctAnswer by mutableStateOf(TextFieldState(text = correctAnswer))
        private set

    var correctAnswerError by mutableStateOf(correctAnswerError)
        private set

    override fun changePrompt(text: String) {
        prompt = prompt.copy(text = text)
        if (text.isBlank()) {
            prompt = prompt.copy(error = "Error blank")
        }
    }

    override fun changeCorrectAnswer(text: String) {
        correctAnswer = correctAnswer.copy(text = text)
        if (text.isBlank()) {
            correctAnswer = correctAnswer.copy(error = "Error blank")
        }
    }
}

fun fromQuestion(question: Question): QuestionState = when (question) {
    is Question.Empty -> TestEmptyHolder()
    is Question.FillIn -> fromQuestion(question)
    is Question.MultipleChoice -> fromQuestion(question)
}

fun fromQuestion(question: Question.MultipleChoice): TestMultipleChoiceHolder =
    TestMultipleChoiceHolder(
        questionText = question.text,
        correctAnswer = question.correctAnswer,
        answers = question.answers,
    )

fun fromQuestion(question: Question.FillIn): TestFillInHolder = TestFillInHolder(
    questionText = question.text,
    correctAnswer = question.correctAnswer ?: "",
)