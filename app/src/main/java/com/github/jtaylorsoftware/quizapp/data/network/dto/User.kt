package com.github.jtaylorsoftware.quizapp.data.network.dto

import com.github.jtaylorsoftware.quizapp.data.domain.models.UserCredentials
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserRegistration
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "_id")
    val id: String,
    val date: String = Instant.now().toString(),
    val username: String = "",
    val email: String = "",
    val quizzes: List<String> = emptyList(),
    val results: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class UserRegistrationDto(
    val username: String = "",
    val email: String = "",
    val password: String = "",
) {
    companion object {
        fun fromDomain(domain: UserRegistration) = UserRegistrationDto(
            username = domain.username, email = domain.email, password = domain.password
        )
    }
}

@JsonClass(generateAdapter = true)
data class UserCredentialsDto(
    val username: String = "",
    val password: String = "",
) {
    companion object {
        fun fromDomain(domain: UserCredentials) = UserCredentialsDto(
            username = domain.username, password = domain.password
        )
    }
}

@JsonClass(generateAdapter = true)
data class AuthToken(
    val token: String = "",
)

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

/**
 * The response from the API when returning the ID of a newly created resource.
 */
@JsonClass(generateAdapter = true)
data class ObjectIdResponse(
    val id: String
)