package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.QuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizFormResponsesDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.HttpURLConnection

/**
 * Fake [QuizResultRepository] that will read from, but doesn't write to, local data sources.
 */
class FakeQuizResultRepository(
    private val databaseSource: QuizResultListingDatabaseSource = FakeQuizResultListingDatabaseSource(),
    private val networkSource: QuizResultNetworkSource = FakeQuizResultNetworkSource()
) : QuizResultRepository {
    override suspend fun getForQuizByUser(
        quiz: ObjectId,
        user: ObjectId
    ): Result<QuizResult, Any?> =
        when (val result = networkSource.getForQuizByUser(quiz.value, user.value)) {
            is NetworkResult.Success -> Result.success(QuizResult.fromDto(result.value))
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    override suspend fun getAllForQuiz(quiz: ObjectId): Result<List<QuizResult>, Any?> =
        when (val result = networkSource.getAllForQuiz(quiz.value)) {
            is NetworkResult.Success -> Result.success(result.value.map { QuizResult.fromDto(it) })
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    override fun getListingForQuizByUser(
        quiz: ObjectId,
        user: ObjectId
    ): Flow<Result<QuizResultListing, Any?>> = flow {
        databaseSource.getByQuizAndUser(quiz.value, user.value)?.let {
            emit(Result.success(QuizResultListing.fromEntity(it)))
        }

        val result = when (val networkResult =
            networkSource.getListingForQuizByUser(quiz.value, user.value)) {
            is NetworkResult.Success -> {
                val listing = QuizResultListing.fromDto(networkResult.value)
                Result.success(listing)
            }
            is NetworkResult.HttpError -> handleGenericHttpError(networkResult)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }
        emit(result)
    }


    override fun getAllListingsForQuiz(quiz: ObjectId): Flow<Result<List<QuizResultListing>, Any?>> =
        flow {
            emit(
                Result.success(
                    databaseSource.getAllByQuiz(quiz.value)
                        .map { QuizResultListing.fromEntity(it) })
            )

            val result = when (val networkResult =
                networkSource.getAllListingForQuiz(quiz.value)) {
                is NetworkResult.Success -> {
                    val listings = networkResult.value.map { QuizResultListing.fromDto(it) }
                    Result.success(listings)
                }
                is NetworkResult.HttpError -> handleGenericHttpError(networkResult)
                is NetworkResult.NetworkError -> Result.NetworkError
                else -> Result.UnknownError
            }
            emit(result)
        }

    override suspend fun createResponseForQuiz(
        responses: List<QuestionResponse>,
        quiz: ObjectId
    ): Result<ObjectId, ResponseValidationErrors>  =
        when (val result = networkSource.createResultForQuiz(
            QuizFormResponsesDto.fromDomain(responses),
            quiz.value
        )) {
            is NetworkResult.Success -> Result.success(ObjectId(result.value.id))
            is NetworkResult.HttpError -> handleResponseHttpError(result, responses.size)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    private fun <T> handleGenericHttpError(httpError: NetworkResult.HttpError): Result<T, Nothing> =
        when (httpError.code) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> Result.Unauthorized
            HttpURLConnection.HTTP_FORBIDDEN -> Result.Forbidden
            HttpURLConnection.HTTP_NOT_FOUND -> Result.NotFound
            else -> Result.NetworkError
        }

    private fun <T> handleResponseHttpError(
        httpError: NetworkResult.HttpError,
        numAnswers: Int
    ): Result<T, ResponseValidationErrors> =
        when (httpError.code) {
            // Checks for expired error, otherwise assumes all errors are present with the same message
            HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_FORBIDDEN -> {
                val expired = httpError.errors.any { it.field == "expiration" }
                val message = httpError.errors[0].message
                if (expired) {
                    Result.Expired // Quiz is expired, can't respond
                } else {
                    Result.BadRequest(
                        ResponseValidationErrors(
                            answers = message,
                            answerErrors = List(numAnswers) { message },
                            expired = false
                        )
                    )
                }
            }
            HttpURLConnection.HTTP_UNAUTHORIZED -> Result.Unauthorized
            HttpURLConnection.HTTP_NOT_FOUND -> Result.NotFound
            else -> Result.NetworkError
        }
}