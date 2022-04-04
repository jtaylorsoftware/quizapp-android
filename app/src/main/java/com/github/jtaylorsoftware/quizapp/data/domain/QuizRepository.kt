package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.QuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.HttpURLConnection.*
import javax.inject.Inject

/**
 * Provides methods for creating and editing quizzes and getting quiz forms.
 */
interface QuizRepository {
    /**
     * Gets a single Quiz.
     */
    suspend fun getQuiz(id: ObjectId): Result<Quiz, Any?>

    /**
     * Gets a single QuizListing.
     *
     * @return A Flow that may emit multiple times if fresher data can be made available.
     */
    fun getAsListing(id: ObjectId): Flow<Result<QuizListing, Any?>>

    /**
     * Gets a QuizForm that the user can use to submit responses to a Quiz.
     */
    suspend fun getFormForQuiz(id: ObjectId): Result<QuizForm, Any?>

    /**
     * Allows a user to submit a new Quiz.
     */
    suspend fun createQuiz(quiz: Quiz): Result<ObjectId, QuizValidationErrors>

    /**
     * Allows a user to edit an existing Quiz.
     */
    suspend fun editQuiz(id: ObjectId, edits: Quiz): Result<Unit, QuizValidationErrors>

    /**
     * Deletes an existing Quiz created by the current user.
     */
    suspend fun deleteQuiz(id: ObjectId): Result<Unit, Any?>
}

data class QuizValidationErrors(
    val title: String? = null,
    val expiration: String? = null,
    val allowedUsers: String? = null,

    /**
     * Error affecting the entire question list (usually because
     * there's too few questions).
     */
    val questions: String? = null,

    /**
     * Errors affecting individual questions.
     */
    val questionErrors: List<String?> = emptyList(),
)

class QuizRepositoryImpl @Inject constructor(
    private val databaseSource: QuizListingDatabaseSource,
    private val networkSource: QuizNetworkSource
) : QuizRepository {
    override suspend fun getQuiz(id: ObjectId): Result<Quiz, Any?> =
        when (val result = networkSource.getById(id.value)) {
            is NetworkResult.Success -> {
                Result.success(Quiz.fromDto(result.value))
            }
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    override fun getAsListing(id: ObjectId): Flow<Result<QuizListing, Any?>> = flow {
        databaseSource.getById(id.value)?.let {
            emit(Result.success(QuizListing.fromEntity(it)))
        }

        val result = when (val networkResult = networkSource.getListingById(id.value)) {
            is NetworkResult.Success -> {
                val listing = networkResult.value.let {
                    databaseSource.insertAll(listOf(QuizListingEntity.fromDto(it)))
                    QuizListing.fromDto(it)
                }
                Result.success(listing)
            }
            is NetworkResult.HttpError -> handleGenericHttpError(networkResult)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }
        emit(result)
    }

    override suspend fun getFormForQuiz(id: ObjectId): Result<QuizForm, Any?> =
        when (val result = networkSource.getForm(id.value)) {
            is NetworkResult.Success -> {
                Result.success(QuizForm.fromDto(result.value))
            }
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    override suspend fun createQuiz(quiz: Quiz): Result<ObjectId, QuizValidationErrors> =
        when (val result = networkSource.createQuiz(QuizDto.fromDomain(quiz))) {
            is NetworkResult.Success -> {
                Result.success(ObjectId(result.value.id))
            }
            is NetworkResult.HttpError -> handleQuizHttpError(result, quiz.questions.size)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    override suspend fun editQuiz(id: ObjectId, edits: Quiz): Result<Unit, QuizValidationErrors> =
        when (val result = networkSource.updateQuiz(id.value, QuizDto.fromDomain(edits))) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> handleQuizHttpError(result, edits.questions.size)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    override suspend fun deleteQuiz(id: ObjectId): Result<Unit, Any?> =
        when (val result = networkSource.delete(id.value)) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> handleGenericHttpError(result)
            is NetworkResult.NetworkError -> Result.NetworkError
            else -> Result.UnknownError
        }

    private fun <T> handleGenericHttpError(httpError: NetworkResult.HttpError): Result<T, Nothing> =
        when (httpError.code) {
            HTTP_UNAUTHORIZED -> Result.Unauthorized
            HTTP_FORBIDDEN -> Result.Forbidden
            HTTP_NOT_FOUND -> Result.NotFound
            else -> Result.NetworkError
        }

    private fun <T> handleQuizHttpError(
        result: NetworkResult.HttpError,
        numQuestions: Int
    ): Result<T, QuizValidationErrors> = when (result.code) {
        HTTP_BAD_REQUEST -> Result.BadRequest(parseApiErrors(result.errors, numQuestions))
        HTTP_UNAUTHORIZED -> Result.Unauthorized
        HTTP_FORBIDDEN -> Result.Forbidden
        HTTP_NOT_FOUND -> Result.NotFound
        else -> Result.NetworkError
    }

    private fun parseApiErrors(apiErrors: List<ApiError>, numQuestions: Int): QuizValidationErrors {
        val questionErrors = MutableList<String?>(numQuestions) { null }
        var errors = QuizValidationErrors(questionErrors = questionErrors)
        apiErrors.forEach { err ->
            when (err.field) {
                "title" -> {
                    errors = errors.copy(title = err.message)
                }
                "expiration" -> {
                    errors = errors.copy(expiration = err.message)
                }
                "allowedUsers" -> {
                    errors = errors.copy(allowedUsers = err.message)
                }
                "questions" -> {
                    if (err.index != null && err.index in questionErrors.indices) {
                        // An error for one specific question
                        questionErrors[err.index] = err.message
                    } else {
                        // An error applying to all questions / the array of questions (length, etc)
                        errors = errors.copy(questions = err.message)
                    }
                }
            }
        }
        return errors
    }
}