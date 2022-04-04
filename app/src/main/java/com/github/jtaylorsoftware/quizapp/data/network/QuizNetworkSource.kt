package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.ObjectIdResponse
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizFormDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizListingDto
import retrofit2.http.*

/**
 * A data source for Quiz data coming over the network from the REST API.
 */
interface QuizNetworkSource {
    /**
     * Gets a [QuizDto] by its id.
     */
    suspend fun getById(id: String): NetworkResult<QuizDto>

    /**
     * Gets a [QuizListingDto] by its id.
     */
    suspend fun getListingById(id: String): NetworkResult<QuizListingDto>

    /**
     * Gets a [QuizFormDto] for a Quiz with the matching id.
     */
    suspend fun getForm(id: String): NetworkResult<QuizFormDto>

    /**
     * Creates a new Quiz from the given [QuizDto].
     */
    suspend fun createQuiz(quizDto: QuizDto): NetworkResult<ObjectIdResponse>

    /**
     * Edits an existing Quiz by merging it with the given [QuizDto].
     * Certain fields cannot be edited after creation - the API will
     * return errors in such a case.
     */
    suspend fun updateQuiz(id: String, updates: QuizDto): NetworkResult<Unit>

    /**
     * Deletes a single Quiz.
     */
    suspend fun delete(id: String): NetworkResult<Unit>
}

/**
 * Retrofit target interface for /quizzes API.
 */
interface QuizService : QuizNetworkSource {
    @GET("quizzes/{id}?format=full")
    override suspend fun getById(@Path("id") id: String): NetworkResult<QuizDto>

    @GET("quizzes/{id}?format=listing")
    override suspend fun getListingById(@Path("id") id: String): NetworkResult<QuizListingDto>

    @GET("quizzes/{id}/form")
    override suspend fun getForm(@Path("id") id: String): NetworkResult<QuizFormDto>

    @POST("quizzes/")
    override suspend fun createQuiz(@Body quizDto: QuizDto): NetworkResult<ObjectIdResponse>

    @PUT("quizzes/{id}/edit")
    override suspend fun updateQuiz(
        @Path("id") id: String,
        @Body updates: QuizDto
    ): NetworkResult<Unit>

    @DELETE("quizzes/{id}")
    override suspend fun delete(@Path("id") id: String): NetworkResult<Unit>
}
//
//class QuizNetworkSourceImpl @Inject constructor(
//    private val service: QuizService
//) : QuizNetworkSource {
//    override suspend fun getById(id: String): NetworkResult<QuizDto> = service.getById(id)
//
//    override suspend fun getListingById(id: String): NetworkResult<QuizListingDto> =
//        service.getListingById(id)
//
//    override suspend fun getForm(id: String): NetworkResult<QuizFormDto> = service.getForm(id)
//
//    override suspend fun createQuiz(quizDto: QuizDto): NetworkResult<ObjectIdResponse> =
//        service.createQuiz(quizDto)
//
//    override suspend fun updateQuiz(
//        id: String,
//        updates: QuizDto
//    ): NetworkResult<Unit> = service.updateQuiz(id, updates)
//
//    override suspend fun delete(id: String): NetworkResult<Unit> = service.delete(id)
//}

