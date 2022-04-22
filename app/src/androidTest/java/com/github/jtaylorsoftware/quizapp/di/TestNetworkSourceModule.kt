package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.data.network.*
import com.github.jtaylorsoftware.quizapp.testdata.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [NetworkSourceModule::class]
)
object TestNetworkSourceModule {
    @Provides
    fun provideQuizNetworkSource(): QuizNetworkSource =
        FakeQuizNetworkSource(testQuizDtos)

    @Provides
    fun provideResultNetworkSource(): QuizResultNetworkSource =
        FakeQuizResultNetworkSource(testResultDtos)

    @Provides
    fun provideUserNetworkSource(): UserNetworkSource =
        FakeUserNetworkSource(
            listOf(loggedInUserWithPassword),
            loggedInUserQuizListingDtos,
            loggedInUserQuizResultListingDtos
        )
}