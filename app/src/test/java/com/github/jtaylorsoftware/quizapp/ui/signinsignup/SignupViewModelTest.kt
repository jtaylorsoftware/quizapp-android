package com.github.jtaylorsoftware.quizapp.ui.signinsignup

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
class SignupViewModelTest {
    private lateinit var userCache: FakeUserCache
    private lateinit var networkSource: FakeUserNetworkSource
    private lateinit var service: UserAuthService
    private lateinit var viewModel: SignupViewModel
    private lateinit var authStateManager: FakeAuthStateManager

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        userCache = FakeUserCache()
        networkSource = FakeUserNetworkSource()
        service = FakeUserAuthService(userCache, networkSource)
        authStateManager = FakeAuthStateManager()
        viewModel = SignupViewModel(service, authStateManager, Dispatchers.Main)
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
    fun `setPassword updates the uiState username text value`() = runTest {
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
    fun `setEmail updates the uiState email text value`() = runTest {
        val email = "email@example.com"
        viewModel.setEmail(email)
        advanceUntilIdle()
        val text = viewModel.uiState.emailState.text
        assertThat(text, `is`(email))
    }

    @Test
    fun `setEmail validates the email`() = runTest {
        // Empty
        var email = ""
        viewModel.setEmail(email)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.emailState.error,
            `is`(notNullValue())
        )

        // Not email
        email = "alsdf345kal@"
        viewModel.setEmail(email)
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.emailState.error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `register does nothing when ui has errors`() = runTest {
        // Only user has errors
        viewModel.setUsername("!@#dsfsdf")
        viewModel.setPassword("password")
        viewModel.setEmail("email@example.com")
        advanceUntilIdle()

        viewModel.register()
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.registerStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )

        // Only pass has errors
        viewModel.setUsername("validuser")
        viewModel.setPassword("a".repeat(21))
        viewModel.setEmail("email@example.com")
        advanceUntilIdle()

        viewModel.register()
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.registerStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )

        // Both have errors
        viewModel.setUsername("!@#32fads")
        viewModel.setPassword("a".repeat(21))
        advanceUntilIdle()

        viewModel.register()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.registerStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `register sets validates state again before doing register`() = runTest {
        // Neither has a value, so both should be error because empty
        viewModel.register()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.registerStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `register sets uiState loading flag before doing register`() = runTest {
        val mockService = spyk(service)
        coEvery { mockService.registerUser(any()) } coAnswers {
            delay(1000)
            Result.success()
        }
        viewModel = SignupViewModel(mockService, authStateManager, Dispatchers.Main)

        viewModel.setUsername("username")
        viewModel.setPassword("password")
        viewModel.setEmail("email@example.com")
        advanceUntilIdle()

        viewModel.register()
        advanceTimeBy(100)
        assertThat(
            viewModel.uiState.registerStatus,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )
    }

    @Test
    fun `register sets registerStatus to Success and notifies AuthStateManager on success`() =
        runTest {
            viewModel.setUsername("username")
            viewModel.setEmail("email@example.com")
            viewModel.setPassword("password")
            viewModel.register()
            advanceUntilIdle()
            assertThat(authStateManager.state, IsInstanceOf(AuthenticationState.Authenticated::class.java))
            assertThat(viewModel.uiState.registerStatus, IsInstanceOf(LoadingState.Success::class.java))
        }

    @Test
    fun `register sets uiState errors when service fails with HTTP Bad Request`() = runTest {
        viewModel.setUsername("username")
        viewModel.setEmail("email@example.com")
        viewModel.setPassword("password")
        advanceUntilIdle()

        networkSource.failOnNextWith(NetworkResult.HttpError(400))

        viewModel.register()
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.registerStatus,
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
    fun `register sets uiState errors when service fails because of network error`() = runTest {
        viewModel.setUsername("username")
        viewModel.setEmail("email@example.com")
        viewModel.setPassword("password")
        advanceUntilIdle()

        networkSource.failOnNextWith(NetworkResult.NetworkError(IllegalArgumentException()))
        viewModel.register()
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.registerStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `register sets uiState errors when service fails because of unknown error`() = runTest {
        viewModel.setUsername("username")
        viewModel.setEmail("email@example.com")
        viewModel.setPassword("password")
        advanceUntilIdle()

        networkSource.failOnNextWith(NetworkResult.Unknown(IllegalArgumentException()))
        viewModel.register()
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.registerStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }
}