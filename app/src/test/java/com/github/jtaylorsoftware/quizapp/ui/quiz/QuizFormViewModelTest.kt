package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.lifecycle.SavedStateHandle
import com.github.jtaylorsoftware.quizapp.data.domain.FakeQuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.FakeQuizResultRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.asForm
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.ui.ErrorStrings
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
            viewModel.uiState.value,
            IsInstanceOf(QuizFormUiState.Form::class.java)
        )
        assertThat(
            (viewModel.uiState.value as QuizFormUiState.Form).quiz.id,
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
            (viewModel.uiState.value as QuizFormUiState.Form).responses,
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
        viewModel.changeResponse(1, QuestionResponse.FillIn(newAnswer))
        advanceUntilIdle()
        assertThat(
            ((viewModel.uiState.value as QuizFormUiState.Form)
                .responses[1].response as QuestionResponse.FillIn).answer,
            `is`(newAnswer)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `changeResponse should throw IllegalArgumentException if the response type doesn't match question type`() =
        runTest {
            viewModel = QuizFormViewModel(
                savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
            )
            advanceUntilIdle()

            viewModel.changeResponse(1, QuestionResponse.MultipleChoice(0))
            advanceUntilIdle()
        }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `changeResponse should throw IndexOOBE if the index is out of bounds`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.changeResponse(-1, QuestionResponse.MultipleChoice(0))
        advanceUntilIdle()
    }

    @Test
    fun `changeResponse should validate FillIn answer text`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        val newAnswer = "" // use empty string
        viewModel.changeResponse(1, QuestionResponse.FillIn(newAnswer))
        advanceUntilIdle()
        assertThat(
            ((viewModel.uiState.value as QuizFormUiState.Form)
                .responses[1].response as QuestionResponse.FillIn).answer,
            `is`(newAnswer)
        )
        assertThat(
            (viewModel.uiState.value as QuizFormUiState.Form).responses[1].error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `changeResponse should validate MultipleChoice choice`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.changeResponse(0, QuestionResponse.MultipleChoice(-1))
        advanceUntilIdle()
        assertThat(
            (viewModel.uiState.value as QuizFormUiState.Form).responses[0].error,
            `is`(notNullValue())
        )
    }

    @Test
    fun `submit should call onSuccess when submit succeeds`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        // Add responses
        viewModel.changeResponse(0, QuestionResponse.MultipleChoice(0))
        viewModel.changeResponse(1, QuestionResponse.FillIn("Ansewr"))

        val onSuccess = mockk<() -> Unit>()
        justRun { onSuccess() }
        viewModel.submit(onSuccess)
        advanceUntilIdle()

        verify(exactly = 1) {
            onSuccess()
        }
        confirmVerified(onSuccess)
    }

    @Test
    fun `submit set uiState loading to error when quiz is expired`() = runTest {
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

        // Add responses
        viewModel.changeResponse(0, QuestionResponse.MultipleChoice(0))
        viewModel.changeResponse(1, QuestionResponse.FillIn("Ansewr"))

        val onSuccess = mockk<() -> Unit>()
        justRun { onSuccess() }
        viewModel.submit(onSuccess)
        advanceUntilIdle()

        verify(exactly = 0) {
            onSuccess()
        }
        confirmVerified(onSuccess)

        assertThat(
            viewModel.uiState.value.loading,
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `submit set uiState to RequireSignIn when repository returns unauthorized`() = runTest {
        val resultNetworkSource = FakeQuizResultNetworkSource()
        resultNetworkSource.failOnNextWith(NetworkResult.HttpError(401))
        viewModel = QuizFormViewModel(
            savedState,
            repository,
            FakeQuizResultRepository(networkSource = resultNetworkSource),
            Dispatchers.Main
        )
        advanceUntilIdle()

        // Add responses
        viewModel.changeResponse(0, QuestionResponse.MultipleChoice(0))
        viewModel.changeResponse(1, QuestionResponse.FillIn("Ansewr"))

        val onSuccess = mockk<() -> Unit>()
        justRun { onSuccess() }
        viewModel.submit(onSuccess)
        advanceUntilIdle()

        verify(exactly = 0) {
            onSuccess()
        }
        confirmVerified(onSuccess)

        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizFormUiState.RequireSignIn::class.java)
        )
    }

    @Test
    fun `submit should validate the responses and not submit when there are errors`() = runTest {
        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        val onSuccess = mockk<() -> Unit>()
        justRun { onSuccess() }

        // Never added any responses, so this call should fail
        viewModel.submit(onSuccess)
        advanceUntilIdle()

        verify(exactly = 0) {
            onSuccess()
        }
        confirmVerified(onSuccess)

        assertThat(
            (viewModel.uiState.value.loading as LoadingState.Error).message,
            containsString("fix form")
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
            viewModel.uiState.value,
            IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
        )
        assertThat(
            viewModel.uiState.value.loading,
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
            viewModel.uiState.value,
            IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
        )
        assertThat(
            (viewModel.uiState.value.loading as LoadingState.Error).message,
            `is`(ErrorStrings.NOT_FOUND.message)
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
            viewModel.uiState.value,
            IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
        )
        assertThat(
            (viewModel.uiState.value.loading as LoadingState.Error).message,
            `is`(ErrorStrings.FORBIDDEN.message)
        )
    }

    @Test
    fun `should set uiState to RequireSignIn when load quiz results in unauthorized`() = runTest {
        networkSource.failOnNextWith(NetworkResult.HttpError(401))

        viewModel = QuizFormViewModel(
            savedState, repository, FakeQuizResultRepository(), Dispatchers.Main
        )
        advanceUntilIdle()

        assertThat(
            viewModel.uiState.value,
            IsInstanceOf(QuizFormUiState.RequireSignIn::class.java)
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
                viewModel.uiState.value,
                IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
            )
            assertThat(
                (viewModel.uiState.value.loading as LoadingState.Error).message,
                `is`(ErrorStrings.NETWORK.message)
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
                viewModel.uiState.value,
                IsInstanceOf(QuizFormUiState.NoQuiz::class.java)
            )
            assertThat(
                (viewModel.uiState.value.loading as LoadingState.Error).message,
                `is`(ErrorStrings.UNKNOWN.message)
            )
        }
}