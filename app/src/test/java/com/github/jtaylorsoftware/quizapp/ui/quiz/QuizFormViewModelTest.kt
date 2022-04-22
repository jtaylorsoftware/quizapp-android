package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.lifecycle.SavedStateHandle
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.FakeQuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.FakeQuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.asForm
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import io.mockk.coEvery
import io.mockk.spyk
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
class QuizFormViewModelTest {
    private lateinit var networkSource: FakeQuizNetworkSource
    private lateinit var repository: QuizRepository

    private lateinit var savedState: SavedStateHandle

    private lateinit var viewModel: QuizFormViewModel

    private val userId = ObjectId("aewirojadlkflzmdfakl")
    private val quizId = ObjectId("abcdef12345")
    private val quizDtos = mutableListOf(
        QuizDto(
            id = quizId.value,
            user = userId.value,
            title = "Test",
            questions = listOf(
                QuestionDto.MultipleChoice(
                    "Question 1", correctAnswer = 0, answers = listOf(
                        QuestionDto.MultipleChoice.Answer("Answer 1"),
                        QuestionDto.MultipleChoice.Answer("Answer 2")
                    )
                ),
                QuestionDto.FillIn("Question 2", "Correct")
            )
        )
    )

    // Runs a block of code on the current Form UiState in the ViewModel.
    private fun <T> QuizFormViewModel.runAsForm(block: QuizFormUiState.Form.() -> T): T {
        return (this.uiState as QuizFormUiState.Form).block()
    }

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        savedState = SavedStateHandle().apply {
            this["quiz"] = quizId.value
        }
        networkSource = FakeQuizNetworkSource(quizDtos)
        repository = FakeQuizRepository(networkSource = networkSource)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should immediately load form for requested quiz`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizFormUiState.Form::class.java)
        )
        assertThat(
            (viewModel.uiState as QuizFormUiState.Form).quiz.id,
            `is`(quizId)
        )
    }

    @Test
    fun `uiState should have same number of responses as quiz has questions`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()
        assertThat(
            (viewModel.uiState as QuizFormUiState.Form).responses,
            hasSize(quizDtos[0].questions.size)
        )
    }

    @Test
    fun `changeResponse should update the response at given index`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        val newAnswer = "TEST"
        viewModel.runAsForm {
            (responses[1] as FormResponseState.FillIn).changeAnswer(newAnswer)
            advanceUntilIdle()
            assertThat(
                (responses[1] as FormResponseState.FillIn).answer.text,
                `is`(newAnswer)
            )
        }
    }

    @Test
    fun `changeResponse should validate FillIn answer text`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        val newAnswer = "" // use empty string
        viewModel.runAsForm {
            (responses[1] as FormResponseState.FillIn).changeAnswer(newAnswer)
            advanceUntilIdle()
            assertThat(
                (responses[1] as FormResponseState.FillIn).answer.error,
                `is`(notNullValue())
            )
        }
    }

    @Test
    fun `upload should set uploadStatus to success when upload succeeds`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.runAsForm {
            (responses[0] as FormResponseState.MultipleChoice).choice = 0
            (responses[1] as FormResponseState.FillIn).changeAnswer("Answer")
        }

        viewModel.uploadResponses()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState as QuizFormUiState.Form).uploadStatus,
            IsInstanceOf(LoadingState.Success::class.java)
        )
    }

    @Test
    fun `upload set uiState loading to error when quiz is expired`() = runTest {
        val resultNetworkSource = FakeQuizResultNetworkSource()
        resultNetworkSource.failOnNextWith(
            NetworkResult.HttpError(
                403,
                listOf(ApiError(field = "expiration", message = "expired"))
            )
        )
        viewModel = QuizFormViewModel(
            savedState,
            repository,
            FakeQuizResultRepository(networkSource = resultNetworkSource),
            Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.runAsForm {
            (responses[0] as FormResponseState.MultipleChoice).choice = 0
            (responses[1] as FormResponseState.FillIn).changeAnswer("Answer")
        }

        viewModel.uploadResponses()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState as QuizFormUiState.Form).uploadStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `upload set uploadStatus to Error when repository returns unauthorized`() = runTest {
        val resultNetworkSource = FakeQuizResultNetworkSource()
        resultNetworkSource.failOnNextWith(NetworkResult.HttpError(401))
        viewModel = QuizFormViewModel(
            savedState,
            repository,
            FakeQuizResultRepository(networkSource = resultNetworkSource),
            Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.runAsForm {
            (responses[0] as FormResponseState.MultipleChoice).choice = 0
            (responses[1] as FormResponseState.FillIn).changeAnswer("Answer")
        }

        viewModel.uploadResponses()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState as QuizFormUiState.Form).uploadStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `upload should validate the responses and not upload when there are errors`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        // Never added any responses, so this call should fail
        viewModel.uploadResponses()
        advanceUntilIdle()

        assertThat(
            (viewModel.uiState as QuizFormUiState.Form).uploadStatus,
            IsInstanceOf(LoadingState.Error::class.java)
        )
        assertThat(
            ((viewModel.uiState as QuizFormUiState.Form).uploadStatus as LoadingState.Error).message,
            `is`(FailureReason.FORM_HAS_ERRORS)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw IllegalArgumentException when not given quiz id`() = runTest {
        viewModel = QuizFormViewModel(
            SavedStateHandle(), repository, FakeQuizResultRepository(), Dispatchers.Main
        )
    }

    @Test
    fun `should set loading to true while loading quiz form`() = runTest {
        networkSource = spyk(networkSource)
        repository = FakeQuizRepository(networkSource = networkSource)
        coEvery {
            networkSource.getForm(any())
        } coAnswers {
            delay(500)
            delay(500)
            NetworkResult.success(quizDtos[0].asForm())
        }

        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceTimeBy(100)
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
        )
        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.InProgress::class.java)
        )
    }

    @Test
    fun `should set uiState to NoQuiz with loading error when quiz not found`() = runTest {
        networkSource.failOnNextWith(NetworkResult.HttpError(404))

        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
        )
        assertThat(
            (viewModel.uiState.loading as LoadingState.Error).message,
            `is`(FailureReason.NOT_FOUND)
        )
    }

    @Test
    fun `should set uiState to NoQuiz with loading error when forbidden to get quiz`() = runTest {
        networkSource.failOnNextWith(NetworkResult.HttpError(403))

        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
        )
        assertThat(
            (viewModel.uiState.loading as LoadingState.Error).message,
            `is`(FailureReason.FORBIDDEN)
        )
    }

    @Test
    fun `should set uiState loading to Error when load quiz results in unauthorized`() = runTest {
        networkSource.failOnNextWith(NetworkResult.HttpError(401))

        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `should set uiState to NoQuiz with error when load quiz results in network error`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.NetworkError(IllegalStateException()))

            viewModel = QuizFormViewModel(
                savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
            )
            advanceUntilIdle()

            assertThat(
                viewModel.uiState,
                IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
            )
            assertThat(
                (viewModel.uiState.loading as LoadingState.Error).message,
                `is`(FailureReason.NETWORK)
            )
        }

    @Test
    fun `should set uiState to NoQuiz with error when load quiz results in unknown error`() =
        runTest {
            networkSource.failOnNextWith(NetworkResult.Unknown(IllegalStateException()))

            viewModel = QuizFormViewModel(
                savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
            )
            advanceUntilIdle()

            assertThat(
                viewModel.uiState,
                IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
            )
            assertThat(
                (viewModel.uiState.loading as LoadingState.Error).message,
                `is`(FailureReason.UNKNOWN)
            )
        }
}