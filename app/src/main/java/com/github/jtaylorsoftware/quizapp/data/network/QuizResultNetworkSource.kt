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
    suspend fun getAllForQuiz(quiz: String): NetworkResult<List<QuizResultDto>>

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
    suspend fun getAllListingForQuiz(quiz: String): NetworkResult<List<QuizResultListingDto>>

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
    override suspend fun getAllForQuiz(@Query("quiz") quiz: String): NetworkResult<List<QuizResultDto>>

    @GET("results?format=listing")
    override suspend fun getListingForQuizByUser(
        @Query("quiz") quiz: String,
        @Query("user") user: String
    ): NetworkResult<QuizResultListingDto>

    @GET("results?format=listing")
    override suspend fun getAllListingForQuiz(@Query("quiz") quiz: String): NetworkResult<List<QuizResultListingDto>>

    @POST("results")
    override suspend fun createResultForQuiz(
        @Body responses: QuizFormResponsesDto,
        @Query("quiz") quiz: String
    ): NetworkResult<ObjectIdResponse>
}
//
//class ResultNetworkSourceImpl @Inject constructor(
//    private val service: ResultService
//) : ResultNetworkSource {
//    override suspend fun getForQuizByUser(
//        quiz: String,
//        user: String
//    ): NetworkResult<ResultDto> = service.getForQuizByUser(quiz, user)
//
//
//    override suspend fun getAllForQuiz(quiz: String): NetworkResult<List<ResultDto>> = service.getAllForQuiz(quiz)
//
//    override suspend fun getListingForQuizByUser(
//        quiz: String,
//        user: String
//    ): NetworkResult<ResultListingDto> = service.getListingForQuizByUser(quiz, user)
//
//    override suspend fun getAllListingForQuiz(quiz: String): NetworkResult<List<ResultListingDto>> = service.getAllListingForQuiz(quiz)
//
//    override suspend fun createResultForQuiz(
//        responses: QuizFormResponsesDto,
//        quiz: String
//    ): NetworkResult<ObjectIdResponse> = service.createResultForQuiz(responses, quiz)
//}