package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDao
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDao
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDatabaseSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class DatabaseSourceModule {
    // Since the DAO implement their respective DatabaseSource interfaces, can use them directly as
    // "default" Bindings

    @Binds
    abstract fun bindQuizListingDatabaseSource(dao: QuizListingDao): QuizListingDatabaseSource

    @Binds
    abstract fun bindResultListingDatabaseSource(dao: QuizResultListingDao): QuizResultListingDatabaseSource
}