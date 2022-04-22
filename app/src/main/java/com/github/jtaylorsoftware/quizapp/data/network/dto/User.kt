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

@JsonClass(generateAdapter = true)
data class ChangeEmailRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    val password: String
)

/**
 * The response from the API when returning the ID of a newly created resource.
 */
@JsonClass(generateAdapter = true)
data class ObjectIdResponse(
    val id: String
)