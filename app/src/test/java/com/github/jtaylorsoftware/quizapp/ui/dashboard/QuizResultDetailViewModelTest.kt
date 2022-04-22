package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.asForm
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultDto
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizResultDetailViewModelTest {
    private lateinit var quizResultNetworkSource: FakeQuizResultNetworkSource
    private lateinit var quizRepository: QuizRepository
    private lateinit var quizNetworkSource: FakeQuizNetworkSource
    private lateinit var quizResultRepository: QuizResultRepository
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
        quizResultNetworkSource = FakeQuizResultNetworkSource(resultDtos)
        quizResultRepository = FakeQuizResultRepository(networkSource = quizResultNetworkSource)
        quizNetworkSource = FakeQuizNetworkSource(quizDtos)
        quizRepository = FakeQuizRepository(networkSource = quizNetworkSource)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should begin with LoadingState NotStarted and NoProfile`() = runTest {
        viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)

        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )

        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.NotStarted::class.java)
        )
    }

    @Test
    fun `refresh should set loading to InProgress and then load data`() = runTest {
        val mockResultNetworkSource = spyk(quizResultNetworkSource)
        coEvery { mockResultNetworkSource.getForQuizByUser(any(), any()) } coAnswers {
            delay(1000)
            NetworkResult.success(resultDtos[0])
        }
        val mockQuizNetworkSource = spyk(quizNetworkSource)
        coEvery { mockQuizNetworkSource.getForm(any()) } coAnswers {
            delay(1000)
            NetworkResult.success(quizDtos[0].asForm())
        }
        quizRepository = FakeQuizRepository(networkSource = mockQuizNetworkSource)
        quizResultRepository = FakeQuizResultRepository(networkSource = mockResultNetworkSource)
        viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)

        viewModel.refresh()
        advanceTimeBy(100)

        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )
        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )

        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultDetailUiState.QuizResultDetail::class.java)
        )
        assertThat(
            (viewModel.uiState as QuizResultDetailUiState.QuizResultDetail).quizResult.quiz.value,
            `is`(resultDtos[0].quiz)
        )
    }

    @Test
    fun `refresh should set not set loading to Success until both form and result have loaded`() = runTest {
        val mockQuizNetworkSource = spyk(quizNetworkSource)
        coEvery { mockQuizNetworkSource.getForm(any()) } coAnswers {
            delay(1000)
            NetworkResult.success(quizDtos[0].asForm())
        }
        val mockResultNetworkSource = spyk(quizResultNetworkSource)
        coEvery { mockResultNetworkSource.getForQuizByUser(any(), any()) } coAnswers {
            delay(2000)
            NetworkResult.success(resultDtos[0])
        }
        quizRepository = FakeQuizRepository(networkSource = mockQuizNetworkSource)
        viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)

        viewModel.refresh()
        // Cause the form to load
        advanceTimeBy(1000)

        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )
        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )

        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultDetailUiState.QuizResultDetail::class.java)
        )
        assertThat(
            (viewModel.uiState as QuizResultDetailUiState.QuizResultDetail).quizResult.quiz.value,
            `is`(resultDtos[0].quiz)
        )
    }

    @Test
    fun `refresh should get fresh data from repository`() = runTest {
        // Make repository return nothing initially
        quizResultNetworkSource = spyk(FakeQuizResultNetworkSource())
        coEvery {
            quizResultNetworkSource.getForQuizByUser(
                any(),
                any()
            )
        } returns NetworkResult.HttpError(404)

        quizResultRepository = FakeQuizResultRepository(networkSource = quizResultNetworkSource)
        viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)

        viewModel.refresh()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )

        // Then will be Loaded with data
        // Set up network to return new data
        clearMocks(quizResultNetworkSource)
        coEvery { quizResultNetworkSource.getForQuizByUser(any(), any()) } returns NetworkResult.success(
            resultDtos[0]
        )

        viewModel.refresh()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizResultDetailUiState.QuizResultDetail::class.java)
        )
        assertThat(
            (viewModel.uiState as QuizResultDetailUiState.QuizResultDetail).quizResult.quiz.value,
            `is`(resultDtos[0].quiz)
        )
    }


    @Test
    fun `refresh should set UiState to NoQuizResult and loading to Error when quizRepository returns Failure`() =
        runTest {
            quizNetworkSource.failOnNextWith(NetworkResult.HttpError(403))
            viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState,
                IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
            )
            assertThat(
                (viewModel.uiState.loading as LoadingState.Error).message,
                `is`(FailureReason.FORBIDDEN)
            )
        }

    @Test
    fun `refresh should set UiState to NoQuizResult and loading to Error when resultRepository returns Failure`() =
        runTest {
            quizResultNetworkSource.failOnNextWith(NetworkResult.HttpError(403))
            viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(
                viewModel.uiState,
                IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
            )
            assertThat(
                (viewModel.uiState.loading as LoadingState.Error).message,
                `is`(FailureReason.FORBIDDEN)
            )
        }

    @Test(expected = IllegalArgumentException::class)
    fun `ViewModel creation should throw IllegalArgumentException when not given quiz id`() =
        runTest {
            val savedState = SavedStateHandle().apply {
                this["user"] = "abdef"
            }
            viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)
        }

    @Test(expected = IllegalArgumentException::class)
    fun `ViewModel creation should throw IllegalArgumentException when not given user id`() =
        runTest {
            val savedState = SavedStateHandle().apply {
                this["quiz"] = "abdef"
            }
            viewModel = QuizResultDetailViewModel(savedState, quizRepository, quizResultRepository)
        }

    @Test
    fun `UiState should be NoQuizResult when either quizResult or quizForm is null`() = runTest {
        var state = QuizResultDetailViewModelState(quizResult = QuizResult())
        var uiState = QuizResultDetailUiState.fromViewModelState(state)
        assertThat(
            uiState,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )

        state = QuizResultDetailViewModelState(quizForm = QuizForm())
        uiState = QuizResultDetailUiState.fromViewModelState(state)
        assertThat(
            uiState,
            IsInstanceOf(QuizResultDetailUiState.NoQuizResult::class.java)
        )
    }

    @Test
    fun `UiState should be QuizResultDetail when both quizResult and quizForm are non-null`() = runTest {
        val state = QuizResultDetailViewModelState(quizResult = QuizResult(), quizForm = QuizForm())
        val uiState = QuizResultDetailUiState.fromViewModelState(state)
        assertThat(
            uiState,
            IsInstanceOf(QuizResultDetailUiState.QuizResultDetail::class.java)
        )
    }
}