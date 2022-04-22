package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.testdata.loggedInUserEntity
import com.github.jtaylorsoftware.quizapp.testdata.loggedInUserJwt
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton


@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UserCacheModule::class]
)
object TestUserCacheModule {
    @Provides
    @Singleton
    fun provideUserCache(): UserCache =
        FakeUserCache(loggedInUserEntity, loggedInUserJwt)
}