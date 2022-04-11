package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.github.jtaylorsoftware.quizapp.data.domain.FakeQuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultDto
import com.github.jtaylorsoftware.quizapp.ui.ErrorStrings
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.IllegalStateException

@OptIn(ExperimentalCoroutinesApi::class)
class QuizResultDetailViewModelTest {
    private lateinit var networkSource: FakeQuizResultNetworkSource
    private lateinit var repository: QuizResultRepository
    private lateinit var viewModel: QuizResultDetailViewModel

    private val userId = ObjectId("aewirojadlkflzmdfakl")
    private val quizDtos = mutableListOf(
        QuizDto(id = "abcdef12345", user = userId.value, title = "Test")
    )
    private val resultDtos = quizDtos.map {
        QuizResultDto(id = "54321fedcba", quiz = it.id, quizTitle = it.title, user = userId.value)
    }

    private lateinit var savedState: SavedStateHandle

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        savedState = SavedStateHandle().apply {
            this["quiz"] = quizDtos[0].id
            this["user"] = userId.value
        }
        networkSource = FakeQuizResultNetworkSource(resultDtos)
        repository = FakeQuizResultRepository(networkSource = networkSource)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh should run immediately on ViewModel creation`() = runTest {
        viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)

        // First is loading
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )

        // Then will be Loaded with data
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizResultDetailUiState.QuizResultDetail::class.java)
        )
        assertThat(
            (viewModel.uiState.value as QuizResultDetailUiState.QuizResultDetail).data.quiz.value,
            `is`(resultDtos[0].quiz)
        )
    }

    @Test
    fun `refresh should get fresh data from repository`() = runTest {
        // Make repository return nothing initially
        networkSource = spyk(FakeQuizResultNetworkSource())
        coEvery { networkSource.getForQuizByUser(any(),any()) } returns NetworkResult.HttpError(404)

        repository = FakeQuizResultRepository(networkSource = networkSource)
        viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)

        advanceUntilIdle()
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )

        // Then will be Loaded with data
        // Set up network to return new data
        clearMocks(networkSource)
        coEvery { networkSource.getForQuizByUser(any(),any()) } returns NetworkResult.success(resultDtos[0])
        viewModel.refresh()

        advanceUntilIdle()
        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizResultDetailUiState.QuizResultDetail::class.java)
        )
        assertThat(
            (viewModel.uiState.value as QuizResultDetailUiState.QuizResultDetail).data.quiz.value,
            `is`(resultDtos[0].quiz)
        )
    }

    @Test
    fun `refresh should set uiState to RequiresSignIn when repository returns unauthorized`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.HttpError(401))
            viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)

            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(QuizResultDetailUiState.RequireSignIn::class.java)
            )
        }

    @Test
    fun `refresh should set uiState to NoQuizResult when repository returns forbidden`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.HttpError(403))
            viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)

            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
            )
            assertThat(
                (viewModel.uiState.value.loading as LoadingState.Error).message,
                `is`(ErrorStrings.FORBIDDEN.message)
            )
        }

    @Test
    fun `refresh should set uiState to NoQuizResult when repository returns not found`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.HttpError(404))
            viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)

            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
            )
            assertThat(
                (viewModel.uiState.value.loading as LoadingState.Error).message,
                `is`(ErrorStrings.NOT_FOUND.message)
            )
        }

    @Test
    fun `refresh should set uiState to NoQuizResult when repository returns network error`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.NetworkError(IllegalStateException()))
            viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)

            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
            )
            assertThat(
                (viewModel.uiState.value.loading as LoadingState.Error).message,
                `is`(ErrorStrings.NETWORK.message)
            )
        }

    @Test
    fun `refresh should set uiState to NoQuizResult when repository returns unknown error`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.Unknown(IllegalStateException()))
            viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)

            advanceUntilIdle()

            assertThat(
                viewModel.uiState.value,
                IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
            )
            assertThat(
                (viewModel.uiState.value.loading as LoadingState.Error).message,
                `is`(ErrorStrings.UNKNOWN.message)
            )
        }

    @Test(expected = IllegalArgumentException::class)
    fun `ViewModel creation should throw IllegalArgumentException when not given quiz id`() =
        runTest {
            val savedState = SavedStateHandle().apply {
                this["user"] = "abdef"
            }
            viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)
        }

    @Test(expected = IllegalArgumentException::class)
    fun `ViewModel creation should throw IllegalArgumentException when not given user id`() =
        runTest {
            val savedState = SavedStateHandle().apply {
                this["quiz"] = "abdef"
            }
            viewModel = QuizResultDetailViewModel(savedState, repository, Dispatchers.Main)
        }
}