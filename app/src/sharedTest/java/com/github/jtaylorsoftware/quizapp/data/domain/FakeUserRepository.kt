package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.data.local.*
import com.github.jtaylorsoftware.quizapp.data.network.FakeUserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.UserNetworkSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A simple implementation of UserRepository that can read from all sources,
 * but will not write out to the local database sources.
 * It can handle errors but assumes the only [NetworkResult.HttpError]
 * will be for status 401 (Unauthorized).
 */
class FakeUserRepository(
    private val userCache: UserCache = FakeUserCache(),
    private val userNetworkSource: UserNetworkSource = FakeUserNetworkSource(),
    private val quizListingDatabaseSource: QuizListingDatabaseSource = FakeQuizListingDatabaseSource(),
    private val resultListingDatabaseSource: QuizResultListingDatabaseSource = FakeQuizResultListingDatabaseSource(),
) : UserRepository {
    override fun getProfile(): Flow<Result<User, Any?>> = flow {
        userCache.loadUser()?.let {
            emit(Result.success(User.fromEntity(it)))
        }

        when (val networkResult = userNetworkSource.getProfile()) {
            is NetworkResult.Success -> {
                emit(Result.success(User.fromDto(networkResult.value)))
            }
            is NetworkResult.HttpError -> {
                emit(Result.Unauthorized)
            }
            is NetworkResult.NetworkError -> emit(Result.NetworkError)
            else -> emit(Result.UnknownError)
        }
    }

    override fun getQuizzes(): Flow<Result<List<QuizListing>, Any?>> = flow {
        userCache.loadUser()?.run {
            val entities = quizListingDatabaseSource.getAllCreatedByUser(id)
            val listings = entities.map { QuizListing.fromEntity(it) }
            emit(Result.success(listings))
        }

        when (val networkResult = userNetworkSource.getQuizzes()) {
            is NetworkResult.Success -> {
                emit(Result.success(networkResult.value.map { QuizListing.fromDto(it) }))
            }
            is NetworkResult.HttpError -> {
                emit(Result.Unauthorized)
            }
            is NetworkResult.NetworkError -> emit(Result.NetworkError)
            else -> emit(Result.UnknownError)
        }
    }

    override fun getResults(): Flow<Result<List<QuizResultListing>, Any?>> = flow {
        userCache.loadUser()?.run {
            val entities = resultListingDatabaseSource.getAllByUser(id)
            val listings = entities.map { QuizResultListing.fromEntity(it) }
            emit(Result.success(listings))
        }

        when (val networkResult = userNetworkSource.getResults()) {
            is NetworkResult.Success -> {
                emit(Result.success(networkResult.value.map { QuizResultListing.fromDto(it) }))
            }
            is NetworkResult.HttpError -> {
                emit(Result.Unauthorized)
            }
            is NetworkResult.NetworkError -> emit(Result.NetworkError)
            else -> emit(Result.UnknownError)
        }
    }
}