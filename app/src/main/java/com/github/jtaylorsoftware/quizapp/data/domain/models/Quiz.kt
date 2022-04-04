package com.github.jtaylorsoftware.quizapp.data.domain.models

import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizFormDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizListingDto
import com.github.jtaylorsoftware.quizapp.util.toInstant
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A quiz with questions for a user to respond to. This is the data
 * used when creating and editing quizzes.
 */
data class Quiz(
    val id: ObjectId = ObjectId(),

    /**
     * The time this Quiz was created.
     */
    val date: Instant = Instant.now(),

    /**
     * The id of the user that created this Quiz.
     */
    val createdBy: ObjectId = ObjectId(),
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
    val results: List<ObjectId> = emptyList(),
) {
    companion object {
        fun fromDto(dto: QuizDto): Quiz =
            Quiz(
                id = ObjectId(dto.id),
                date = dto.date.toInstant() ?: Instant.now(),
                createdBy = ObjectId(dto.user),
                title = dto.title,
                expiration = dto.expiration.toInstant() ?: Instant.now(),
                isPublic = dto.isPublic,
                allowedUsers = dto.allowedUsers.toList(),
                questions = dto.questions.map { Question.fromDto(it) },
                results = dto.results.map { ObjectId(it) },
            )

    }
}

/**
 * A simplified view of a [Quiz].
 */
data class QuizListing(
    val id: ObjectId = ObjectId(),
    val date: Instant = Instant.now(),

    /**
     * The id of the user that created the [Quiz] associated with this listing.
     */
    val createdBy: ObjectId = ObjectId(),

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
) {
    companion object {
        fun fromDto(dto: QuizListingDto) = QuizListing(
            id = ObjectId(dto.id),
            date = dto.date.toInstant() ?: Instant.now(),
            createdBy = ObjectId(dto.user),
            title = dto.title,
            expiration = dto.expiration.toInstant() ?: Instant.now().plus(1, ChronoUnit.DAYS),
            isPublic = dto.isPublic,
            resultsCount = dto.resultsCount,
            questionCount = dto.questionCount
        )

        fun fromEntity(entity: QuizListingEntity) = QuizListing(
            id = ObjectId(entity.id),
            date = entity.date.toInstant() ?: Instant.now(),
            createdBy = ObjectId(entity.user),
            title = entity.title,
            expiration = entity.expiration.toInstant() ?: Instant.now().plus(1, ChronoUnit.DAYS),
            isPublic = entity.isPublic,
            resultsCount = entity.resultsCount,
            questionCount = entity.questionCount
        )
    }
}

/**
 * A [Quiz] with answers and other owner-restricted data taken out.
 * This is given to users to respond to.
 */
data class QuizForm(
    val id: ObjectId = ObjectId(),
    val date: Instant = Instant.now(),

    /**
     * The name of the user that created the [Quiz] associated with this listing.
     */
    val createdBy: String = "",

    val title: String = "",
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    val questions: List<Question> = emptyList(),
) {
    companion object {
        fun fromDto(dto: QuizFormDto): QuizForm = QuizForm(
            id = ObjectId(dto.id),
            date = requireNotNull(dto.date.toInstant()),
            createdBy = dto.username,
            title = dto.title,
            expiration = requireNotNull(dto.expiration.toInstant()),
            questions = dto.questions.map { Question.fromDto(it) }
        )
    }
}

/**
 * A user's response to a question of a [QuizForm]. This is all the data
 * necessary for the user to respond to a quiz question.
 */
sealed interface QuestionResponse {
    val type: QuestionType

    data class MultipleChoice(
        /**
         * The index of the answer the user has chosen as their response-answer.
         */
        val choice: Int = 0
    ) : QuestionResponse {
        override val type: QuestionType = QuestionType.MultipleChoice
    }

    data class FillIn(
        /**
         * The user's input for a fill-in question.
         */
        val answer: String = ""
    ) : QuestionResponse {
        override val type: QuestionType = QuestionType.FillIn
    }
}

/**
 * A single question for a [Quiz] or [QuizForm]. It provides a prompt
 * and the answer choices.
 */
sealed interface Question {
    val type: QuestionType

    /**
     * The prompt displayed for this Question ("what to answer").
     */
    val text: String

    /**
     * A typeless placeholder Question.
     */
    object Empty : Question {
        override val type: QuestionType = QuestionType.Empty
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
        override val type: QuestionType = QuestionType.MultipleChoice

        /**
         * The body of one choice ("answer").
         */
        data class Answer(
            /**
             * The answer body text.
             */
            val text: String
        ) {
            companion object {
                fun fromDto(dto: QuestionDto.MultipleChoice.Answer) = Answer(
                    text = dto.text
                )
            }
        }
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
        override val type: QuestionType = QuestionType.FillIn
    }

    companion object {
        fun fromDto(dto: QuestionDto): Question = when (dto) {
            is QuestionDto.MultipleChoice -> MultipleChoice(
                text = dto.text,
                correctAnswer = dto.correctAnswer,
                answers = dto.answers.map { MultipleChoice.Answer.fromDto(it) }
            )
            is QuestionDto.FillIn -> FillIn(
                text = dto.text,
                correctAnswer = dto.correctAnswer
            )
        }
    }
}