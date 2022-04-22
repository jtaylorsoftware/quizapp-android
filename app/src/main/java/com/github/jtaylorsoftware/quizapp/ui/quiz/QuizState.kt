package com.github.jtaylorsoftware.quizapp.ui.quiz

import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import java.time.Instant
import java.util.*

/**
 * Contains simple header data for a Quiz, such as expiration and title.
 * Also includes errors for each applicable field.
 */
interface QuizState {
    val data: Quiz

    val title: TextFieldState
    fun changeTitleText(value: String)

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
    fun deleteQuestion(index: Int)
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

    val prompt: TextFieldState

    /**
     * Stable, unique key for use in lazy lists.
     */
    val key: String

    /**
     * Changes the prompt/text of the Question.
     */
    fun changePrompt(text: String)

    /**
     * State holder for a Question that has not had its type selected.
     */
    abstract class Empty(override val error: String? = null) : QuestionState {
        override val data: Question.Empty = Question.Empty
        override val prompt: TextFieldState = TextFieldState(error = "Select question type")
        override val key: String = UUID.randomUUID().toString()

        override fun changePrompt(text: String) {
            throw IllegalStateException("Cannot modify Question.Empty")
        }
    }

    /**
     * State for a MultipleChoice Question.
     */
    interface MultipleChoice : QuestionState {
        override val data: Question.MultipleChoice

        val answers: List<Answer>

        val correctAnswer: Int?

        val correctAnswerError: String?

        /**
         * Adds an answer to a [QuestionState.MultipleChoice].
         */
        fun addAnswer()

        /**
         * Changes the correct answer of a [QuestionState.MultipleChoice].
         */
        fun changeCorrectAnswer(
            index: Int,
        )

        /**
         * Deletes an answer of a [QuestionState.MultipleChoice].
         */
        fun removeAnswer(
            index: Int,
        )

        interface Answer {
            val text: TextFieldState

            fun changeText(value: String)
        }
    }

    interface FillIn : QuestionState {
        override val data: Question.FillIn

        val correctAnswer: TextFieldState

        /**
         * Changes the correct answer text for this [FillIn] question.
         */
        fun changeCorrectAnswer(text: String)
    }
}

/**
 * A [QuizState] suitable for simple Previews that don't require
 * any functionality, just data.
 */
data class PreviewQuizState(
    override val title: TextFieldState = TextFieldState(),
    override var expiration: Instant = Instant.now(),
    override val expirationError: String? = "foo bar",
    override var isPublic: Boolean = false,
    override var allowedUsers: String = "username1,username2",
    override val allowedUsersError: String? = "foo bar",
    override val questions: List<QuestionState> = emptyList(),
    override val questionsError: String? = null,
) : QuizState {
    override val data: Quiz
        get() = Quiz()

    override fun changeTitleText(value: String) {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }

    override fun addQuestion() {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }

    override fun changeQuestionType(index: Int, newType: QuestionType) {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }

    override fun deleteQuestion(index: Int) {
        throw NotImplementedError("Not supported by PreviewQuizState")
    }
}

/**
 * A `QuestionState.Empty` suitable for simple Previews that don't require
 * any functionality, just data.
 */
class PreviewEmptyState : QuestionState.Empty()

/**
 * A `QuestionState.MultipleChoice` suitable for simple Previews that don't require
 * any functionality, just data.
 */
data class PreviewMultipleChoiceState(
    override val error: String? = null,
    override val correctAnswerError: String? = null,
    override val data: Question.MultipleChoice = Question.MultipleChoice(),
) : QuestionState.MultipleChoice {
    override val key: String = UUID.randomUUID().toString()
    override val answers: List<QuestionState.MultipleChoice.Answer> = data.answers.map {
        PreviewAnswerHolder(TextFieldState(text = it.text))
    }
    override val correctAnswer: Int? = data.correctAnswer
    override val prompt: TextFieldState = TextFieldState(text = data.text)

    override fun changePrompt(text: String) {
        TODO("Not yet implemented")
    }

    override fun addAnswer() {
        TODO("Not yet implemented")
    }

    override fun changeCorrectAnswer(index: Int) {
        TODO("Not yet implemented")
    }

    override fun removeAnswer(index: Int) {
        TODO("Not yet implemented")
    }

    data class PreviewAnswerHolder(override val text: TextFieldState) :
        QuestionState.MultipleChoice.Answer {
        override fun changeText(value: String) {
            TODO("Not yet implemented")
        }
    }
}

/**
 * A `QuestionState.FillIn` suitable for simple Previews that don't require
 * any functionality, just data.
 */
data class PreviewFillInState(
    override val error: String? = null,
    val correctAnswerError: String? = null,
    override val data: Question.FillIn = Question.FillIn(),
) : QuestionState.FillIn {
    override val key: String = UUID.randomUUID().toString()
    override val correctAnswer = TextFieldState(text = data.correctAnswer ?: "", error = correctAnswerError)
    override val prompt: TextFieldState = TextFieldState(text = data.text)

    override fun changePrompt(text: String) {
        TODO("Not yet implemented")
    }

    override fun changeCorrectAnswer(text: String) {
        TODO("Not yet implemented")
    }
}