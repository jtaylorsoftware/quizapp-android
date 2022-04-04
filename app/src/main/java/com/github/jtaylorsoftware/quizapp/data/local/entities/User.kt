package com.github.jtaylorsoftware.quizapp.data.local.entities

import com.github.jtaylorsoftware.quizapp.data.network.dto.UserDto
import java.time.Instant

data class UserEntity(
    /** ObjectId **/
    val id: String,
    val date: String = Instant.now().toString(),
    val username: String = "",
    val email: String = "'",
    val quizzes: List<String> = emptyList(),
    val results: List<String> = emptyList(),
) {
    companion object {
        fun fromDto(dto: UserDto) = UserEntity(
            id = dto.id,
            date = dto.date,
            username = dto.username,
            email = dto.email,
            quizzes = dto.quizzes.toList(),
            results = dto.results.toList()
        )
    }
}