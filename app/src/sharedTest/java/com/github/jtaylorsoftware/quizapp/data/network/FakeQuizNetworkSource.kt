package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.*
import java.util.*

class FakeQuizNetworkSource(
    quizzes: List<QuizDto> = emptyList()
) : QuizNetworkSource, FallibleNetworkSource() {
    private val cache = quizzes.toMutableList()

    override suspend fun getById(id: String): NetworkResult<QuizDto> =
        failOnNext ?: cache.firstOrNull { it.id == id }?.let {
            NetworkResult.success(it)
        } ?: NetworkResult.HttpError(404)


    override suspend fun getListingById(id: String): NetworkResult<QuizListingDto> =
        failOnNext ?: cache.firstOrNull { it.id == id }?.let {
            NetworkResult.success(it.asListing())
        } ?: NetworkResult.HttpError(404)

    override suspend fun getForm(id: String): NetworkResult<QuizFormDto> =
        failOnNext ?: cache.firstOrNull { it.id == id }?.let {
            NetworkResult.success(it.asForm())
        } ?: NetworkResult.HttpError(404)

    override suspend fun createQuiz(quizDto: QuizDto): NetworkResult<ObjectIdResponse> =
        failOnNext ?: run {
            val quizWithId = quizDto.copy(id = UUID.randomUUID().toString())
            cache.add(quizWithId)
            NetworkResult.success(ObjectIdResponse(quizWithId.id))
        }

    override suspend fun updateQuiz(id: String, updates: QuizDto): NetworkResult<Unit> =
        failOnNext ?: run {
            val index = cache.indexOfFirst { it.id == id }
            if (index == -1) return NetworkResult.HttpError(400)

            val newQuiz = updates.copy(id = id)
            cache[index] = newQuiz
            NetworkResult.success()
        }

    override suspend fun delete(id: String): NetworkResult<Unit> = failOnNext ?: run {
        if (cache.removeIf { it.id == id }) NetworkResult.success() else null
    } ?: NetworkResult.HttpError(404)
}

fun QuizDto.asListing() = QuizListingDto(
    id = id,
    date = date,
    user = user,
    title = title,
    expiration = expiration,
    isPublic = isPublic,
    resultsCount = results.size,
    questionCount = questions.size,
)

fun QuizDto.asForm() = QuizFormDto(
    id = id,
    date = date,
    username = "username",
    title = title,
    expiration = expiration,
    questions = questions.map {
        when (it) {
            is QuestionDto.MultipleChoice -> it.copy(correctAnswer = null)
            is QuestionDto.FillIn -> it.copy(correctAnswer = null)
        }
    }
)