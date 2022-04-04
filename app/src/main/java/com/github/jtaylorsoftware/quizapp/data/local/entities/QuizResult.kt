package com.github.jtaylorsoftware.quizapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultListingDto
import java.time.Instant

@Entity(tableName = "result_listing")
data class QuizResultListingEntity(
    /** ObjectId **/
    @get:JvmName("getId")
    @PrimaryKey val id: String,
    val date: String = Instant.now().toString(),
    /** ObjectId **/
    val user: String = "",
    val username: String = "",
    /** ObjectId **/
    val quiz: String = "",
    val score: Float = 0.0f,
    @ColumnInfo(name = "quiz_title") val quizTitle: String = "",
    @ColumnInfo(name = "created_by") val createdBy: String = "",
) {
    companion object {
        fun fromDto(dto: QuizResultListingDto) = QuizResultListingEntity(
            id = dto.id,
            date = dto.date,
            user = dto.user,
            username = dto.username,
            quiz = dto.quiz,
            quizTitle = dto.quizTitle,
            createdBy = dto.createdBy,
            score = dto.score
        )
    }
}