package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.UserCredentials
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserRegistration
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.UserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.*
import com.github.jtaylorsoftware.quizapp.di.AppMainScope
import kotlinx.coroutines.*
import java.net.HttpURLConnection.*
import javax.inject.Inject

/**
 * Sign-up and login service for Users.
 */
interface UserAuthService {
    /**
     * Returns `true` if the user is signed in locally, but may not be authorized.
     */
    fun userIsSignedIn(): ResultOrFailure<Boolean>

    /**
     * Registers a new User.
     */
    suspend fun registerUser(registration: UserRegistration): Result<Unit, UserRegistrationErrors>

    /**
     * Signs in an existing User.
     */
    suspend fun signInUser(credentials: UserCredentials): Result<Unit, UserCredentialErrors>

    /**
     * Signs out the current User, clearing their token and cached data.
     */
    suspend fun signOut()

    /**
     * Changes the current User's email.
     */
    suspend fun changeEmail(email: String): Result<Unit, ChangeEmailError>

    /**
     * Changes the current User's password.
     */
    suspend fun changePassword(password: String): Result<Unit, ChangePasswordError>
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

/**
 * The email to change to is possibly invalid.
 */
data class ChangeEmailError(
    val email: String? = null
)

/**
 * The password to change to is possibly invalid.
 */
data class ChangePasswordError(
    val password: String? = null
)

class UserAuthServiceImpl @Inject constructor(
    private val cache: UserCache,
    private val networkSource: UserNetworkSource,
    @AppMainScope private val externalScope: CoroutineScope = MainScope()
) : UserAuthService {

    override fun userIsSignedIn(): ResultOrFailure<Boolean> = cache.loadToken()?.let {
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
                        HTTP_BAD_REQUEST, HTTP_CONFLICT -> {
                            val validationError = parseRegistrationErrors(result.errors)
                            Result.failure(FailureReason.FORM_HAS_ERRORS, validationError)
                        }
                        else -> Result.failure(FailureReason.NETWORK)
                    }
                }
                else -> Result.failure(FailureReason.NETWORK)
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
                            Result.failure(FailureReason.FORM_HAS_ERRORS, validationError)
                        }
                        else -> Result.failure(FailureReason.NETWORK)
                    }
                }
                else -> Result.failure(FailureReason.NETWORK)
            }
        }

    override suspend fun signOut() {
        withContext(externalScope.coroutineContext + NonCancellable) {
            cache.clearToken()
            cache.clearUser()
        }
    }

    override suspend fun changeEmail(email: String): Result<Unit, ChangeEmailError> =
        // Only wrap the changeEmail in NonCancellable because reading the result isn't
        // that important if we're cancelling (leaving screen)
        when (val result = withContext(externalScope.coroutineContext + NonCancellable) {
            networkSource.changeEmail(ChangeEmailRequest(email))
        }) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> {
                when (result.code) {
                    HTTP_BAD_REQUEST, HTTP_CONFLICT -> {
                        val error = parseSingleError(result.errors, "email")
                        Result.failure(FailureReason.FORM_HAS_ERRORS, ChangeEmailError(error))
                    }
                    HTTP_UNAUTHORIZED -> Result.Failure(FailureReason.UNAUTHORIZED)
                    else -> Result.failure(FailureReason.NETWORK)
                }
            }
            else -> Result.failure(FailureReason.NETWORK)
        }

    override suspend fun changePassword(password: String): Result<Unit, ChangePasswordError> =
        when (val result = withContext(externalScope.coroutineContext + NonCancellable) {
            networkSource.changePassword(ChangePasswordRequest(password))
        }) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> {
                when (result.code) {
                    HTTP_BAD_REQUEST -> {
                        val error = parseSingleError(result.errors, "password")
                        Result.failure(FailureReason.FORM_HAS_ERRORS, ChangePasswordError(error))
                    }
                    HTTP_UNAUTHORIZED -> Result.Failure(FailureReason.UNAUTHORIZED)
                    else -> Result.failure(FailureReason.NETWORK)
                }
            }
            else -> Result.failure(FailureReason.NETWORK)
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