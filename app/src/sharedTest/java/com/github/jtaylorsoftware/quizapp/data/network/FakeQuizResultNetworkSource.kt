package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.*
import java.time.Instant
import java.util.*

class FakeQuizResultNetworkSource(
    results: List<QuizResultDto> = emptyList()
) : QuizResultNetworkSource, FallibleNetworkSource() {
    private val cache = results.toMutableList()

    override suspend fun getForQuizByUser(
        quiz: String,
        user: String
    ): NetworkResult<QuizResultDto> =
        failOnNext ?: cache.firstOrNull { it.quiz == quiz && it.user == user }?.let {
            NetworkResult.success(it)
        } ?: NetworkResult.HttpError(404)

    override suspend fun getAllForQuiz(quiz: String): NetworkResult<QuizResultsForQuizDto> =
        failOnNext ?: NetworkResult.success(QuizResultsForQuizDto(cache.filter { it.quiz == quiz }))

    override suspend fun getListingForQuizByUser(
        quiz: String,
        user: String
    ): NetworkResult<QuizResultListingDto> =
        failOnNext ?: cache.firstOrNull { it.quiz == quiz && it.user == user }?.let {
            NetworkResult.success(it.asListing())
        }
        ?: NetworkResult.HttpError(404)

    override suspend fun getAllListingForQuiz(quiz: String): NetworkResult<QuizResultListingsForQuizDto> =
        failOnNext ?: NetworkResult.success(QuizResultListingsForQuizDto(
            cache.filter { it.quiz == quiz }.map { it.asListing() }
        ))

    override suspend fun createResultForQuiz(
        responses: QuizFormResponsesDto,
        quiz: String
    ): NetworkResult<ObjectIdResponse> = failOnNext ?: run {
        val result = QuizResultDto(
            id = UUID.randomUUID().toString(),
            date = Instant.now().toString(),
            user = UUID.randomUUID().toString(),
            username = "username",
            quiz = quiz,
            score = 1.0f,
            quizTitle = "Quiz Title",
            createdBy = "createdBy",
            answers = responses.answers.map {
                when (it) {
                    is QuestionResponseDto.MultipleChoice -> GradedAnswerDto.MultipleChoice(
                        true,
                        it.choice,
                        it.choice
                    )
                    is QuestionResponseDto.FillIn -> GradedAnswerDto.FillIn(
                        true,
                        it.answer,
                        it.answer
                    )
                }
            }
        )
        cache.add(result)
        NetworkResult.success(ObjectIdResponse(result.id))
    }
}

fun QuizResultDto.asListing() = QuizResultListingDto(
    id = id,
    date = date,
    user = user,
    username = username,
    quiz = quiz,
    score = score,
    quizTitle = quizTitle,
    createdBy = createdBy
)