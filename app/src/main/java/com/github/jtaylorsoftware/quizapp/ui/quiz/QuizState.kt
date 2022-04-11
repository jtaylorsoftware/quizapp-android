package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.*
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.max

/**
 * Contains simple header data for a Quiz, such as expiration and title.
 * Also includes errors for each applicable field.
 */
interface QuizState {
    val title: TextFieldState
    fun setTitle(value: String)

    var expiration: Instant
    val expirationError: String?

    var isPublic: Boolean

    /**
     * A comma-separated string of allowed usernames.
     */
    var allowedUsers: String
    val allowedUsersError: String?

    val questions: List<QuestionState>

    /**
     * "Overall" error for questions (such as too few questions)
     */
    val questionsError: String?

    fun addQuestion()
    fun changeQuestionType(index: Int, newType: QuestionType)
    fun changeQuestion(index: Int, newState: QuestionState)
    fun deleteQuestion(index: Int)
}

/**
 * A [QuizState] suitable for Previews.
 */
data class PreviewQuizState(
    override val title: TextFieldState = TextFieldState(),
    override var expiration: Instant = Instant.now(),
    override val expirationError: String? = "Errors: foo bar",
    override var isPublic: Boolean = false,
    override var allowedUsers: String = "username1,username2",
    override val allowedUsersError: String? = "Errors: foo bar",
    override val questions: List<QuestionState> = emptyList(),
    override val questionsError: String? = null,
) : QuizState {
    override fun setTitle(value: String) {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }

    override fun addQuestion() {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }

    override fun changeQuestionType(index: Int, newType: QuestionType) {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }

    override fun changeQuestion(index: Int, newState: QuestionState) {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }

    override fun deleteQuestion(index: Int) {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }
}

/**
 * A [QuizState] where setter methods are delegated to [MutableState]
 * instances.
 *
 * Although it could be used with actual screens, it's more suited
 * to tests or quick experimentation where creating a ViewModel isn't
 * desired or possible. This is because of an expensive upfront
 * allocation for initializing the internal list of questions.
 *
 * In tests, create an instance outside of `composeTestRule.setContent`
 *
 * Inside a `Composable`, you can use it in `remember { }`.
 */
class QuizStateHolder(
    title: String = "",
    expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    isPublic: Boolean = true,
    allowedUsers: String = "",
    override val expirationError: String? = null,
    override val allowedUsersError: String? = null,
    questions: List<QuestionState> = emptyList(),
    override val questionsError: String? = null,
) : QuizState {
    private var _title by mutableStateOf(TextFieldState(text = title))
    override val title: TextFieldState
        get() = _title

    override fun setTitle(value: String) {
        _title = _title.copy(text = value)
    }

    override var expiration by mutableStateOf(expiration)
    override var isPublic by mutableStateOf(isPublic)
    override var allowedUsers by mutableStateOf(allowedUsers)

    private var _questions = mutableStateListOf(*questions.toTypedArray())
    override val questions: List<QuestionState>
        get() = _questions

    override fun addQuestion() {
        _questions.add(QuestionState.Empty())
    }

    override fun changeQuestionType(index: Int, newType: QuestionType) {
        _questions[index] = when (newType) {
            QuestionType.Empty -> throw IllegalArgumentException("Cannot change QuestionType to Empty")
            QuestionType.FillIn -> QuestionState.FillIn()
            QuestionType.MultipleChoice -> QuestionState.MultipleChoice()
        }
    }

    override fun changeQuestion(index: Int, newState: QuestionState) {
        _questions[index] = newState
    }

    override fun deleteQuestion(index: Int) {
        _questions.removeAt(index)
    }
}

/**
 * Stores a [Question] and its errors.
 */
sealed interface QuestionState {
    val data: Question

    /**
     * Overall error for the question (such as if a MultipleChoice question has too few answers).
     */
    val error: String?

    /**
     * The error for the question text/prompt.
     */
    val questionTextError: String?

    /**
     * The error for the correct answer to this Question.
     * Some `QuestionType` may use a default correctAnswer,
     * or may have no validation constraints, so there may never be an error.
     */
    val correctAnswerError: String?

    /**
     * Stable, unique key for use in lazy lists.
     */
    val key: String

    /**
     * Changes the prompt/text of the Question.
     *
     * @return A copy of this QuestionState, with an updated copy of [data].
     */
    fun changeText(text: String): QuestionState

    /**
     * State holder for a Question that has not had its type selected.
     */
    data class Empty(override val error: String? = null) : QuestionState {
        override val data: Question.Empty = Question.Empty
        override val questionTextError: String? = null
        override val correctAnswerError: String? = null
        override val key: String = UUID.randomUUID().toString()

        override fun changeText(text: String): QuestionState {
            throw IllegalStateException("Cannot modify Question.Empty")
        }
    }

    /**
     * State holder for a MultipleChoice Question.
     * @param answerErrors The errors for the answers of this question, with `null` indicating no error.
     *                     Will always be the same size as `question.answers`.
     */
    data class MultipleChoice(
        override val data: Question.MultipleChoice = Question.MultipleChoice(),
        override val error: String? = null,
        override val questionTextError: String? = null,
        override val correctAnswerError: String? = null,
        val answerErrors: List<String?> = emptyList(),
        private val _key: UUID = UUID.randomUUID()
    ) : QuestionState {
        override val key: String = _key.toString()

        override fun changeText(text: String): QuestionState {
            return copy(data = data.copy(text = text))
        }

        /**
         * Adds an answer to a [QuestionState.MultipleChoice] while maintaining any variants.
         *
         * @return A copy of [QuestionState] updated with added answer and correct invariants.
         */
        fun addAnswer(): MultipleChoice {
            val newAnswers = data.answers + Question.MultipleChoice.Answer("")
            val newErrors = answerErrors + null
            return copy(data = data.copy(answers = newAnswers), answerErrors = newErrors)
        }

        /**
         * Changes the correct answer of a [QuestionState.MultipleChoice].
         *
         * @return A copy of [QuestionState] updated with changes.
         */
        fun changeCorrectAnswer(
            index: Int,
        ): MultipleChoice {
            return copy(data = data.copy(correctAnswer = index))
        }

        /**
         * Deletes an answer of a [QuestionState.MultipleChoice] while maintaining any invariants.
         *
         * @return A copy of [QuestionState] updated with changes.
         */
        fun changeAnswer(
            index: Int,
            answer: Question.MultipleChoice.Answer
        ): MultipleChoice {
            val newAnswers = data.answers.toMutableList().apply { this[index] = answer.copy() }
            val newErrors = answerErrors.toMutableList()
            return copy(data = data.copy(answers = newAnswers), answerErrors = newErrors)
        }

        /**
         * Deletes an answer of a [QuestionState.MultipleChoice] while maintaining any invariants.
         *
         * @return A copy of [QuestionState] with removed answer.
         */
        fun removeAnswer(
            index: Int,
        ): MultipleChoice {
            var correctAnswer = data.correctAnswer
            if (index == correctAnswer) {
                correctAnswer = max(0, correctAnswer - 1)
            }
            val newAnswers = data.answers.toMutableList().apply { removeAt(index) }
            val newErrors = answerErrors.toMutableList().apply { removeAt(index) }
            return copy(
                data = data.copy(correctAnswer = correctAnswer, answers = newAnswers),
                answerErrors = newErrors
            )
        }
    }

    /**
     * State holder for a FillIn Question.
     */
    data class FillIn(
        override val data: Question.FillIn = Question.FillIn(),
        override val error: String? = null,
        override val questionTextError: String? = null,
        override val correctAnswerError: String? = null,
        private val _key: UUID = UUID.randomUUID()
    ) : QuestionState {
        override val key: String = _key.toString()

        override fun changeText(text: String): QuestionState {
            return copy(data = data.copy(text = text))
        }

        /**
         * Changes the correct answer text for this [FillIn] question.
         *
         * @return A copy with the updated correct answer text.
         */
        fun changeCorrectAnswer(correctAnswer: String): FillIn {
            return copy(
                data = data.copy(correctAnswer = correctAnswer),
                questionTextError = null
            )
        }
    }

    companion object {
        fun fromQuestion(question: Question): QuestionState = when (question) {
            is Question.Empty -> Empty()
            is Question.FillIn -> FillIn(data = question)
            is Question.MultipleChoice -> MultipleChoice(
                data = question,
                answerErrors = List(question.answers.size) { null })
        }
    }
}