package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.UserCredentials
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserRegistration
import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.data.network.FakeUserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.UserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserCredentialsDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserRegistrationDto

class FakeUserAuthService constructor(
    private val userCache: UserCache = FakeUserCache(),
    private val networkSource: UserNetworkSource = FakeUserNetworkSource()
) : UserAuthService {
    override fun userIsSignedIn(): Result<Boolean, Nothing> {
        return userCache.loadToken()?.let { Result.success(true) } ?: Result.success(false)
    }

    override suspend fun registerUser(registration: UserRegistration): Result<Unit, UserRegistrationErrors> {
        return when (val networkResult =
            networkSource.registerUser(UserRegistrationDto.fromDomain(registration))) {
            is NetworkResult.Success -> {
                userCache.saveToken(networkResult.value.token)
                Result.success()
            }
            is NetworkResult.HttpError -> {
                Result.BadRequest(
                    UserRegistrationErrors(
                        username = "Invalid",
                        email = "Invalid",
                        password = "Invalid"
                    )
                )
            }
            else -> Result.NetworkError
        }
    }

    override suspend fun signInUser(credentials: UserCredentials): Result<Unit, UserCredentialErrors> {
        return when (val networkResult =
            networkSource.signInUser(UserCredentialsDto.fromDomain(credentials))) {
            is NetworkResult.Success -> {
                userCache.saveToken(networkResult.value.token)
                Result.success()
            }
            is NetworkResult.HttpError -> {
                Result.BadRequest(
                    UserCredentialErrors(
                        username = "Invalid",
                        password = "Invalid"
                    )
                )
            }
            else -> Result.NetworkError
        }
    }

    override suspend fun changeEmail(email: String): Result<Unit, String?> {
        return when (networkSource.changeEmail(email)) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> {
                Result.BadRequest("Invalid")
            }
            else -> Result.NetworkError
        }
    }

    override suspend fun changePassword(password: String): Result<Unit, String?> {
        return when (networkSource.changePassword(password)) {
            is NetworkResult.Success -> {
                Result.success()
            }
            is NetworkResult.HttpError -> {
                Result.BadRequest("Invalid")
            }
            else -> Result.NetworkError
        }
    }
}