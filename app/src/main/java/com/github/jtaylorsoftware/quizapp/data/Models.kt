package com.github.jtaylorsoftware.quizapp.data

import java.time.Instant
import java.time.temporal.ChronoUnit

data class Quiz(
    val id: String = "",
    val date: Instant = Instant.now(),
    // id
    val createdBy: String = "",
    val title: String = "",
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    val isPublic: Boolean = false,
    // names
    val allowedUsers: List<String> = emptyList(),
    // ids
    val questions: List<Question> = emptyList(),
    // ids
    val results: List<String> = emptyList(),
)

data class QuizListing(
    val id: String = "",
    val date: Instant = Instant.now(),
    // name (signed in user)
    val createdBy: String = "",
    val title: String = "",
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    val isPublic: Boolean = false,
    val resultsCount: Int = 0,
    val questionCount: Int = 0,
)

data class QuizForm(
    val id: String = "",
    val date: Instant = Instant.now(),
    // name
    val createdBy: String = "",
    val title: String = "",
    val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    val questions: List<Question> = emptyList(),
)

sealed interface Response {
    val type: Question.Type

    data class MultipleChoice(
        val choice: Int = 0
    ): Response {
        override val type: Question.Type = Question.Type.MultipleChoice
    }

    data class FillIn(
        val answer: String = ""
    ): Response {
        override val type: Question.Type = Question.Type.FillIn
    }
}

sealed interface Question {
    val type: Type
    val text: String

    enum class Type {
        MultipleChoice, FillIn
    }

    data class MultipleChoice(
        override val text: String = "",
        val correctAnswer: Int? = 0,
        val answers: List<Answer> = emptyList(),
    ) : Question {
        override val type: Type = Type.MultipleChoice

        data class Answer(val text: String)
    }

    data class FillIn(
        override val text: String = "",
        val correctAnswer: String? = "",
    ) : Question {
        override val type: Type = Type.FillIn
    }
}

data class QuizResult(
    val id: String = "",
    val date: Instant = Instant.now(),
    val userId: String = "",
    val username: String = "",
    // id
    val quiz: String = "",
    val quizTitle: String = "",
    // name
    val createdBy: String = "",
    val answers: List<GradedAnswer> = emptyList(),
    val score: Float = 0f,
)

data class QuizResultListing(
    val id: String = "",
    val date: Instant = Instant.now(),
    val userId: String = "",
    val username: String = "",
    // id
    val quiz: String = "",
    val quizTitle: String = "",
    // name
    val createdBy: String = "",
    val score: Float = 0f,
)

sealed interface GradedAnswer {
    val type: Question.Type
    val isCorrect: Boolean

    data class MultipleChoice(
        override val isCorrect: Boolean = false,
        val choice: Int = 0,
        val correctAnswer: Int? = null,
    ) : GradedAnswer {
        override val type: Question.Type = Question.Type.MultipleChoice
    }

    data class FillIn(
        override val isCorrect: Boolean = false,
        val answer: String = "",
        val correctAnswer: String? = null,
    ) : GradedAnswer {
        override val type: Question.Type = Question.Type.FillIn
    }
}

data class User(
    val id: String = "",
    val date: Instant = Instant.now(),
    val username: String = "",
    val email: String = "",
    // ids
    val quizzes: List<String> = emptyList(),
    // ids
    val results: List<String> = emptyList(),
)