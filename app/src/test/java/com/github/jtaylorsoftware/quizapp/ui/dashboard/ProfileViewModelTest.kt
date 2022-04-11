package com.github.jtaylorsoftware.quizapp.ui.dashboard

import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import com.github.jtaylorsoftware.quizapp.data.network.FakeUserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.UserWithPassword
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizListingDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultListingDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserDto
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private lateinit var userCache: FakeUserCache
    private lateinit var networkSource: FakeUserNetworkSource
    private lateinit var repository: UserRepository
    private lateinit var quizListingDbSource: QuizListingDatabaseSource
    private lateinit var quizResultListingDbSource: FakeQuizResultListingDatabaseSource
    private lateinit var service: UserAuthService
    private lateinit var viewModel: ProfileViewModel

    private val userId = ObjectId("aewirojadlkflzmdfakl")
    private val userDto = UserDto(id = userId.value, email = "emailOne@email.com")
    private val quizListingDtos = mutableListOf(
        QuizListingDto(id = "a290fadda09da39kjfnm", user = userId.value)
    )
    private val resultListingDtos = mutableListOf(
        QuizResultListingDto(id = "a290fadda09da39kjfnm", user = userId.value)
    )

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        userCache = FakeUserCache(UserEntity.fromDto(userDto))
        networkSource =
            FakeUserNetworkSource(
                data = listOf(UserWithPassword(userDto, "password")),
                quizzes = quizListingDtos,
                results = resultListingDtos
            )
        quizListingDbSource = FakeQuizListingDatabaseSource()
        quizResultListingDbSource = FakeQuizResultListingDatabaseSource()
        repository = FakeUserRepository(
            userCache,
            networkSource,
            quizListingDbSource,
            quizResultListingDbSource
        )
        service = FakeUserAuthService(userCache, networkSource)
        viewModel = ProfileViewModel(repository, service, Dispatchers.Main)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load profile data immediately`() = runTest {
        // Should first be Loading, before initial refresh
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(ProfileUiState.NoProfile::class.java)
        )

        advanceUntilIdle()
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(ProfileUiState.Profile::class.java)
        )
    }

    @Test
    fun `should set uiState to RequireSignIn when getting profile results in Http Unauthorized`() =
        runTest {
            // local cache has no value, so force error in network to cause Unauthorized
            networkSource.failOnNextWith(NetworkResult.HttpError(401))

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(ProfileUiState.RequireSignIn::class.java)
            )
        }

    @Test
    fun `refresh should do nothing if already refreshing data`() = runTest {
        val mockRepository = spyk(repository)
        coEvery { mockRepository.getProfile() } coAnswers {
            repository.getProfile()
        }

        viewModel = ProfileViewModel(mockRepository, service, Dispatchers.Main)
        // Finish initial load - call #1
        advanceUntilIdle()

        // Do first refresh - call #2, sets loading = true
        viewModel.refresh()
        // Immediately try another - would be call #3
        viewModel.refresh()
        advanceUntilIdle()

        // Should be 2 calls
        coVerify(exactly = 2) {
            mockRepository.getProfile()
        }
        confirmVerified(mockRepository)
    }

    @Test
    fun `refresh should get fresh profile from repository`() = runTest {
        val mockUser = mockk<UserDto>(relaxed = true)
        val userData = mutableListOf(UserWithPassword(mockUser, "password"))
        networkSource =
            FakeUserNetworkSource(
                data = userData,
                quizzes = quizListingDtos,
                results = resultListingDtos
            )
        userCache = FakeUserCache(user = UserEntity.fromDto(userDto))
        repository = FakeUserRepository(
            userCache,
            networkSource,
            quizListingDbSource,
            quizResultListingDbSource
        )
        service = FakeUserAuthService(userCache, networkSource)
        viewModel = ProfileViewModel(repository, service, Dispatchers.Main)

        every { mockUser.id } returns userDto.id
        every { mockUser.email } returns "emailOne@example.com" andThen "emailTwo@example.com"

        // Should be initial value
        viewModel.refresh()
        advanceUntilIdle()

        // Should be the initial value - "emailOne"
        assertThat(
            (viewModel.uiState.value as ProfileUiState.Profile).data.email,
            `is`("emailOne@example.com")
        )

        // Refresh
        viewModel.refresh()
        advanceUntilIdle()

        // Should now be new value - "emailTwo" (latest value returned from networkSource)
        assertThat(
            (viewModel.uiState.value as ProfileUiState.Profile).data.email,
            `is`("emailTwo@example.com")
        )
    }

    @Test
    fun `setEmail updates the uiState email text value`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        val email = "email@example.com"
        viewModel.setEmail(email)
        advanceUntilIdle()

        val text = (viewModel.uiState.value as ProfileUiState.Editor).emailState.text
        assertThat(text, `is`(email))
    }

    @Test
    fun `setEmail validates the email`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // Empty
        var email = ""
        viewModel.setEmail(email)
        advanceUntilIdle()

        var error = (viewModel.uiState.value as ProfileUiState.Editor).emailState.error
        assertThat(error, `is`(notNullValue()))

        // Not email
        email = "alsdf345kal@"
        viewModel.setEmail(email)
        advanceUntilIdle()

        error = (viewModel.uiState.value as ProfileUiState.Editor).emailState.error
        assertThat(error, `is`(notNullValue()))
    }

    @Test
    fun `submitEmail does nothing when emailState has errors`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        viewModel.setEmail("adflkadjk")
        viewModel.submitEmail()

        // Should do nothing since it has errors
        assertThat(
            (viewModel.uiState.value as ProfileUiState.Editor).loading,
            IsInstanceOf(LoadingState.AwaitingAction::class.java)
        )
    }

    @Test
    fun `submitEmail validates email again before calling service`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // empty email because it hasn't been set, so validation fails
        viewModel.submitEmail()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState.value as ProfileUiState.Editor).loading,
            IsInstanceOf(LoadingState.AwaitingAction::class.java)
        )
    }

    @Test
    fun `submitEmail sets loading flag before calling service`() = runTest {
        // Setup with a mocked authService that adds some delay so order can be controlled
        service = FakeUserAuthService(userCache, networkSource)

        val mockService = spyk(service)
        coEvery { mockService.changeEmail(any()) } coAnswers {
            delay(1000)
            Result.success()
        }

        viewModel = ProfileViewModel(repository, mockService, Dispatchers.Main)
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // set valid email
        viewModel.setEmail("email@example.com")
        viewModel.submitEmail()
        advanceTimeBy(100)

        assertThat(
            (viewModel.uiState.value as ProfileUiState.Editor).loading,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )
    }

    @Test
    fun `submitEmail sets emailState error when service fails with HTTP error`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // force an error
        networkSource.failOnNextWith(NetworkResult.HttpError(400))

        // set email
        viewModel.setEmail("email@example.com")

        // send changes
        viewModel.submitEmail()
        advanceUntilIdle()

        // Should complete loading
        val state = (viewModel.uiState.value as ProfileUiState.Editor)
        assertThat(state.loading, IsInstanceOf(LoadingState.AwaitingAction::class.java))

        // Should have error in emailState
        assertThat(state.emailState.error, `is`(notNullValue()))
    }

    @Test
    fun `submitEmail sets uiState loading to Error when service fails with network error`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // set valid email
        viewModel.setEmail("email@example.com")

        // force an error
        networkSource.failOnNextWith(NetworkResult.NetworkError(IllegalArgumentException()))
        viewModel.submitEmail()
        advanceUntilIdle()

        val state = (viewModel.uiState.value as ProfileUiState.Editor)
        assertThat(state.loading, IsInstanceOf(LoadingState.Error::class.java))
    }

    @Test
    fun `setPassword updates the uiState username text value`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        val password = "password123"
        viewModel.setPassword(password)
        advanceUntilIdle()

        val text = (viewModel.uiState.value as ProfileUiState.Editor).passwordState.text
        assertThat(text, `is`(password))
    }

    @Test
    fun `setPassword validates the password`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // Length < 8
        var password = "a"
        viewModel.setPassword(password)
        advanceUntilIdle()

        var error = (viewModel.uiState.value as ProfileUiState.Editor).passwordState.error
        assertThat(error, `is`(notNullValue()))

        // Length > 20
        password = "a".repeat(21)
        viewModel.setPassword(password)
        advanceUntilIdle()

        error = (viewModel.uiState.value as ProfileUiState.Editor).passwordState.error
        assertThat(error, `is`(notNullValue()))
    }

    @Test
    fun `submitPassword does nothing when passwordState has errors`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        viewModel.setPassword("a")
        viewModel.submitPassword()
        advanceUntilIdle()

        val state = (viewModel.uiState.value as ProfileUiState.Editor)
        assertThat(state.loading, IsInstanceOf(LoadingState.AwaitingAction::class.java))
    }

    @Test
    fun `submitPassword validates password again before calling service`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // Should do nothing since it has errors
        viewModel.submitPassword()
        assertThat(
            (viewModel.uiState.value as ProfileUiState.Editor).loading,
            IsInstanceOf(LoadingState.AwaitingAction::class.java)
        )
    }

    @Test
    fun `submitPassword sets loading flag before calling service`() = runTest {
        // Setup with a mocked authService that adds some delay so order can be controlled
        service = FakeUserAuthService(userCache, networkSource)

        val mockService = spyk(service)
        coEvery { mockService.changePassword(any()) } coAnswers {
            delay(1000)
            Result.success()
        }

        viewModel = ProfileViewModel(repository, mockService, Dispatchers.Main)
        advanceUntilIdle()

        // Set to correct screen
        viewModel.openEditor()
        advanceUntilIdle()

        viewModel.setPassword("a".repeat(9))
        viewModel.submitPassword()
        advanceTimeBy(100)

        assertThat(
            (viewModel.uiState.value as ProfileUiState.Editor).loading,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )
    }

    @Test
    fun `submitPassword sets passwordState error when service fails with HTTP error`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // set valid password
        viewModel.setEmail("password!23")

        // force an error
        networkSource.failOnNextWith(NetworkResult.HttpError(400))
        viewModel.submitPassword()
        advanceUntilIdle()

        val state = (viewModel.uiState.value as ProfileUiState.Editor)

        // Should complete loading
        assertThat(
            (viewModel.uiState.value as ProfileUiState.Editor).loading,
            IsInstanceOf(LoadingState.AwaitingAction::class.java)
        )

        // Should have error in emailState
        assertThat(state.emailState.error, `is`(notNullValue()))
    }

    @Test
    fun `submitPassword sets uiState error when service fails with network error`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        // set valid password
        viewModel.setPassword("password!23")

        // force an error
        networkSource.failOnNextWith(NetworkResult.NetworkError(IllegalStateException()))
        viewModel.submitPassword()
        advanceUntilIdle()

        val state = (viewModel.uiState.value as ProfileUiState.Editor)
        assertThat(state.loading, IsInstanceOf(LoadingState.Error::class.java))
    }

    @Test
    fun `openEditor should set TextFieldStates to non-null values`() = runTest {
        advanceUntilIdle()
        viewModel.openEditor()
        advanceUntilIdle()

        val state = (viewModel.uiState.value as ProfileUiState.Editor)
        assertThat(state.emailState, `is`(notNullValue()))
        assertThat(state.passwordState, `is`(notNullValue()))
    }

    @Test
    fun `closeEditor should set TextFieldStates to null values`() = runTest {
        advanceUntilIdle()

        viewModel.openEditor()
        runCurrent()

        viewModel.closeEditor()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value, IsInstanceOf(ProfileUiState.Profile::class.java))
    }

    @Test
    fun `UiState is RequireSignIn if ViewModelState has unauthorized flag set`() = runTest {
        val state = ProfileViewModelState(unauthorized = true)
        val uiState = ProfileUiState.fromViewModelState(state)
        assertThat(uiState, IsInstanceOf(ProfileUiState.RequireSignIn::class.java))
    }

    @Test
    fun `UiState is NoProfile if ViewModelState isLoading and screen data is null`() = runTest {
        val state = ProfileViewModelState(
            loading = true,
            unauthorized = false,
            data = null,
        )
        val uiState = ProfileUiState.fromViewModelState(state)
        assertThat(uiState, IsInstanceOf(ProfileUiState.NoProfile::class.java))
    }

    @Test
    fun `UiState is NoProfile if ViewModelState not isLoading or unauthorized and screen data is null`() =
        runTest {
            val state = ProfileViewModelState(
                loading = false,
                unauthorized = false,
                data = null,
            )
            val uiState = ProfileUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(ProfileUiState.NoProfile::class.java)
            )
        }

    @Test
    fun `UiState is Profile if ViewModelState not loading or unauthorized and both TextFieldStates are null`() =
        runTest {
            val state = ProfileViewModelState(
                loading = false,
                unauthorized = false,
                data = User(),
            )
            val uiState = ProfileUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(ProfileUiState.Profile::class.java)
            )
        }

    @Test
    fun `UiState is Editor if ViewModelState not loading or unauthorized and both TextFieldStates are not null`() =
        runTest {
            val state = ProfileViewModelState(
                loading = false,
                unauthorized = false,
                data = User(),
                emailState = TextFieldState(),
                passwordState = TextFieldState(),
            )
            val uiState = ProfileUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(ProfileUiState.Editor::class.java)
            )
        }
}