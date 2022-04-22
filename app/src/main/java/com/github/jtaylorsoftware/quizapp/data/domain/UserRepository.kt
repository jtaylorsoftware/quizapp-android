package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.UserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizListingDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultListingDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject

/**
 * Provides methods for retrieving User profile data.
 */
interface UserRepository {
    /**
     * Gets the current user's profile data.
     *
     * @return A Flow that may emit multiple times if fresher data can be made available.
     */
    fun getProfile(): Flow<ResultOrFailure<User>>

    /**
     * Gets a list containing the user's created quizzes as listings.
     *
     * @return A Flow that may emit multiple times if fresher data can be made available.
     */
    fun getQuizzes(): Flow<ResultOrFailure<List<QuizListing>>>

    /**
     * Gets a list containing the user's submitted results as listings.
     *
     * @return A Flow that may emit multiple times if fresher data can be made available.
     */
    fun getResults(): Flow<ResultOrFailure<List<QuizResultListing>>>
}

class UserRepositoryImpl @Inject constructor(
    private val userCache: UserCache,
    private val userNetworkSource: UserNetworkSource,
    private val quizListingDatabaseSource: QuizListingDatabaseSource,
    private val resultListingDatabaseSource: QuizResultListingDatabaseSource,
) : UserRepository {
    override fun getProfile(): Flow<ResultOrFailure<User>> = flow {
        userCache.loadUser()?.let {
            emit(Result.success(User.fromEntity(it)))
        }
        val result = when (val networkResult = userNetworkSource.getProfile()) {
            is NetworkResult.Success -> {
                val user = networkResult.value.let {
                    userCache.saveUser(UserEntity.fromDto(it))
                    User.fromDto(it)
                }
                Result.success(user)
            }
            is NetworkResult.HttpError -> {
                if (networkResult.code == HTTP_UNAUTHORIZED) {
                    Result.Failure(FailureReason.UNAUTHORIZED)
                } else {
                    Result.failure(FailureReason.NETWORK)
                }
            }
            is NetworkResult.NetworkError -> Result.failure(FailureReason.NETWORK)
            else -> Result.failure(FailureReason.UNKNOWN)
        }

        emit(result)
    }

    override fun getQuizzes(): Flow<ResultOrFailure<List<QuizListing>>> = flow {
        userCache.loadUser()?.run {
            val entities = quizListingDatabaseSource.getAllCreatedByUser(id)
            val listings = entities.map { QuizListing.fromEntity(it) }
            emit(Result.success(listings))
        }

        val result = when (val networkResult = userNetworkSource.getQuizzes()) {
            is NetworkResult.Success -> {
                val dtoListings = networkResult.value
                if (dtoListings.isEmpty()) {
                    // API returned an empty list, in which case they are
                    // actually deleted and all local data is invalid
                    userCache.loadUser()?.id?.let { userId ->
                        quizListingDatabaseSource.deleteAllByUser(userId)
                    }
                    Result.success(emptyList<QuizListing>())
                } else {
                    val entities = mutableListOf<QuizListingEntity>()
                    val listings = mutableListOf<QuizListing>()
                    convertQuizListingDto(dtoListings, entities, listings)
                    quizListingDatabaseSource.insertAll(entities)
                    Result.success(listings)
                }
            }
            is NetworkResult.HttpError -> {
                if (networkResult.code == HTTP_UNAUTHORIZED) {
                    Result.Failure(FailureReason.UNAUTHORIZED)
                } else {
                    Result.failure(FailureReason.NETWORK)
                }
            }
            is NetworkResult.NetworkError -> Result.failure(FailureReason.NETWORK)
            else -> Result.failure(FailureReason.UNKNOWN)
        }

        emit(result)
    }

    override fun getResults(): Flow<ResultOrFailure<List<QuizResultListing>>> = flow {
        userCache.loadUser()?.run {
            val entities = resultListingDatabaseSource.getAllByUser(id)
            val listings = entities.map { QuizResultListing.fromEntity(it) }
            emit(Result.success(listings))
        }

        val result = when (val networkResult = userNetworkSource.getResults()) {
            is NetworkResult.Success -> {
                val dtoListings = networkResult.value
                if (dtoListings.isEmpty()) {
                    // API returned an empty list, in which case they are
                    // actually deleted and all local data is invalid
                    userCache.loadUser()?.id?.let { userId ->
                        resultListingDatabaseSource.deleteAllByUser(userId)
                    }
                    Result.success(emptyList<QuizResultListing>())
                } else {
                    val entities = mutableListOf<QuizResultListingEntity>()
                    val listings = mutableListOf<QuizResultListing>()
                    convertResultListingDto(dtoListings, entities, listings)
                    resultListingDatabaseSource.insertAll(entities)
                    Result.success(listings)
                }
            }
            is NetworkResult.HttpError -> {
                if (networkResult.code == HTTP_UNAUTHORIZED) {
                    Result.Failure(FailureReason.UNAUTHORIZED)
                } else {
                    Result.failure(FailureReason.NETWORK)
                }
            }
            is NetworkResult.NetworkError -> Result.failure(FailureReason.NETWORK)
            else -> Result.failure(FailureReason.UNKNOWN)
        }

        emit(result)
    }

    private suspend fun convertQuizListingDto(
        dto: List<QuizListingDto>,
        outEntities: MutableList<QuizListingEntity>,
        outListings: MutableList<QuizListing>
    ) {
        dto.forEach {
            yield()
            outEntities += QuizListingEntity.fromDto(it)
            outListings += QuizListing.fromDto(it)
        }
    }

    private suspend fun convertResultListingDto(
        dto: List<QuizResultListingDto>,
        outEntities: MutableList<QuizResultListingEntity>,
        outListings: MutableList<QuizResultListing>
    ) {
        dto.forEach {
            yield()
            outEntities += QuizResultListingEntity.fromDto(it)
            outListings += QuizResultListing.fromDto(it)
        }
    }
}