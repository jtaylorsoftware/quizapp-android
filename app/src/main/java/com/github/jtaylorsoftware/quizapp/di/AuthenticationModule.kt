package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.auth.*
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthenticationModule {
    @Provides
    @Singleton
    fun provideAuthenticationStateManager(userCache: UserCache): AuthenticationStateManager =
        AuthenticationStateManager(stateSource = mutableAuthenticationStateSource(
            // Set initial AuthenticationState based on whether the user was signed in
            // the last time they were in the app (currently without checking expiration of the JWT)
            initialState = when(userCache.loadToken()) {
                null -> AuthenticationState.RequireAuthentication
                else -> AuthenticationState.Authenticated
            }
        ))

    @Provides
    @Singleton
    fun provideAuthenticationStateSource(authenticationEventProducer: AuthenticationStateManager): AuthenticationStateSource =
        authenticationEventProducer

    @Provides
    @Singleton
    fun provideAuthenticationEventProducer(authenticationStateManager: AuthenticationStateManager): AuthenticationEventProducer =
        authenticationStateManager

}