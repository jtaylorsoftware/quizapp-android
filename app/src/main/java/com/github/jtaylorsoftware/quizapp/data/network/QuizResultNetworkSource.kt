package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * A data source for performing CRUD operations on the Results REST API.
 */
interface QuizResultNetworkSource {
    /**
     * Gets one user's [QuizResultDto] for a Quiz, if the signed-in user submitted
     * the result or created the Quiz.
     */
    suspend fun getForQuizByUser(quiz: String, user: String): NetworkResult<QuizResultDto>

    /**
     * Gets all users' results for a Quiz, if the signed-in user created the
     * Quiz.
     */
    suspend fun getAllForQuiz(quiz: String): NetworkResult<QuizResultsForQuizDto>

    /**
     * Gets one user's [QuizResultListingDto] for a Quiz, if the signed-in user submitted
     * the result or created the Quiz.
     */
    suspend fun getListingForQuizByUser(
        quiz: String,
        user: String
    ): NetworkResult<QuizResultListingDto>

    /**
     * Gets all [QuizResultListingDto] for a Quiz, if the signed-in user created the Quiz.
     */
    suspend fun getAllListingForQuiz(quiz: String): NetworkResult<QuizResultListingsForQuizDto>

    /**
     * Submits a user's [response][QuestionResponseDto] to a Quiz. The API will respond with
     * an error status if the user already submitted a response.
     */
    suspend fun createResultForQuiz(
        responses: QuizFormResponsesDto,
        quiz: String
    ): NetworkResult<ObjectIdResponse>
}

/**
 * Retrofit target interface for /results API.
 */
interface QuizResultService : QuizResultNetworkSource {
    @GET("results?format=full")
    override suspend fun getForQuizByUser(
        @Query("quiz") quiz: String,
        @Query("user") user: String
    ): NetworkResult<QuizResultDto>

    @GET("results?format=full")
    override suspend fun getAllForQuiz(@Query("quiz") quiz: String): NetworkResult<QuizResultsForQuizDto>

    @GET("results?format=listing")
    override suspend fun getListingForQuizByUser(
        @Query("quiz") quiz: String,
        @Query("user") user: String
    ): NetworkResult<QuizResultListingDto>

    @GET("results?format=listing")
    override suspend fun getAllListingForQuiz(@Query("quiz") quiz: String): NetworkResult<QuizResultListingsForQuizDto>

    @POST("results")
    override suspend fun createResultForQuiz(
        @Body responses: QuizFormResponsesDto,
        @Query("quiz") quiz: String
    ): NetworkResult<ObjectIdResponse>
}