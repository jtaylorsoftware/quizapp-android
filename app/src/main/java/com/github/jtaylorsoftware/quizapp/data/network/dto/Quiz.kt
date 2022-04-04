package com.github.jtaylorsoftware.quizapp.data.network.dto

import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant
import java.time.temporal.ChronoUnit

@JsonClass(generateAdapter = true)
data class QuizDto(
    @Json(name = "_id")
    val id: String,
    val date: String = Instant.now().toString(),
    /** ObjectId **/
    val user: String = "",
    val title: String = "",
    val expiration: String = Instant.now().plus(1, ChronoUnit.DAYS).toString(),
    val isPublic: Boolean = true,
    val allowedUsers: List<String> = emptyList(),
    val questions: List<QuestionDto> = emptyList(),
    val results: List<String> = emptyList(),
) {
    companion object {
        fun fromDomain(domain: Quiz) = QuizDto(
            id = domain.id.value,
            date = domain.date.toString(),
            user = domain.createdBy.value,
            title = domain.title,
            expiration = domain.expiration.toString(),
            isPublic = domain.isPublic,
            allowedUsers = domain.allowedUsers.toList(),
            questions = domain.questions.map { QuestionDto.fromDomain(it) },
            results = domain.results.map { it.value }
        )
    }
}

@JsonClass(generateAdapter = true)
data class QuizListingDto(
    @Json(name = "_id")
    val id: String,
    val date: String = Instant.now().toString(),
    /** ObjectId **/
    val user: String = "",
    val title: String = "",
    val expiration: String = Instant.now().plus(1, ChronoUnit.DAYS).toString(),
    val isPublic: Boolean = true,
    val resultsCount: Int = 0,
    val questionCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class QuizFormDto(
    @Json(name = "_id")
    val id: String = "",
    val date: String = Instant.now().toString(),
    @Json(name = "user")
    val username: String = "",
    val title: String = "",
    val expiration: String = Instant.now().plus(1, ChronoUnit.DAYS).toString(),
    val questions: List<QuestionDto>
)

sealed interface QuestionDto {
    val type: String
    val text: String

    @JsonClass(generateAdapter = true)
    data class MultipleChoice(
        override val text: String = "",
        val correctAnswer: Int? = null,
        val answers: List<Answer> = emptyList()
    ) : QuestionDto {
        override val type: String = QuestionType.MultipleChoice.name

        @JsonClass(generateAdapter = true)
        data class Answer(
            val text: String = ""
        ) {
            companion object {
                fun fromDomain(domain: Question.MultipleChoice.Answer) = Answer(
                    text = domain.text,
                )
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class FillIn(
        override val text: String = "",
        val correctAnswer: String? = null,
    ) : QuestionDto {
        override val type: String = QuestionType.FillIn.name
    }

    companion object {
        fun fromDomain(domain: Question) = when (domain) {
            is Question.MultipleChoice -> MultipleChoice(
                text = domain.text,
                correctAnswer = domain.correctAnswer,
                answers = domain.answers.map { MultipleChoice.Answer.fromDomain(it) }
            )
            is Question.FillIn -> FillIn(
                text = domain.text,
                correctAnswer = domain.correctAnswer
            )
            else -> throw IllegalArgumentException("Cannot convert Question.Empty to QuestionDto")
        }
    }
}