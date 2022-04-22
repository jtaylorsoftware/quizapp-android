package com.github.jtaylorsoftware.quizapp.data.network.dto

import com.squareup.moshi.JsonClass

/**
 * Contains detailed information about an error in an API call.
 */
@JsonClass(generateAdapter = true)
data class ApiError(
    /**
     * Name of the field in the response body causing an error.
     */
    val field: String? = null,
    /**
     * Description of the problem.
     */
    val message: String? = null,
    /**
     * The value of [field] that caused the problem.
     */
    val value: Any? = null,
    /**
     * The expected value of [field], if there is a predictable
     * constraint on the field that has to be met.
     */
    val expected: Any? = null,
    /**
     * For collection types, indicates which element caused the problem.
     */
    val index: Int? = null,
)

/**
 * The response from the API when there are errors.
 */
@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val errors: List<ApiError> = emptyList()
)