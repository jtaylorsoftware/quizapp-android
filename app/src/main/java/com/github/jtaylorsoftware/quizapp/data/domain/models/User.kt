package com.github.jtaylorsoftware.quizapp.data.domain.models

import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserDto
import com.github.jtaylorsoftware.quizapp.util.toInstant
import java.time.Instant

/**
 * Data for the signed-in user.
 */
data class User(
    val id: ObjectId = ObjectId(),
    val date: Instant = Instant.now(),
    val username: String = "",
    val email: String = "",

    /**
     * List of the ids of the user's created quizzes.
     */
    val quizzes: List<ObjectId> = emptyList(),

    /**
     * List of the ids of the user's quiz responses.
     */
    val results: List<ObjectId> = emptyList(),
) {
    companion object {
        fun fromDto(dto: UserDto) = User(
            id = ObjectId(dto.id),
            date = dto.date.toInstant() ?: Instant.now(),
            username = dto.username,
            email = dto.email,
            quizzes = dto.quizzes.map { id -> ObjectId(id) },
            results = dto.results.map { id -> ObjectId(id) },
        )

        fun fromEntity(entity: UserEntity) = User(
            id = ObjectId(entity.id),
            date = entity.date.toInstant() ?: Instant.now(),
            username = entity.username,
            email = entity.email,
            quizzes = entity.quizzes.map { id -> ObjectId(id) },
            results = entity.results.map { id -> ObjectId(id) },
        )
    }
}

/**
 * Data necessary to register a new User.
 */
data class UserRegistration(
    val username: String,
    val email: String,
    val password: String,
)


/**
 * Data necessary for a User to sign in.
 */
data class UserCredentials(
    val username: String,
    val password: String
)