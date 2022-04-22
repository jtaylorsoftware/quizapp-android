package com.github.jtaylorsoftware.quizapp.ui.login

import com.github.jtaylorsoftware.quizapp.auth.AuthenticationState
import com.github.jtaylorsoftware.quizapp.auth.FakeAuthStateManager
import com.github.jtaylorsoftware.quizapp.data.domain.FakeUserAuthService
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.UserAuthService
import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.network.FakeUserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private lateinit var userCache: FakeUserCache
    private lateinit var networkSource: FakeUserNetworkSource
    private lateinit var service: UserAuthService
    private lateinit var viewModel: LoginViewModel
    private lateinit var authStateManager: FakeAuthStateManager
    
    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        userCache = FakeUserCache()
        networkSource = FakeUserNetworkSource()
        service = FakeUserAuthService(userCache, networkSource)
        authStateManager = FakeAuthStateManager()
        viewModel = LoginViewModel(service, authStateManager, Dispatchers.Main)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setUsername updates the uiState username text value`() = runTest {
        val username = "username123"
        viewModel.setUsername(username)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.usernameState.text,
            `is`(username)
        )
    }

    @Test
    fun `setUsername validates the username`() = runTest {
        // Symbols
        var username = "@#!2345fsd"
        viewModel.setUsername(username)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.usernameState.error,
            `is`(notNullValue())
        )

        // Length < 5
        username = "a"
        viewModel.setUsername(username)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.usernameState.error,
            `is`(notNullValue())
        )

        // Length > 12
        username = "a".repeat(13)
        viewModel.setUsername(username)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.usernameState.error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `setPassword updates the uiState password text value`() = runTest {
        val password = "password123"
        viewModel.setPassword(password)
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.passwordState.text,
            `is`(password)
        )
    }

    @Test
    fun `setPassword validates the password`() = runTest {
        // Length < 8
        var password = "a"
        viewModel.setPassword(password)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.passwordState.error,
            `is`(notNullValue())
        )

        // Length > 20
        password = "a".repeat(21)
        viewModel.setPassword(password)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.passwordState.error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `login does nothing when ui has errors`() = runTest {
        // Only user has errors
        viewModel.setUsername("!@#dsfsdf")
        viewModel.login()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
        assertThat(
            viewModel.uiState.usernameState.error,
            `is`(notNullValue())
        )

        // Only pass has errors
        viewModel.setUsername("validuser")
        viewModel.setPassword("a".repeat(21))
        viewModel.login()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
        assertThat(
            viewModel.uiState.passwordState.error,
            `is`(notNullValue())
        )


        // Both have errors
        viewModel.setUsername("!@#32fads")
        viewModel.setPassword("a".repeat(21))
        viewModel.login()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
        assertThat(
            viewModel.uiState.usernameState.error,
            `is`(notNullValue())
        )
        assertThat(
            viewModel.uiState.passwordState.error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `login sets validates state again before doing login`() = runTest {
        // Neither has a value, so both should be error because empty
        viewModel.login()
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
        assertThat(
            viewModel.uiState.usernameState.error,
            `is`(notNullValue())
        )
        assertThat(
            viewModel.uiState.passwordState.error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `login sets uiState loading flag before doing login`() = runTest {
        val mockService = spyk(service)
        coEvery { mockService.signInUser(any()) } coAnswers {
            delay(1000)
            Result.success()
        }

        viewModel = LoginViewModel(mockService, authStateManager, Dispatchers.Main)

        viewModel.setUsername("username")
        viewModel.setPassword("password")

        viewModel.login()

        runCurrent()
        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )
    }

    @Test
    fun `login notifies AuthStateManager after successful login`() = runTest {
        viewModel.setUsername("username")
        viewModel.setPassword("password")
        viewModel.login()
        advanceUntilIdle()
        assertThat(authStateManager.state, IsInstanceOf(AuthenticationState.Authenticated::class.java))
    }

    @Test
    fun `login sets uiState errors when service fails with HTTP Bad Request`() = runTest {
        viewModel.setUsername("username")
        viewModel.setPassword("password")
        networkSource.failOnNextWith(NetworkResult.HttpError(400))
        viewModel.login()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
        assertThat(
            viewModel.uiState.usernameState.error,
            `is`(notNullValue())
        )
        assertThat(
            viewModel.uiState.passwordState.error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `login sets uiState errors when service fails because of network error`() = runTest {
        viewModel.setUsername("username")
        viewModel.setPassword("password")
        networkSource.failOnNextWith(NetworkResult.NetworkError(IllegalArgumentException()))
        viewModel.login()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `login sets uiState errors when service fails because of unknown error`() = runTest {
        viewModel.setUsername("username")
        viewModel.setPassword("password")
        networkSource.failOnNextWith(NetworkResult.Unknown(IllegalArgumentException()))
        viewModel.login()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loginStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }
}