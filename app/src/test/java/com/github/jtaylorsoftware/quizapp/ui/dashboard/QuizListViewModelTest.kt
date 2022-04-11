package com.github.jtaylorsoftware.quizapp.ui.dashboard

import com.github.jtaylorsoftware.quizapp.data.domain.FakeQuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.FakeUserRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.UserRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import com.github.jtaylorsoftware.quizapp.data.network.*
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserDto
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
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
class QuizListViewModelTest {
    private lateinit var userCache: FakeUserCache
    private lateinit var networkSource: FakeUserNetworkSource
    private lateinit var userRepository: UserRepository
    private lateinit var quizListingDbSource: QuizListingDatabaseSource
    private lateinit var quizResultListingDbSource: FakeQuizResultListingDatabaseSource
    private lateinit var quizNetworkSource: FakeQuizNetworkSource
    private lateinit var quizRepository: QuizRepository
    private lateinit var viewModel: QuizListViewModel

    private val userId = ObjectId("aewirojadlkflzmdfakl")
    private val userDto = UserDto(id = userId.value, email = "emailOne@email.com")
    private val quizDtos = mutableListOf(
        QuizDto(id = "a290fadda09da39kjfnm", user = userId.value)
    )
    private val quizListingDtos = quizDtos.map {
        it.asListing()
    }

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        userCache = FakeUserCache(UserEntity.fromDto(userDto))
        networkSource =
            FakeUserNetworkSource(
                data = listOf(UserWithPassword(userDto, "password")),
                quizzes = quizListingDtos,
            )
        quizListingDbSource = FakeQuizListingDatabaseSource()
        quizResultListingDbSource = FakeQuizResultListingDatabaseSource()
        userRepository = FakeUserRepository(
            userCache,
            networkSource,
            quizListingDbSource,
            quizResultListingDbSource
        )
        quizNetworkSource = FakeQuizNetworkSource(quizDtos)
        quizRepository = FakeQuizRepository(quizListingDbSource, quizNetworkSource)
        viewModel = QuizListViewModel(userRepository, quizRepository)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel should load quizzes immediately`() = runTest {
        // Should first be Loading, before initial refresh
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizListUiState.NoQuizzes::class.java)
        )

        advanceUntilIdle()
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizListUiState.QuizList::class.java)
        )

        assertThat(
            (viewModel.uiState.value as QuizListUiState.QuizList).data,
            containsInAnyOrder(*quizListingDtos.map { QuizListing.fromDto(it) }.toTypedArray())
        )
    }

    @Test
    fun `refresh should set uiState to RequireSignIn when getting quizzes results in Http Unauthorized`() =
        runTest {
            // local cache has no value, so force error in network to cause Unauthorized
            networkSource.failOnNextWith(NetworkResult.HttpError(401))

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(QuizListUiState.RequireSignIn::class.java)
            )
        }

    @Test
    fun `refresh should do nothing if already refreshing data`() = runTest {
        val mockRepository = spyk(userRepository)
        coEvery { mockRepository.getQuizzes() } coAnswers {
            userRepository.getQuizzes()
        }

        viewModel = QuizListViewModel(mockRepository, quizRepository)
        // Finish initial load - call #1
        advanceUntilIdle()

        // Do first refresh - call #2, sets loading = true
        viewModel.refresh()
        // Immediately try another - would be call #3
        viewModel.refresh()
        advanceUntilIdle()

        // Should be 2 calls
        coVerify(exactly = 2) {
            mockRepository.getQuizzes()
        }
        confirmVerified(mockRepository)
    }

    @Test
    fun `refresh should get fresh quizzes from repository`() = runTest {
        networkSource =
            spyk(FakeUserNetworkSource(data = listOf(UserWithPassword(userDto, "password"))))
        // Make network return nothing so both local and network initially empty
        coEvery { networkSource.getQuizzes() } returns NetworkResult.success(emptyList())
        userRepository = FakeUserRepository(
            userCache,
            networkSource,
            quizListingDbSource,
            quizResultListingDbSource
        )

        viewModel = QuizListViewModel(userRepository, quizRepository)
        // Get initial load
        advanceUntilIdle()

        // Should be empty first
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizListUiState.NoQuizzes::class.java)
        )

        // Set up network to return new data
        clearMocks(networkSource)
        coEvery { networkSource.getQuizzes() } returns NetworkResult.success(quizListingDtos)

        // Call refresh, and assert that it's new data
        viewModel.refresh()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState.value as QuizListUiState.QuizList).data,
            containsInAnyOrder(*quizListingDtos.map { QuizListing.fromDto(it) }.toTypedArray())
        )
    }

    @Test
    fun `deleteQuiz should set loading flag before deletion`() = runTest {
        quizNetworkSource = spyk(quizNetworkSource)
        quizRepository = FakeQuizRepository(quizListingDbSource, quizNetworkSource)

        coEvery { quizNetworkSource.delete(any()) } coAnswers {
            delay(1000)
            callOriginal()
        }

        // Finish initial load
        viewModel = QuizListViewModel(userRepository, quizRepository)
        advanceUntilIdle()

        // Do delete
        viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
        advanceTimeBy(100)

        assertThat(
            (viewModel.uiState.value as QuizListUiState.QuizList).loading,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )
    }

    @Test
    fun `deleteQuiz should delete one quiz and clear loading on success`() = runTest {
        quizNetworkSource = spyk(quizNetworkSource)
        quizRepository = FakeQuizRepository(quizListingDbSource, quizNetworkSource)

        coEvery { quizNetworkSource.delete(any()) } coAnswers {
            delay(1000)
            callOriginal()
        }

        // Finish initial load
        viewModel = QuizListViewModel(userRepository, quizRepository)
        advanceUntilIdle()

        // Do delete
        viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
        advanceUntilIdle()
        coVerify(exactly = 1) {
            quizNetworkSource.delete(quizListingDtos[0].id)
        }
        confirmVerified(quizNetworkSource)

        assertThat(
            (viewModel.uiState.value as QuizListUiState.QuizList).loading,
            IsInstanceOf(LoadingState.AwaitingAction::class.java)
        )
    }

    @Test
    fun `deleteQuiz does nothing when quizzes are null or empty`() = runTest {
        networkSource =
                // Empty network source - no listings to load
            FakeUserNetworkSource(
                data = listOf(UserWithPassword(userDto, "password")),
            )
        userRepository = FakeUserRepository(
            userCache,
            networkSource,
            quizListingDbSource,
            quizResultListingDbSource
        )
        viewModel = QuizListViewModel(userRepository, quizRepository)

        // Finish first load - no listings in network so should be emptyList
        advanceUntilIdle()

        // Try delete
        viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
        runCurrent()

        // Shouldn't set loading
        assertThat(
            (viewModel.uiState.value as QuizListUiState.NoQuizzes).loading,
            IsInstanceOf(LoadingState.AwaitingAction::class.java)
        )
    }

    @Test
    fun `deleteQuiz does nothing when loading is true`() = runTest {
        // Finish first load
        advanceUntilIdle()

        // Do refresh to set loading
        viewModel.refresh()

        // Try delete
        viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
        runCurrent()

        // Shouldn't set loading
        assertThat(
            (viewModel.uiState.value as QuizListUiState.QuizList).loading,
            IsInstanceOf(LoadingState.AwaitingAction::class.java)
        )
    }

    @Test
    fun `deleteQuiz should set uiState to RequireSignIn when delete fails with Unauthorized`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.HttpError(401))
            viewModel.deleteQuiz(ObjectId("123"))
            advanceUntilIdle()
            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(QuizListUiState.RequireSignIn::class.java)
            )
        }

    @Test
    fun `UiState is RequireSignIn if ViewModelState has unauthorized flag set`() = runTest {
        val state = QuizListViewModelState(unauthorized = true)
        val uiState = QuizListUiState.fromViewModelState(state)
        assertThat(uiState, IsInstanceOf(QuizListUiState.RequireSignIn::class.java))
    }

    @Test
    fun `UiState is NoQuizzes if ViewModelState isLoading and screen data is null`() = runTest {
        val state = QuizListViewModelState(
            loading = true,
            unauthorized = false,
            data = null,
        )
        val uiState = QuizListUiState.fromViewModelState(state)
        assertThat(uiState, IsInstanceOf(QuizListUiState.NoQuizzes::class.java))
    }

    @Test
    fun `UiState is NoQuizzes if ViewModelState not isLoading or unauthorized and screen data is null`() =
        runTest {
            val state = QuizListViewModelState(
                loading = false,
                unauthorized = false,
                data = null,
            )
            val uiState = QuizListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizListUiState.NoQuizzes::class.java)
            )
        }

    @Test
    fun `UiState is NoQuizzes if ViewModelState not isLoading or unauthorized and screen data is emptyList`() =
        runTest {
            val state = QuizListViewModelState(
                loading = false,
                unauthorized = false,
                data = emptyList(),
            )
            val uiState = QuizListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizListUiState.NoQuizzes::class.java)
            )
        }

    @Test
    fun `UiState is QuizList if ViewModelState not loading or unauthorized data is not null`() =
        runTest {
            val state = QuizListViewModelState(
                loading = false,
                unauthorized = false,
                data = listOf(QuizListing()),
            )
            val uiState = QuizListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizListUiState.QuizList::class.java)
            )
        }
}