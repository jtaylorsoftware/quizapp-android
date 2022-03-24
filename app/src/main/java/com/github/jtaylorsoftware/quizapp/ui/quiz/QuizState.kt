package com.github.jtaylorsoftware.quizapp.ui.quiz

import com.github.jtaylorsoftware.quizapp.data.Question
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Contains simple header data for a Quiz, such as expiration and title.
 * Also includes errors for each applicable field.
 */
data class QuizState(
    val title: TextFieldState = TextFieldState(),
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    val expirationError: String? = null,
    val isPublic: Boolean = true,

    /**
     * A comma-separated string of allowed usernames.
     */
    val allowedUsers: String = "",

    val allowedUsersError: String? = null,
)

/**
 * Stores a [Question] and its errors.
 */
sealed interface QuestionState {
    val question: Question

    /**
     * The error for the question text/prompt.
     */
    val questionTextError: String?

    /**
     * The error for the correct answer to this Question.
     * Some [Question Types][Question.Type] may use a default correctAnswer,
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
     * @return A copy of this QuestionState, with an updated copy of [question].
     */
    fun changeText(text: String): QuestionState

    /**
     * State holder for a Question that has not had its type selected.
     */
    class Empty : QuestionState {
        override val question: Question.Empty = Question.Empty
        override val questionTextError: String? = null
        override val correctAnswerError: String? = null
        private val _key: UUID = UUID.randomUUID()
        override val key: String = _key.toString()

        override fun changeText(text: String): QuestionState {
            throw IllegalStateException("Cannot modify Question.Empty")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Empty

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }

    /**
     * State holder for a MultipleChoice Question.
     * @param answerErrors The errors for the answers of this question, with `null` indicating no error.
     *                     Will always be the same size as `question.answers`.
     */
    data class MultipleChoice(
        override val question: Question.MultipleChoice = Question.MultipleChoice(),
        override val questionTextError: String? = null,
        override val correctAnswerError: String? = null,
        val answerErrors: List<String?> = emptyList(),
        private val _key: UUID = UUID.randomUUID()
    ) : QuestionState {
        override val key: String = _key.toString()

        override fun changeText(text: String): QuestionState {
            return copy(question = question.copy(text = text), questionTextError = null)
        }
    }

    /**
     * State holder for a FillIn Question.
     */
    data class FillIn(
        override val question: Question.FillIn = Question.FillIn(),
        override val questionTextError: String? = null,
        override val correctAnswerError: String? = null,
        private val _key: UUID = UUID.randomUUID()
    ) : QuestionState {
        override val key: String = _key.toString()

        override fun changeText(text: String): QuestionState {
            return copy(question = question.copy(text = text), questionTextError = null)
        }
    }
}

/**
 * Utility function to add an answer to a [QuestionState.MultipleChoice] while maintaining any variants.
 *
 * @return A copy of [questionState] updated with added answer and correct invariants.
 */
fun QuestionState.MultipleChoice.addAnswer(): QuestionState.MultipleChoice {
    val newAnswers = question.answers + Question.MultipleChoice.Answer("")
    val newErrors = answerErrors + null
    return copy(question = question.copy(answers = newAnswers), answerErrors = newErrors)
}

/**
 * Utility function to change the correct answer of a [QuestionState.MultipleChoice].
 *
 * @return A copy of [questionState] updated with changes.
 */
fun QuestionState.MultipleChoice.changeCorrectAnswer(
    index: Int,
): QuestionState.MultipleChoice {
    return copy(question = question.copy(correctAnswer = index))
}

/**
 * Utility function to modify an answer of a [QuestionState.MultipleChoice] while maintaining any variants.
 *
 * @return A copy of [questionState] updated with changes.
 */
fun QuestionState.MultipleChoice.changeAnswer(
    index: Int,
    answer: Question.MultipleChoice.Answer
): QuestionState.MultipleChoice {
    val newAnswers = question.answers.toMutableList().apply { this[index] = answer.copy() }
    val newErrors = answerErrors.toMutableList().apply { this[index] = null }
    return copy(question = question.copy(answers = newAnswers), answerErrors = newErrors)
}

/**
 * Utility function to delete an answer of a [QuestionState.MultipleChoice] while maintaining any variants.
 *
 * @return A copy of [questionState] removed answer.
 */
fun QuestionState.MultipleChoice.removeAnswer(
    index: Int,
): QuestionState.MultipleChoice {
    val newAnswers = question.answers.toMutableList().apply { removeAt(index) }
    val newErrors = answerErrors.toMutableList().apply { removeAt(index) }
    return copy(question = question.copy(answers = newAnswers), answerErrors = newErrors)
}
