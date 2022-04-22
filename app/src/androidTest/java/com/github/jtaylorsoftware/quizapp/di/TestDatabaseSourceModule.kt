package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.testdata.loggedInUserQuizListingEntities
import com.github.jtaylorsoftware.quizapp.testdata.otherUserQuizResultListingEntities
import com.github.jtaylorsoftware.quizapp.testdata.testResultEntities
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [DatabaseSourceModule::class]
)
object TestDatabaseSourceModule {
    @Provides
    fun provideQuizListingDatabaseSource(): QuizListingDatabaseSource =
        FakeQuizListingDatabaseSource(loggedInUserQuizListingEntities)

    @Provides
    fun provideResultListingDatabaseSource(): QuizResultListingDatabaseSource =
        FakeQuizResultListingDatabaseSource(testResultEntities)
}