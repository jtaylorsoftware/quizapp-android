package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.data.network.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class NetworkSourceModule {
    @Binds
    abstract fun bindQuizNetworkSource(quizService: QuizService): QuizNetworkSource

    @Binds
    abstract fun bindResultNetworkSource(resultService: QuizResultService): QuizResultNetworkSource

    @Binds
    abstract fun bindUserNetworkSource(userService: UserService): UserNetworkSource
}