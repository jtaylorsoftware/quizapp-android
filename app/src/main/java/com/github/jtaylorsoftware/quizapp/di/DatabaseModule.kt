package com.github.jtaylorsoftware.quizapp.di

import android.content.Context
import androidx.room.Room
import com.github.jtaylorsoftware.quizapp.data.local.AppDatabase
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDao
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java, "quizapp-db",
        ).build()

    @Provides
    fun provideQuizListingDao(db: AppDatabase): QuizListingDao = db.quizListingDao()

    @Provides
    fun provideResultListingDao(db: AppDatabase): QuizResultListingDao = db.resultListingDao()
}