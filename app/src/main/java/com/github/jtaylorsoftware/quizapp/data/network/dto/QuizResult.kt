package com.github.jtaylorsoftware.quizapp.data.network.dto

import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class QuizResultDto(
    @Json(name = "_id")
    val id: String = "",
    val date: String = Instant.now().toString(),
    /** ObjectId **/
    val user: String = "",
    val username: String = "",
    /** ObjectId **/
    val quiz: String = "",
    val score: Float = 0.0f,
    val quizTitle: String = "",
    @Json(name = "ownerUsername")
    val createdBy: String = "",
    val answers: List<GradedAnswerDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QuizResultsForQuizDto(
    val results: List<QuizResultDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QuizResultListingDto(
    @Json(name = "_id")
    val id: String = "",
    val date: String = Instant.now().toString(),
    /** ObjectId **/
    val user: String = "",
    val username: String = "",
    /** ObjectId **/
    val quiz: String = "",
    val score: Float = 0.0f,
    val quizTitle: String = "",
    @Json(name = "ownerUsername")
    val createdBy: String = "",
)

@JsonClass(generateAdapter = true)
data class QuizResultListingsForQuizDto(
    val results: List<QuizResultListingDto> = emptyList()
)

sealed interface GradedAnswerDto {
    val type: String
    val isCorrect: Boolean?

    @JsonClass(generateAdapter = true)
    data class MultipleChoice(
        override val isCorrect: Boolean? = null,
        val choice: Int = 0,
        val correctAnswer: Int? = null,
    ) : GradedAnswerDto {
        override val type: String = QuestionType.MultipleChoice.name
    }

    @JsonClass(generateAdapter = true)
    data class FillIn(
        override val isCorrect: Boolean? = null,
        val answer: String = "",
        val correctAnswer: String? = null,
    ) : GradedAnswerDto {
        override val type: String = QuestionType.FillIn.name
    }
}

@JsonClass(generateAdapter = true)
data class QuizFormResponsesDto(
    val answers: List<QuestionResponseDto>
) {
    companion object {
        fun fromDomain(domain: List<QuestionResponse>) = QuizFormResponsesDto(
            answers = domain.map { QuestionResponseDto.fromDomain(it) }
        )
    }
}

sealed interface QuestionResponseDto {
    val type: String

    @JsonClass(generateAdapter = true)
    data class MultipleChoice(
        val choice: Int = 0,
    ) : QuestionResponseDto {
        override val type: String = QuestionType.MultipleChoice.name
    }

    @JsonClass(generateAdapter = true)
    data class FillIn(
        val answer: String = "",
    ) : QuestionResponseDto {
        override val type: String = QuestionType.FillIn.name
    }

    companion object {
        fun fromDomain(domain: QuestionResponse) = when (domain) {
            is QuestionResponse.MultipleChoice -> MultipleChoice(choice = domain.choice)
            is QuestionResponse.FillIn -> FillIn(answer = domain.answer)
        }
    }
}