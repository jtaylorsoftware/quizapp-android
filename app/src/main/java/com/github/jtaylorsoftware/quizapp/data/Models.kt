package com.github.jtaylorsoftware.quizapp.data

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A quiz with questions for a user to respond to. This is the data
 * used when creating and editing quizzes.
 */
data class Quiz(
    val id: String = "",

    /**
     * The time this Quiz was created.
     */
    val date: Instant = Instant.now(),

    /**
     * The id of the user that created this Quiz.
     */
    val createdBy: String = "",
    val title: String = "",

    /**
     * The time this Quiz will expire, after which the server will not accept user responses.
     */
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),

    /**
     * Flag controlling access to the Quiz. If true, anyone with the link can access. Otherwise,
     * only uses in [allowedUsers] can access.
     */
    val isPublic: Boolean = false,

    /**
     * The usernames of the users that are allowed to access this Quiz. (They will be transformed
     * into ids on the server.)
     */
    val allowedUsers: List<String> = emptyList(),

    val questions: List<Question> = emptyList(),

    /**
     * The ids of the responses to this Quiz.
     */
    val results: List<String> = emptyList(),
)

/**
 * A simplified view of a [Quiz].
 */
data class QuizListing(
    val id: String = "",
    val date: Instant = Instant.now(),

    /**
     * The name of the user that created the [Quiz] associated with this listing.
     */
    val createdBy: String = "",

    val title: String = "",
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    val isPublic: Boolean = false,

    /**
     * The number of responses to this Quiz.
     */
    val resultsCount: Int = 0,

    /**
     * The number of questions in this Quiz.
     */
    val questionCount: Int = 0,
)

/**
 * A [Quiz] with answers and other owner-restricted data taken out.
 * This is given to users to respond to.
 */
data class QuizForm(
    val id: String = "",
    val date: Instant = Instant.now(),

    /**
     * The name of the user that created the [Quiz] associated with this listing.
     */
    val createdBy: String = "",

    val title: String = "",
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    val questions: List<Question> = emptyList(),
)

/**
 * A user's response to a question of a [QuizForm]. This is all the data
 * necessary for the user to respond to a quiz question.
 */
sealed interface Response {
    val type: Question.Type

    data class MultipleChoice(
        /**
         * The index of the answer the user has chosen as their response-answer.
         */
        val choice: Int = 0
    ): Response {
        override val type: Question.Type = Question.Type.MultipleChoice
    }

    data class FillIn(
        /**
         * The user's input for a fill-in question.
         */
        val answer: String = ""
    ): Response {
        override val type: Question.Type = Question.Type.FillIn
    }
}

/**
 * A single question for a [Quiz] or [QuizForm]. It provides a prompt
 * and the answer choices.
 */
sealed interface Question {
    val type: Type

    /**
     * The prompt displayed for this Question ("what to answer").
     */
    val text: String

    /**
     * The type of Question, which indicates what its body will contain.
     */
    enum class Type {
        /**
         * A multiple-choice question with multiple answers that the user will
         * select one of.
         */
        MultipleChoice,

        /**
         * A fill-in-the-blank question with one exact answer.
         */
        FillIn,

        /**
         * No body type. This Type is used as a placeholder when creating a [Quiz].
         */
        Empty
    }

    /**
     * A typeless placeholder Question.
     */
    object Empty : Question {
        override val type: Type = Type.Empty
        override val text: String = ""
    }

    /**
     * A multiple-choice Question.
     */
    data class MultipleChoice(
        override val text: String = "",

        val correctAnswer: Int? = 0,

        /**
         * The list of choices ("answers").
         */
        val answers: List<Answer> = emptyList(),
    ) : Question {
        override val type: Type = Type.MultipleChoice

        /**
         * The body of one choice ("answer").
         */
        data class Answer(
            /**
             * The answer body text.
             */
            val text: String
        )
    }

    /**
     * A fill-in-the-blank Question.
     */
    data class FillIn(
        override val text: String = "",
        /**
         * The exact answer to this Question.
         */
        val correctAnswer: String? = "",
    ) : Question {
        override val type: Type = Type.FillIn
    }
}

/**
 * A graded user response to a Quiz.
 */
data class QuizResult(
    val id: String = "",
    /**
     * The date this response was received and graded.
     */
    val date: Instant = Instant.now(),
    val userId: String = "",
    val username: String = "",

    /**
     * The id of the [Quiz] for this result.
     */
    val quiz: String = "",

    val quizTitle: String = "",

    /**
     * The name of the user that created the associated [Quiz].
     */
    val createdBy: String = "",

    /**
     * The list of graded user responses ("answers").
     */
    val answers: List<GradedAnswer> = emptyList(),

    /**
     * The user's total score, as a percentage (value in `[0.0,1.0]`).
     */
    val score: Float = 0f,
)

/**
 * A simplified view of a [QuizResult].
 */
data class QuizResultListing(
    val id: String = "",
    val date: Instant = Instant.now(),
    val userId: String = "",
    val username: String = "",
    val quiz: String = "",
    val quizTitle: String = "",
    val createdBy: String = "",
    val score: Float = 0f,
)

/**
 * A graded response to a [Question].
 */
sealed interface GradedAnswer {
    val type: Question.Type

    /**
     * True if the response is correct (full score for this question).
     */
    val isCorrect: Boolean

    data class MultipleChoice(
        override val isCorrect: Boolean = false,

        /**
         * The index of the answer the user has chosen as their response-answer.
         */
        val choice: Int = 0,

        /**
         * The actual correct choice for the question.
         */
        val correctAnswer: Int? = null,
    ) : GradedAnswer {
        override val type: Question.Type = Question.Type.MultipleChoice
    }

    data class FillIn(
        override val isCorrect: Boolean = false,

        /**
         * The user's input for a fill-in question.
         */
        val answer: String = "",

        /**
         * The actual correct answer to the question.
         */
        val correctAnswer: String? = null,
    ) : GradedAnswer {
        override val type: Question.Type = Question.Type.FillIn
    }
}

/**
 * Data for the signed-in user.
 */
data class User(
    val id: String = "",
    val date: Instant = Instant.now(),
    val username: String = "",
    val email: String = "",

    /**
     * List of the ids of the user's created quizzes.
     */
    val quizzes: List<String> = emptyList(),

    /**
     * List of the ids of the user's quiz responses.
     */
    val results: List<String> = emptyList(),
)