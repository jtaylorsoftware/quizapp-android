package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.QuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizFormResponsesDto
import com.github.jtaylorsoftware.quizapp.di.AppMainScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.HttpURLConnection.*
import javax.inject.Inject

/**
 * Provides methods for submitting responses to quizzes and getting graded results.
 */
interface QuizResultRepository {
    /**
     * Gets one user's QuizResult when the signed-in user created either the Result or Quiz.
     */
    suspend fun getForQuizByUser(quiz: ObjectId, user: ObjectId): ResultOrFailure<QuizResult>

    /**
     * Gets all QuizResults for a Quiz when the signed-in user is the one that created the Quiz.
     */
    suspend fun getAllForQuiz(quiz: ObjectId): ResultOrFailure<List<QuizResult>>

    /**
     * Gets one user's QuizResultListing when the signed-in user created either the Result or Quiz.
     *
     * @return A Flow that may emit multiple times if fresher data can be made available.
     */
    fun getListingForQuizByUser(
        quiz: ObjectId,
        user: ObjectId
    ): Flow<ResultOrFailure<QuizResultListing>>

    /**
     * Gets all QuizResultListings for a Quiz when the signed-in user is the one that created the Quiz.
     *
     * @return A Flow that may emit multiple times if fresher data can be made available.
     */
    fun getAllListingsForQuiz(quiz: ObjectId): Flow<ResultOrFailure<List<QuizResultListing>>>

    /**
     * Allows a user to submit their responses to a Quiz.
     */
    suspend fun createResponseForQuiz(
        responses: List<QuestionResponse>,
        quiz: ObjectId
    ): Result<ObjectId, ResponseValidationErrors>
}

data class ResponseValidationErrors(
    /**
     * Errors affecting the entire answers array (likely incorrect length).
     */
    val answers: String? = null,

    /**
     * Errors affecting individual answers.
     */
    val answerErrors: List<String?> = emptyList(),

    /**
     * The Quiz being responded to has "expired" and users may not submit responses.
     */
    val expired: Boolean = false,
)

class QuizResultRepositoryImpl @Inject constructor(
    private val databaseSource: QuizResultListingDatabaseSource,
    private val networkSource: QuizResultNetworkSource,
    @AppMainScope private val externalScope: CoroutineScope = MainScope()
) : QuizResultRepository {
    override suspend fun getForQuizByUser(
        quiz: ObjectId,
        user: ObjectId
    ): ResultOrFailure<QuizResult> =
        when (val result = networkSource.getForQuizByUser(quiz.value, user.value)) {
            is NetworkResult.Success -> Result.success(QuizResult.fromDto(result.value))
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    override suspend fun getAllForQuiz(quiz: ObjectId): ResultOrFailure<List<QuizResult>> =
        when (val result = networkSource.getAllForQuiz(quiz.value)) {
            is NetworkResult.Success -> Result.success(result.value.let { (dtoResults) ->
                dtoResults.map {
                    QuizResult.fromDto(it)
                }
            })
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    override fun getListingForQuizByUser(
        quiz: ObjectId,
        user: ObjectId
    ): Flow<ResultOrFailure<QuizResultListing>> = flow {
        databaseSource.getByQuizAndUser(quiz.value, user.value)?.let {
            emit(Result.success(QuizResultListing.fromEntity(it)))
        }

        val result = when (val networkResult =
            networkSource.getListingForQuizByUser(quiz.value, user.value)) {
            is NetworkResult.Success -> {
                val listing = networkResult.value.let {
                    databaseSource.insertAll(listOf(QuizResultListingEntity.fromDto(it)))
                    QuizResultListing.fromDto(it)
                }
                Result.success(listing)
            }
            is NetworkResult.HttpError -> {
                if (networkResult.code == HTTP_NOT_FOUND) {
                    // Listing actually no longer exists
                    databaseSource.deleteByQuizAndUser(quiz.value, user.value)
                }
                handleGenericHttpError(networkResult)
            }
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }
        emit(result)
    }

    override fun getAllListingsForQuiz(quiz: ObjectId): Flow<ResultOrFailure<List<QuizResultListing>>> =
        flow {
            emit(
                Result.success(
                    databaseSource.getAllByQuiz(quiz.value).map { QuizResultListing.fromEntity(it) }
                )
            )

            val result = when (val networkResult =
                networkSource.getAllListingForQuiz(quiz.value)) {
                is NetworkResult.Success -> {
                    val dtoListings = networkResult.value.results
                    if (dtoListings.isEmpty()) {
                        // API returned an empty list, in which case they are
                        // actually deleted and all local data is invalid
                        databaseSource.deleteAllByQuiz(quiz.value)
                        Result.success(emptyList())
                    } else {
                        databaseSource.insertAll(dtoListings.map {
                            QuizResultListingEntity.fromDto(it)
                        })
                        Result.success(dtoListings.map { QuizResultListing.fromDto(it) })
                    }
                }
                is NetworkResult.HttpError -> handleGenericHttpError(networkResult)
                is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
                else -> Result.Failure(FailureReason.UNKNOWN)
            }
            emit(result)
        }

    override suspend fun createResponseForQuiz(
        responses: List<QuestionResponse>,
        quiz: ObjectId
    ): Result<ObjectId, ResponseValidationErrors> =
        when (val result = withContext(externalScope.coroutineContext + NonCancellable) {
            networkSource.createResultForQuiz(
                QuizFormResponsesDto.fromDomain(responses), quiz.value
            )
        }) {
            is NetworkResult.Success -> Result.success(ObjectId(result.value.id))
            is NetworkResult.HttpError -> handleResponseHttpError(result, responses.size)
            is NetworkResult.NetworkError -> Result.Failure(FailureReason.NETWORK)
            else -> Result.Failure(FailureReason.UNKNOWN)
        }

    private suspend fun <T> handleResponseHttpError(
        httpError: NetworkResult.HttpError,
        numAnswers: Int
    ): Result<T, ResponseValidationErrors> =
        when (httpError.code) {
            HTTP_BAD_REQUEST, HTTP_FORBIDDEN -> {
                val errors = parseApiErrors(httpError.errors, numAnswers)
                if (errors.expired) {
                    Result.Failure(FailureReason.QUIZ_EXPIRED) // Quiz is expired, can't respond
                } else {
                    Result.Failure(FailureReason.FORM_HAS_ERRORS, errors)
                }
            }
            HTTP_UNAUTHORIZED -> Result.Failure(FailureReason.UNAUTHORIZED)
            HTTP_NOT_FOUND -> Result.Failure(FailureReason.NOT_FOUND)
            else -> Result.Failure(FailureReason.NETWORK)
        }

    private fun handleGenericHttpError(httpError: NetworkResult.HttpError): Result<Nothing, Nothing> =
        when (httpError.code) {
            HTTP_UNAUTHORIZED -> Result.Failure(FailureReason.UNAUTHORIZED)
            HTTP_FORBIDDEN -> Result.Failure(FailureReason.FORBIDDEN)
            HTTP_NOT_FOUND -> Result.Failure(FailureReason.NOT_FOUND)
            else -> Result.Failure(FailureReason.NETWORK)
        }

    private suspend fun parseApiErrors(
        apiErrors: List<ApiError>,
        numAnswers: Int
    ): ResponseValidationErrors {
        val answerErrors = MutableList<String?>(numAnswers) { null }
        var errors = ResponseValidationErrors(answerErrors = answerErrors)
        apiErrors.forEach { err ->
            yield()
            when (err.field) {
                "expiration" -> {
                    errors = errors.copy(expired = true)
                    // Rest doesn't matter, can't respond to expired quiz
                    return errors
                }
                "answers" -> {
                    if (err.index != null && err.index in answerErrors.indices) {
                        // An error for one specific question
                        answerErrors[err.index] = err.message
                    } else {
                        // An error applying to all questions / the array of questions (length, etc)
                        errors = errors.copy(answers = err.message)
                    }
                }
            }
        }
        return errors
    }
}