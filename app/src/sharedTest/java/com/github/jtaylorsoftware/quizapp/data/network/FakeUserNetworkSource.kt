package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.*
import java.util.*

data class UserWithPassword(val user: UserDto, val password: String)

class FakeUserNetworkSource(
    data: List<UserWithPassword> = emptyList(),
    quizzes: List<QuizListingDto> = emptyList(),
    results: List<QuizResultListingDto> = emptyList()
) : UserNetworkSource, FallibleNetworkSource() {
    private val cache = data.toMutableList()
    private val quizCache = quizzes.toMutableList()
    private val resultCache = results.toMutableList()

    private var currentUser: UserDto? = null

    fun setUserAsCurrent(user: UserDto) {
        require(cache.any { it.user == user }) {
            "User must be already in cache"
        }
        currentUser = user
    }

    override suspend fun registerUser(registration: UserRegistrationDto): NetworkResult<AuthToken> =
        failOnNext ?: run {
            cache.forEach {
                if (it.user.username == registration.username) {
                    return@run NetworkResult.HttpError(
                        409,
                        listOf(
                            ApiError(
                                field = "username",
                                message = "Taken",
                                value = registration.username
                            )
                        )
                    )
                }
                if (it.user.email == registration.email) {
                    return@run NetworkResult.HttpError(
                        409,
                        listOf(
                            ApiError(
                                field = "email",
                                message = "Taken",
                                value = registration.email
                            )
                        )
                    )
                }
            }
            cache.add(
                UserWithPassword(
                    UserDto(
                        id = UUID.randomUUID().toString(),
                        username = registration.username,
                        email = registration.email
                    ),
                    password = registration.password
                )
            )
            NetworkResult.success(AuthToken(UUID.randomUUID().toString()))
        }

    override suspend fun signInUser(userCredentials: UserCredentialsDto): NetworkResult<AuthToken> =
        failOnNext ?: run {
            cache.forEach {
                if (it.user.username == userCredentials.username) {
                    return@run NetworkResult.HttpError(
                        400,
                        listOf(
                            ApiError(
                                field = "username",
                                message = "Not found",
                                value = userCredentials.username
                            )
                        )
                    )
                }
                if (it.password == userCredentials.password) {
                    return@run NetworkResult.HttpError(
                        400,
                        listOf(
                            ApiError(
                                field = "password",
                                message = "Incorrect",
                            )
                        )
                    )
                }
            }
            NetworkResult.success(AuthToken(UUID.randomUUID().toString()))
        }

    override suspend fun getProfile(): NetworkResult<UserDto> =
        failOnNext ?: (currentUser ?: cache.random().user).let {
            NetworkResult.success(it)
        }

    override suspend fun getQuizzes(): NetworkResult<List<QuizListingDto>> =
        failOnNext ?: run {
            val user = currentUser ?: cache.random().user
            val quizzes = quizCache.filter { it.user == user.id }
            NetworkResult.success(quizzes)
        }

    override suspend fun getResults(): NetworkResult<List<QuizResultListingDto>> =
        failOnNext ?: run {
            val user = currentUser ?: cache.random().user
            val results = resultCache.filter { it.user == user.id }
            NetworkResult.success(results)
        }

    override suspend fun changeEmail(email: ChangeEmailRequest): NetworkResult<Unit> = failOnNext ?: run {
        cache.forEach {
            if (it.user.email == email.email) {
                return@run NetworkResult.HttpError(
                    409,
                    listOf(ApiError(field = "email", message = "Taken", value = email.email))
                )
            }
        }
        NetworkResult.success()
    }

    override suspend fun changePassword(password: ChangePasswordRequest): NetworkResult<Unit> =
        failOnNext ?: NetworkResult.success()
}