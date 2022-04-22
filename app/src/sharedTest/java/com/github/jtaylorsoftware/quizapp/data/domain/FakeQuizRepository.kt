package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.QuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.HttpURLConnection

/**
 * Fake [QuizRepository] that will read from, but doesn't write to, local data sources.
 */
class FakeQuizRepository constructor(
    private val databaseSource: QuizListingDatabaseSource = FakeQuizListingDatabaseSource(),
    private val networkSource: QuizNetworkSource = FakeQuizNetworkSource(),
) : QuizRepository {
    override suspend fun getQuiz(id: ObjectId): ResultOrFailure<Quiz> =
        when (val result = networkSource.getById(id.value)) {
            is NetworkResult.Success -> {
                Result.success(Quiz.fromDto(result.value))
            }
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    override fun getAsListing(id: ObjectId): Flow<ResultOrFailure<QuizListing>> = flow {
        databaseSource.getById(id.value)?.let {
            emit(Result.success(QuizListing.fromEntity(it)))
        }

        val result = when (val networkResult = networkSource.getListingById(id.value)) {
            is NetworkResult.Success -> {
                val listing = QuizListing.fromDto(networkResult.value)
                Result.success(listing)
            }
            is NetworkResult.HttpError -> handleGenericHttpError(networkResult)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }
        emit(result)
    }

    override suspend fun getFormForQuiz(id: ObjectId): ResultOrFailure<QuizForm> =
        when (val result = networkSource.getForm(id.value)) {
            is NetworkResult.Success -> {
                Result.success(QuizForm.fromDto(result.value))
            }
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    /**
     * Performs realistic submission of [quiz], except it assumes
     * on HttpError of HTTP_BAD_REQUEST that ALL errors are present.
     */
    override suspend fun createQuiz(quiz: Quiz): Result<ObjectId, QuizValidationErrors> =
        when (val result = networkSource.createQuiz(QuizDto.fromDomain(quiz))) {
            is NetworkResult.Success -> {
                Result.success(ObjectId(result.value.id))
            }
            is NetworkResult.HttpError -> handleQuizHttpError(result, quiz.questions.size)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    /**
     * Performs realistic editing of [quiz], except it assumes
     * on HttpError of HTTP_BAD_REQUEST that ALL errors are present.
     */
    override suspend fun editQuiz(id: ObjectId, edits: Quiz): Result<Unit, QuizValidationErrors> =
        when (val result = networkSource.updateQuiz(id.value, QuizDto.fromDomain(edits))) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> handleQuizHttpError(result, edits.questions.size)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    override suspend fun deleteQuiz(id: ObjectId): ResultOrFailure<Unit> =
        when (val result = networkSource.delete(id.value)) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    private fun <T> handleGenericHttpError(httpError: NetworkResult.HttpError): Result<T, Nothing> =
        when (httpError.code) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> Result.Failure(FailureReason.UNAUTHORIZED)
            HttpURLConnection.HTTP_FORBIDDEN -> Result.Failure(FailureReason.FORBIDDEN)
            HttpURLConnection.HTTP_NOT_FOUND -> Result.Failure(FailureReason.NOT_FOUND)
            else -> Result.Failure(FailureReason.NETWORK)
        }

    private fun <T> handleQuizHttpError(
        result: NetworkResult.HttpError,
        numQuestions: Int
    ): Result<T, QuizValidationErrors> = when (result.code) {
        // Assumes all errors are present in the request and all have the same message
        HttpURLConnection.HTTP_BAD_REQUEST -> {
            val message = result.errors[0].message
            Result.Failure(
                FailureReason.FORM_HAS_ERRORS,
                QuizValidationErrors(
                    title = message,
                    expiration = message,
                    allowedUsers = message,
                    questions = message,
                    questionErrors = List(numQuestions) { message })
            )
        }
        HttpURLConnection.HTTP_UNAUTHORIZED -> Result.Failure(FailureReason.UNAUTHORIZED)
        HttpURLConnection.HTTP_FORBIDDEN -> Result.Failure(FailureReason.FORBIDDEN)
        HttpURLConnection.HTTP_NOT_FOUND -> Result.Failure(FailureReason.NOT_FOUND)
        else -> Result.Failure(FailureReason.NETWORK)
    }
}