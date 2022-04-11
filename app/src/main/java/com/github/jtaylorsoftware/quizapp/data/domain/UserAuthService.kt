package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.UserCredentials
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserRegistration
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.UserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserCredentialsDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserRegistrationDto
import com.github.jtaylorsoftware.quizapp.di.AppMainScope
import kotlinx.coroutines.*
import java.net.HttpURLConnection.*
import javax.inject.Inject

/**
 * Sign-up and sign-in service for Users.
 */
interface UserAuthService {
    /**
     * Returns `true` if the user is signed in locally, but may not be authorized.
     */
    fun userIsSignedIn(): Result<Boolean, Nothing>

    /**
     * Registers a new User.
     */
    suspend fun registerUser(registration: UserRegistration): Result<Unit, UserRegistrationErrors>

    /**
     * Signs in an existing User.
     */
    suspend fun signInUser(credentials: UserCredentials): Result<Unit, UserCredentialErrors>

    /**
     * Changes the current User's email.
     */
    suspend fun changeEmail(email: String): Result<Unit, String?>

    /**
     * Changes the current User's password.
     */
    suspend fun changePassword(password: String): Result<Unit, String?>
}

/**
 * Part or all of the registration data was invalid.
 */
data class UserRegistrationErrors(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
)

/**
 * Part or all of the credential data was invalid.
 */
data class UserCredentialErrors(
    val username: String? = null,
    val password: String? = null,
)

class UserAuthServiceImpl @Inject constructor(
    private val cache: UserCache,
    private val networkSource: UserNetworkSource,
    @AppMainScope private val externalScope: CoroutineScope = MainScope()
) : UserAuthService {

    override fun userIsSignedIn(): Result<Boolean, Nothing> = cache.loadToken()?.let {
        Result.success(true)
    } ?: Result.success(false)

    override suspend fun registerUser(registration: UserRegistration): Result<Unit, UserRegistrationErrors> =
        // Need to handle both registration and reading the result in NonCancellable context so
        // the token gets saved too
        withContext(externalScope.coroutineContext + NonCancellable) {
            when (val result =
                networkSource.registerUser(UserRegistrationDto.fromDomain(registration))) {
                is NetworkResult.Success -> {
                    cache.saveToken(result.value.token)
                    Result.success()
                }
                is NetworkResult.HttpError -> {
                    when (result.code) {
                        HTTP_BAD_REQUEST -> {
                            val validationError = parseRegistrationErrors(result.errors)
                            Result.BadRequest(validationError)
                        }
                        HTTP_CONFLICT -> {
                            val validationError = parseRegistrationErrors(result.errors)
                            Result.Conflict(validationError)
                        }
                        else -> Result.UnknownError
                    }
                }
                else -> Result.NetworkError
            }
        }

    override suspend fun signInUser(credentials: UserCredentials): Result<Unit, UserCredentialErrors> =
        withContext(externalScope.coroutineContext + NonCancellable) {
            when (val result =
                networkSource.signInUser(UserCredentialsDto.fromDomain(credentials))) {
                is NetworkResult.Success -> {
                    cache.saveToken(result.value.token)
                    Result.success()
                }
                is NetworkResult.HttpError -> {
                    when (result.code) {
                        HTTP_BAD_REQUEST -> {
                            val validationError = parseCredentialErrors(result.errors)
                            Result.BadRequest(validationError)
                        }
                        else -> Result.UnknownError
                    }
                }
                else -> Result.NetworkError
            }
        }

    override suspend fun changeEmail(email: String): Result<Unit, String?> =
        // Only wrap the changeEmail in NonCancellable because reading the result isn't
        // that important if we're cancelling (leaving screen)
        when (val result = withContext(externalScope.coroutineContext + NonCancellable) {
            networkSource.changeEmail(email)
        }) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> {
                when (result.code) {
                    HTTP_BAD_REQUEST -> {
                        val error = parseSingleError(result.errors, "email")
                        Result.BadRequest(error)
                    }
                    HTTP_UNAUTHORIZED -> Result.Unauthorized
                    HTTP_CONFLICT -> {
                        val error = parseSingleError(result.errors, "email")
                        Result.Conflict(error)
                    }
                    else -> Result.UnknownError
                }
            }
            else -> Result.NetworkError
        }

    override suspend fun changePassword(password: String): Result<Unit, String?> =
        when (val result = withContext(externalScope.coroutineContext + NonCancellable) {
            networkSource.changePassword(password)
        }) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> {
                when (result.code) {
                    HTTP_BAD_REQUEST -> {
                        val error = parseSingleError(result.errors, "password")
                        Result.BadRequest(error)
                    }
                    HTTP_UNAUTHORIZED -> Result.Unauthorized
                    else -> Result.UnknownError
                }
            }
            else -> Result.NetworkError
        }

    private suspend fun parseRegistrationErrors(apiErrors: List<ApiError>): UserRegistrationErrors {
        var error = UserRegistrationErrors()
        apiErrors.forEach { err ->
            yield()
            when (err.field) {
                "username" -> {
                    error = error.copy(username = err.message)
                }
                "email" -> {
                    error = error.copy(email = err.message)
                }
                "password" -> {
                    error = error.copy(password = err.message)
                }
            }
        }
        return error
    }

    private suspend fun parseCredentialErrors(apiErrors: List<ApiError>): UserCredentialErrors {
        var error = UserCredentialErrors()
        apiErrors.forEach { err ->
            yield()
            when (err.field) {
                "username" -> {
                    error = error.copy(username = err.message)
                }
                "password" -> {
                    error = error.copy(password = err.message)
                }
            }
        }
        return error
    }

    private suspend fun parseSingleError(apiErrors: List<ApiError>, errorField: String): String? {
        var error: String? = null
        apiErrors.forEach { err ->
            yield()
            if (err.field == errorField) {
                error = err.message
            }
        }
        return error
    }
}