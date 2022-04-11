package com.github.jtaylorsoftware.quizapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizListingDto
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity(tableName = "quiz_listing")
data class QuizListingEntity(
    /** ObjectId **/
    @PrimaryKey val id: String,
    val date: String = Instant.now().toString(),
    /** ObjectId **/
    val user: String = "",
    val title: String = "",
    val expiration: String = Instant.now().plus(1, ChronoUnit.DAYS).toString(),
    @ColumnInfo(name = "is_public") val isPublic: Boolean = true,
    @ColumnInfo(name = "results_count") val resultsCount: Int = 0,
    @ColumnInfo(name = "question_count") val questionCount: Int = 0,
) {
    companion object {
        fun fromDto(dto: QuizListingDto) = QuizListingEntity(
            id = dto.id,
            date = dto.date,
            user = dto.user,
            title = dto.title,
            expiration = dto.expiration,
            isPublic = dto.isPublic,
            resultsCount = dto.resultsCount,
            questionCount = dto.questionCount
        )

        fun fromDomain(domain: QuizListing) = QuizListingEntity(
            id = domain.id.value,
            date = domain.date.toString(),
            user = domain.createdBy.value,
            title = domain.title,
            expiration = domain.expiration.toString(),
            isPublic = domain.isPublic,
            resultsCount = domain.resultsCount,
            questionCount = domain.questionCount
        )
    }
}