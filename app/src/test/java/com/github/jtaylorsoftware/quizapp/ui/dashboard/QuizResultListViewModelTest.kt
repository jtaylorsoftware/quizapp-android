package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.local.QuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import com.github.jtaylorsoftware.quizapp.data.network.*
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserDto
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizResultListViewModelTest {
    private lateinit var userCache: FakeUserCache
    private lateinit var userNetworkSource: FakeUserNetworkSource
    private lateinit var userRepository: UserRepository
    private lateinit var quizListingDbSource: QuizListingDatabaseSource
    private lateinit var quizResultListingDbSource: FakeQuizResultListingDatabaseSource
    private lateinit var quizResultNetworkSource: FakeQuizResultNetworkSource
    private lateinit var quizRepository: QuizRepository
    private lateinit var quizNetworkSource: FakeQuizNetworkSource
    private lateinit var quizResultRepository: QuizResultRepository

    private val userId = ObjectId("aewirojadlkflzmdfakl")
    private val userDto = UserDto(id = userId.value, email = "emailOne@email.com")
    private val quizDtos = mutableListOf(
        QuizDto(id = "abcdef12345", user = userId.value, title = "Test")
    )
    private val resultDtos = quizDtos.map {
        QuizResultDto(id = "54321fedcba", quiz = it.id, quizTitle = it.title, user = userId.value)
    }
    private val resultListingDtos = resultDtos.map {
        it.asListing()
    }

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        userCache = FakeUserCache(UserEntity.fromDto(userDto))
        userNetworkSource =
            FakeUserNetworkSource(
                data = listOf(UserWithPassword(userDto, "password")),
                results = resultListingDtos
            )
        quizListingDbSource = FakeQuizListingDatabaseSource()
        quizResultListingDbSource = FakeQuizResultListingDatabaseSource()
        userRepository = FakeUserRepository(
            userCache,
            userNetworkSource,
            quizListingDbSource,
            quizResultListingDbSource
        )
        quizNetworkSource = FakeQuizNetworkSource(quizDtos)
        quizRepository = FakeQuizRepository(quizListingDbSource, quizNetworkSource)
        quizResultNetworkSource = FakeQuizResultNetworkSource(resultDtos)
        quizResultRepository = FakeQuizResultRepository(
            quizResultListingDbSource,
            quizResultNetworkSource
        )
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh should load results for current user, when nothing is set in SavedStateHandle`() =
        runTest {
            val savedState = SavedStateHandle()
//        savedState.set("user", userId.value)
            val viewModel = QuizResultListViewModel(
                savedState,
                userRepository,
                quizRepository,
                quizResultRepository
            )

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                (viewModel.uiState as QuizResultListUiState.ListForUser).data,
                containsInAnyOrder(*resultListingDtos.map { QuizResultListing.fromDto(it) }
                    .toTypedArray())
            )
        }

    @Test
    fun `refresh should load results for a quiz and set quizTitle, when quiz is set in SavedStateHandle`() =
        runTest {
            val savedState = SavedStateHandle()
            savedState.set("quiz", quizDtos[0].id)
            val viewModel = QuizResultListViewModel(
                savedState,
                userRepository,
                quizRepository,
                quizResultRepository
            )

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                (viewModel.uiState as QuizResultListUiState.ListForQuiz).quizTitle,
                `is`(quizDtos[0].title)
            )

            assertThat(
                (viewModel.uiState as QuizResultListUiState.ListForQuiz).data,
                containsInAnyOrder(*resultListingDtos.map { QuizResultListing.fromDto(it) }
                    .toTypedArray())
            )
        }

    @Test
    fun `refresh should set loading to Error, when refresh for user causes Http Unauthorized`() =
        runTest {
            val savedState = SavedStateHandle()
//            savedState.set("user", userId.value)
            val viewModel =
                QuizResultListViewModel(
                    savedState,
                    userRepository,
                    quizRepository,
                    quizResultRepository
                )

            // local cache has no value, so force error in network to cause Unauthorized
            userNetworkSource.failOnNextWith(NetworkResult.HttpError(401))

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.loading,
                IsInstanceOf(LoadingState.Error::class.java)
            )
        }

    @Test
    fun `refresh should set loading to Error, when refresh for a quiz causes Http Forbidden`() =
        runTest {
            val savedState = SavedStateHandle()
            savedState.set("quiz", quizDtos[0].id)
            val viewModel =
                QuizResultListViewModel(
                    savedState,
                    userRepository,
                    quizRepository,
                    quizResultRepository
                )

            // local cache has no value, so force error in network to cause Forbidden
            quizResultNetworkSource.failOnNextWith(NetworkResult.HttpError(403))

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.loading,
                IsInstanceOf(LoadingState.Error::class.java)
            )
        }

    @Test
    fun `refresh should set loading to Error, when refresh for a quiz causes Http Not Found`() =
        runTest {
            val savedState = SavedStateHandle()
            savedState.set("quiz", quizDtos[0].id)
            val viewModel =
                QuizResultListViewModel(
                    savedState,
                    userRepository,
                    quizRepository,
                    quizResultRepository
                )

            // local cache has no value, so force error in network to cause Not Found
            quizResultNetworkSource.failOnNextWith(NetworkResult.HttpError(404))


            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState.loading,
                IsInstanceOf(LoadingState.Error::class.java)
            )
        }

    @Test
    fun `refresh should do nothing if already refreshing data`() = runTest {
        val mockUserRepo = spyk(userRepository)
        coEvery { mockUserRepo.getResults() } coAnswers {
            delay(1000)
            callOriginal()
        }

        val savedState = SavedStateHandle()
//        savedState.set("user", userId.value)
        val viewModel = QuizResultListViewModel(
            savedState,
            mockUserRepo,
            quizRepository,
            quizResultRepository
        )

        viewModel.refresh()
        // Enter refresh
        advanceTimeBy(100)

        // Try to call while still refreshing
        viewModel.refresh()

        // Should be 2 calls
        coVerify(exactly = 1) {
            mockUserRepo.getResults()
        }
        confirmVerified(mockUserRepo)

        // Finish initial load - call #1
        advanceUntilIdle()
    }

    @Test
    fun `refresh should get fresh results from repository`() = runTest {
        userNetworkSource =
            spyk(FakeUserNetworkSource(data = listOf(UserWithPassword(userDto, "password"))))
        // Make network return nothing so both local and network initially empty
        coEvery { userNetworkSource.getResults() } returns NetworkResult.success(emptyList())
        userRepository = FakeUserRepository(
            userCache,
            userNetworkSource,
            quizListingDbSource,
            quizResultListingDbSource
        )

        val savedState = SavedStateHandle()
//        savedState.set("user", userId.value)
        val viewModel = QuizResultListViewModel(
            savedState,
            userRepository,
            quizRepository,
            quizResultRepository
        )

        viewModel.refresh()
        advanceUntilIdle()

        // Should be empty
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultListUiState.NoQuizResults::class.java)
        )

        // Set up network to return new data
        clearMocks(userNetworkSource)
        coEvery { userNetworkSource.getResults() } returns NetworkResult.success(resultListingDtos)

        // Call refresh, and assert that it's new data
        viewModel.refresh()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState as QuizResultListUiState.ListForUser).data,
            containsInAnyOrder(*resultListingDtos.map { QuizResultListing.fromDto(it) }
                .toTypedArray())
        )
    }

    @Test
    fun `UiState is NoQuizResults with Loading InProgress if ViewModelState isLoading and screen data is null`() =
        runTest {
            val state = QuizResultListViewModelState(
                loading = LoadingState.InProgress,
                data = null,
            )
            val uiState = QuizResultListUiState.fromViewModelState(state)
            assertThat(
                (uiState as QuizResultListUiState.NoQuizResults).loading,
                IsInstanceOf(LoadingState.InProgress::class.java)
            )
        }

    @Test
    fun `UiState is NoQuizResults if ViewModelState loading not InProgress and screen data is null`() =
        runTest {
            val state = QuizResultListViewModelState(
                loading = LoadingState.Success(),
                data = null,
            )
            val uiState = QuizResultListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizResultListUiState.NoQuizResults::class.java)
            )
        }

    @Test
    fun `UiState is NoQuizResults if ViewModelState not isLoading and screen data is emptyList`() =
        runTest {
            val state = QuizResultListViewModelState(
                loading = LoadingState.Success(),
                data = emptyList(),
            )
            val uiState = QuizResultListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizResultListUiState.NoQuizResults::class.java)
            )
        }

    @Test
    fun `UiState is ListForUser if not loading or error, data is not null, and quizId is null`() =
        runTest {
            val state = QuizResultListViewModelState(
                loading = LoadingState.Success(),
                data = listOf(QuizResultListing()),
                quizId = null
            )
            val uiState = QuizResultListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizResultListUiState.ListForUser::class.java)
            )
        }

    @Test
    fun `UiState is ListForQuiz if not loading or other error, data is not null, and quizId is not null`() =
        runTest {
            val state = QuizResultListViewModelState(
                loading = LoadingState.Success(),
                data = listOf(QuizResultListing()),
                quizId = ObjectId("id123")
            )
            val uiState = QuizResultListUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizResultListUiState.ListForQuiz::class.java)
            )
        }
}