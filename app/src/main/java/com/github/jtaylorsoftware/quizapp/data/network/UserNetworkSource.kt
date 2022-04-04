package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * A data source for managing users by sending requests to
 * the REST API.
 */
interface UserNetworkSource {
    /**
     * Creates a new user.
     */
    suspend fun registerUser(registration: UserRegistrationDto): NetworkResult<AuthToken>

    /**
     * Attempts to sign in using the supplied username and password.
     */
    suspend fun signInUser(userCredentials: UserCredentialsDto): NetworkResult<AuthToken>

    /**
     * Gets a user's [profile data][UserDto].
     */
    suspend fun getProfile(): NetworkResult<UserDto>

    /**
     * Gets the signed-in user's list of created quizzes as listings.
     */
    suspend fun getQuizzes(): NetworkResult<List<QuizListingDto>>

    /**
     * Gets the signed-in user's list of created results as listings.
     */
    suspend fun getResults(): NetworkResult<List<QuizResultListingDto>>

    /**
     * Attempts to change the signed-in user's email.
     */
    suspend fun changeEmail(email: String): NetworkResult<Unit>

    /**
     * Changes the signed-in user's password.
     */
    suspend fun changePassword(password: String): NetworkResult<Unit>
}


/**
 * Retrofit target interface for /users API (which includes /users/auth and /users/me).
 */
interface UserService: UserNetworkSource {
    @POST("users/")
    override suspend fun registerUser(@Body registration: UserRegistrationDto): NetworkResult<AuthToken>

    @POST("users/auth")
    override suspend fun signInUser(@Body userCredentials: UserCredentialsDto): NetworkResult<AuthToken>

    @GET("users/me")
    override suspend fun getProfile(): NetworkResult<UserDto>

    @GET("users/me/quizzes")
    override suspend fun getQuizzes(): NetworkResult<List<QuizListingDto>>

    @GET("users/me/results")
    override suspend fun getResults(): NetworkResult<List<QuizResultListingDto>>

    @PUT("users/me/email")
    override suspend fun changeEmail(email: String): NetworkResult<Unit>

    @PUT("users/me/password")
    override suspend fun changePassword(password: String): NetworkResult<Unit>
}
//
//class UserNetworkSourceImpl @Inject constructor(
//    private val service: UserService
//) : UserNetworkSource {
//    override suspend fun registerUser(registration: UserRegistrationDto): NetworkResult<AuthToken> =
//        service.registerUser(registration)
//
//    override suspend fun signInUser(userCredentials: UserCredentialsDto): NetworkResult<AuthToken> =
//        service.signInUser(userCredentials)
//
//    override suspend fun getProfile(): NetworkResult<UserDto> = service.getProfile()
//
//    override suspend fun getQuizzes(): NetworkResult<List<QuizListingDto>> = service.getQuizzes()
//
//    override suspend fun getResults(): NetworkResult<List<ResultListingDto>> = service.getResults()
//
//    override suspend fun changeEmail(email: String): NetworkResult<Unit> =
//        service.changeEmail(email)
//
//    override suspend fun changePassword(password: String): NetworkResult<Unit> =
//        service.changePassword(password)
//}