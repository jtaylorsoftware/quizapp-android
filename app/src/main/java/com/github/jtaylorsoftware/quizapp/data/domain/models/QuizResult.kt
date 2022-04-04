package com.github.jtaylorsoftware.quizapp.data.domain.models

import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.dto.GradedAnswerDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultListingDto
import com.github.jtaylorsoftware.quizapp.util.toInstant
import java.time.Instant

/**
 * A graded user response to a Quiz.
 */
data class QuizResult(
    val id: ObjectId = ObjectId(),
    /**
     * The date this response was received and graded.
     */
    val date: Instant = Instant.now(),
    val user: ObjectId = ObjectId(),
    val username: String = "",

    /**
     * The id of the [Quiz] for this result.
     */
    val quiz: ObjectId = ObjectId(),

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
) {
    companion object {
        fun fromDto(dto: QuizResultDto) = QuizResult(
            id = ObjectId(dto.id),
            date = dto.date.toInstant() ?: Instant.now(),
            user = ObjectId(dto.user),
            username = dto.username,
            quiz = ObjectId(dto.quiz),
            quizTitle = dto.quizTitle,
            createdBy = dto.createdBy,
            answers = dto.answers.map { GradedAnswer.fromDto(it) },
            score = dto.score
        )
    }
}

/**
 * A simplified view of a [QuizResult].
 */
data class QuizResultListing(
    val id: ObjectId = ObjectId(),
    val date: Instant = Instant.now(),
    val user: ObjectId = ObjectId(),
    val username: String = "",
    val quiz: ObjectId = ObjectId(),
    val quizTitle: String = "",
    val createdBy: String = "",
    val score: Float = 0f,
) {
    companion object {
        fun fromDto(dto: QuizResultListingDto) = QuizResultListing(
            id = ObjectId(dto.id),
            date = dto.date.toInstant() ?: Instant.now(),
            user = ObjectId(dto.user),
            username = dto.username,
            quiz = ObjectId(dto.quiz),
            quizTitle = dto.quizTitle,
            createdBy = dto.createdBy,
            score = dto.score
        )

        fun fromEntity(entity: QuizResultListingEntity) = QuizResultListing(
            id = ObjectId(entity.id),
            date = entity.date.toInstant() ?: Instant.now(),
            user = ObjectId(entity.user),
            username = entity.username,
            quiz = ObjectId(entity.quiz),
            quizTitle = entity.quizTitle,
            createdBy = entity.createdBy,
            score = entity.score
        )
    }
}

/**
 * A graded response to a [Question].
 */
sealed interface GradedAnswer {
    val type: QuestionType

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
        override val type: QuestionType = QuestionType.MultipleChoice
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
        override val type: QuestionType = QuestionType.FillIn
    }

    companion object {
        fun fromDto(dto: GradedAnswerDto) = when (dto) {
            is GradedAnswerDto.MultipleChoice -> MultipleChoice(
                isCorrect = requireNotNull(dto.isCorrect),
                choice = dto.choice,
                correctAnswer = requireNotNull(dto.correctAnswer)
            )
            is GradedAnswerDto.FillIn -> FillIn(
                isCorrect = requireNotNull(dto.isCorrect),
                answer = dto.answer,
                correctAnswer = requireNotNull(dto.correctAnswer)
            )
        }
    }
}