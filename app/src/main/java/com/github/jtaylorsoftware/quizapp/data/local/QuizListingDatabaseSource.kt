package com.github.jtaylorsoftware.quizapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity

/**
 * A data source that persists [quiz listings][QuizListingEntity] to a local
 * database.
 */
interface QuizListingDatabaseSource {
    /**
     * Gets one [QuizListingEntity] by its id.
     */
    suspend fun getById(id: String): QuizListingEntity?

    /**
     * Gets all [QuizListingEntity] created by a user.
     */
    suspend fun getAllCreatedByUser(user: String): List<QuizListingEntity>

    /**
     * Saves multiple listings.
     */
    suspend fun insertAll(listings: List<QuizListingEntity>)

    /**
     * Deletes one listing by its quiz id.
     */
    suspend fun delete(id: String)

    /**
     * Deletes all listings by its user id.
     */
    suspend fun deleteAllByUser(user: String)

    /**
     * Deletes all previously saved listings.
     */
    suspend fun deleteAll()
}

@Dao
interface QuizListingDao : QuizListingDatabaseSource {
    @Query("SELECT * FROM quiz_listing WHERE id = :id")
    override suspend fun getById(id: String): QuizListingEntity?

    @Query("SELECT * FROM quiz_listing WHERE user = :user")
    override suspend fun getAllCreatedByUser(user: String): List<QuizListingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insertAll(listings: List<QuizListingEntity>)

    @Query("DELETE FROM quiz_listing")
    override suspend fun deleteAll()

    @Query("DELETE FROM quiz_listing WHERE id = :id")
    override suspend fun delete(id: String)

    @Query("DELETE FROM quiz_listing WHERE user = :user")
    override suspend fun deleteAllByUser(user: String)
}