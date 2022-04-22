package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.QuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.data.network.QuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.QuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.UserNetworkSource
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {
    @Provides
    fun provideUserRepository(
        userCache: UserCache,
        userNetworkSource: UserNetworkSource,
        quizListingDatabaseSource: QuizListingDatabaseSource,
        quizResultListingDatabaseSource: QuizResultListingDatabaseSource,
    ): UserRepository = FakeUserRepository(
        userCache, userNetworkSource, quizListingDatabaseSource, quizResultListingDatabaseSource
    )

    @Provides
    fun provideUserAuthService(
        userCache: UserCache,
        userNetworkSource: UserNetworkSource,
    ): UserAuthService = FakeUserAuthService(
        userCache, userNetworkSource
    )

    @Provides
    fun provideQuizRepository(
        quizListingDatabaseSource: QuizListingDatabaseSource,
        quizNetworkSource: QuizNetworkSource
    ): QuizRepository = FakeQuizRepository(
        quizListingDatabaseSource, quizNetworkSource
    )

    @Provides
    fun provideQuizResultRepository(
        quizResultListingDatabaseSource: QuizResultListingDatabaseSource,
        quizResultNetworkSource: QuizResultNetworkSource
    ): QuizResultRepository = FakeQuizResultRepository(
        quizResultListingDatabaseSource, quizResultNetworkSource
    )
}