package com.github.jtaylorsoftware.quizapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity

/**
 * A data source that persists [result listings][QuizResultListingEntity] to a local
 * database.
 */
interface QuizResultListingDatabaseSource {
    /**
     * Gets one [QuizResultListingEntity] by its id.
     */
    suspend fun getById(id: String): QuizResultListingEntity?

    /**
     * Gets all [QuizResultListingEntity] created by a user.
     */
    suspend fun getAllByUser(user: String): List<QuizResultListingEntity>

    /**
     * Gets all [QuizResultListingEntity] for a quiz.
     */
    suspend fun getAllByQuiz(quiz: String): List<QuizResultListingEntity>

    /**
     * Gets a [QuizResultListingEntity] created by a user for a quiz.
     */
    suspend fun getByQuizAndUser(quiz: String, user: String): QuizResultListingEntity?

    /**
     * Saves multiple listings.
     */
    suspend fun insertAll(listings: List<QuizResultListingEntity>)

    /**
     * Deletes all previously saved listings.
     */
    suspend fun deleteAll()
}

@Dao
interface QuizResultListingDao : QuizResultListingDatabaseSource {
    @Query("SELECT * FROM result_listing WHERE id = :id")
    override suspend fun getById(id: String): QuizResultListingEntity?

    @Query("SELECT * FROM result_listing WHERE user = :user")
    override suspend fun getAllByUser(user: String): List<QuizResultListingEntity>

    @Query("SELECT * FROM result_listing WHERE quiz = :quiz")
    override suspend fun getAllByQuiz(quiz: String): List<QuizResultListingEntity>

    @Query("SELECT * FROM result_listing WHERE quiz = :quiz AND user = :user")
    override suspend fun getByQuizAndUser(quiz: String, user: String): QuizResultListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insertAll(listings: List<QuizResultListingEntity>)

    @Query("DELETE FROM result_listing")
    override suspend fun deleteAll()
}
