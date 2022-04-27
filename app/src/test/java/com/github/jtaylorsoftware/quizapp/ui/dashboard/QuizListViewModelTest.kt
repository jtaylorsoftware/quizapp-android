package com.github.jtaylorsoftware.quizapp.ui.dashboard

import com.github.jtaylorsoftware.quizapp.data.domain.*
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
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
    fun `refresh should set loading to InProgress and then load data`() = runTest {
        val mockUserRepository = spyk(userRepository)
        viewModel = QuizListViewModel(mockUserRepository, quizRepository)

        every { mockUserRepository.getQuizzes() } returns flow {
            delay(1000)
            emit(Result.success(listOf(QuizListing.fromDto(quizListingDtos[0]))))
        }

        viewModel.refresh()
        advanceTimeBy(100)
        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )

        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizListUiState.QuizList::class.java)
        )
    }

    @Test
    fun `refresh should do nothing if already refreshing data`() = runTest {
        val mockRepository = spyk(userRepository)
        coEvery { mockRepository.getQuizzes() } coAnswers {
            delay(1000)
            userRepository.getQuizzes()
        }

        viewModel = QuizListViewModel(mockRepository, quizRepository)

        viewModel.refresh()
        // Enter refresh
        advanceTimeBy(100)

        // Try to call while still refreshing
        viewModel.refresh()

        // Should be 2 calls
        coVerify(exactly = 1) {
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
            viewModel.uiState,
            IsInstanceOf(QuizListUiState.NoQuizzes::class.java)
        )

        // Set up network to return new data
        clearMocks(networkSource)
        coEvery { networkSource.getQuizzes() } returns NetworkResult.success(quizListingDtos)

        // Call refresh, and assert that it's new data
        viewModel.refresh()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState as QuizListUiState.QuizList).data,
            containsInAnyOrder(*quizListingDtos.map { QuizListing.fromDto(it) }.toTypedArray())
        )
    }

    @Test
    fun `deleteQuiz should set deleteQuizStatus to InProgress before deletion`() = runTest {
        quizNetworkSource = spyk(quizNetworkSource)
        quizRepository = FakeQuizRepository(quizListingDbSource, quizNetworkSource)

        coEvery { quizNetworkSource.delete(any()) } coAnswers {
            delay(1000)
            callOriginal()
        }

        // Finish initial load
        viewModel = QuizListViewModel(userRepository, quizRepository)
        viewModel.refresh()
        advanceUntilIdle()

        // Do delete
        viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
        advanceTimeBy(100)

        assertThat(
            (viewModel.uiState as QuizListUiState.QuizList).deleteQuizStatus,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )
    }

    @Test
    fun `deleteQuiz when successful should delete one quiz and clear loading on success`() =
        runTest {
            quizNetworkSource = spyk(quizNetworkSource)
            quizRepository = FakeQuizRepository(quizListingDbSource, quizNetworkSource)

            coEvery { quizNetworkSource.delete(any()) } coAnswers {
                delay(1000)
                callOriginal()
            }

            // Finish initial load
            viewModel = QuizListViewModel(userRepository, quizRepository)
            viewModel.refresh()
            advanceUntilIdle()

            // Do delete
            viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
            advanceUntilIdle()
            coVerify(exactly = 1) {
                quizNetworkSource.delete(quizListingDtos[0].id)
            }
            confirmVerified(quizNetworkSource)

            assertThat(
                (viewModel.uiState as QuizListUiState.QuizList).deleteQuizStatus,
                IsInstanceOf(LoadingState.Success::class.java)
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
        quizNetworkSource = spyk(FakeQuizNetworkSource(quizDtos))
        coEvery { quizNetworkSource.delete(any()) } returns NetworkResult.success()
        quizRepository = FakeQuizRepository(quizListingDbSource, quizNetworkSource)
        viewModel = QuizListViewModel(userRepository, quizRepository)

        // Finish first load - no listings in network so should be emptyList
        advanceUntilIdle()

        // Try delete
        viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
        runCurrent()

        // Shouldn't do deletion
        coVerify(exactly = 0) {
            quizNetworkSource.delete(any())
        }
        confirmVerified(quizNetworkSource)
    }

    @Test
    fun `deleteQuiz does nothing when loading is true`() = runTest {
        val mockRepository = spyk(userRepository)
        coEvery { mockRepository.getQuizzes() } coAnswers {
            delay(1000)
            userRepository.getQuizzes()
        }

        viewModel = QuizListViewModel(mockRepository, quizRepository)
        // Do refresh to set loading
        viewModel.refresh()
        advanceTimeBy(100)
        // Try delete
        viewModel.deleteQuiz(ObjectId(quizListingDtos[0].id))
        advanceUntilIdle()

        // Shouldn't set loading
        assertThat(
            (viewModel.uiState as QuizListUiState.QuizList).deleteQuizStatus,
            IsInstanceOf(LoadingState.NotStarted::class.java)
        )
    }

    @Test
    fun `deleteQuiz should set deleteQuizStatus to Error when delete fails with Unauthorized`() =
        runTest {
            viewModel.refresh()
            advanceUntilIdle()

            quizNetworkSource.failOnNextWith(NetworkResult.HttpError(401))
            viewModel.deleteQuiz(ObjectId("123"))
            advanceUntilIdle()
            assertThat(
                (viewModel.uiState as QuizListUiState.QuizList).deleteQuizStatus,
                IsInstanceOf(LoadingState.Error::class.java)
            )
        }

    @Test
    fun `UiState is NoQuizzes if ViewModelState loading InProgress and screen data is null`() =
        runTest {
            val state = QuizListViewModelState(
                loading = LoadingState.InProgress,
                data = null,
            )
            val uiState = QuizListUiState.fromViewModelState(state)
            assertThat(uiState, IsInstanceOf(QuizListUiState.NoQuizzes::class.java))
        }

    @Test
    fun `UiState is NoQuizzes if ViewModelState loading not InProgress and screen data is null`() =
        runTest {
            val state = QuizListViewModelState(
                loading = LoadingState.NotStarted,
                data = null,
            )
            val uiState = QuizListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizListUiState.NoQuizzes::class.java)
            )
        }

    @Test
    fun `UiState is NoQuizzes if ViewModelState loading not InProgress and screen data is emptyList`() =
        runTest {
            val state = QuizListViewModelState(
                loading = LoadingState.Success(),
                data = emptyList(),
            )
            val uiState = QuizListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizListUiState.NoQuizzes::class.java)
            )
        }

    @Test
    fun `UiState is QuizList if ViewModelState not loading data is not null`() =
        runTest {
            val state = QuizListViewModelState(
                loading = LoadingState.Success(),
                data = listOf(QuizListing()),
            )
            val uiState = QuizListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizListUiState.QuizList::class.java)
            )
        }
}