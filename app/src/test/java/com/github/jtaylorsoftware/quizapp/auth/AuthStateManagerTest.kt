package com.github.jtaylorsoftware.quizapp.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthStateManagerTest {
    private lateinit var stateManager: AuthenticationStateManager

    @Test
    fun `onAuthenticated should cause state to become AuthenticationState Authenticated`() = runTest {
        stateManager = AuthenticationStateManager(mutableAuthenticationStateSource(AuthenticationState.RequireAuthentication))
        stateManager.onAuthenticated()
        assertThat(
            stateManager.state,
            `is`(AuthenticationState.Authenticated)
        )
    }

    @Test
    fun `onRequireLogin should cause state to become AuthenticationState RequireAuthentication`() = runTest {
        stateManager = AuthenticationStateManager(mutableAuthenticationStateSource(AuthenticationState.Authenticated))
        stateManager.onRequireLogIn()
        assertThat(
            stateManager.state,
            `is`(AuthenticationState.RequireAuthentication)
        )
    }
}