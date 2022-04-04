package com.github.jtaylorsoftware.quizapp.di

import android.content.Context
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.data.local.UserSharedPrefCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object UserCacheModule {
    @Provides
    fun provideUserCache(@ApplicationContext context: Context): UserCache =
        UserSharedPrefCache(context)
}