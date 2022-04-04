package com.github.jtaylorsoftware.quizapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity

@Database(
    entities = [QuizListingEntity::class, QuizResultListingEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quizListingDao(): QuizListingDao

    abstract fun resultListingDao(): QuizResultListingDao
}