package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.lifecycle.SavedStateHandle
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.FakeQuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.QuizRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.util.toInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class QuizEditorViewModelTest {
    private lateinit var viewModel: QuizEditorViewModel

    private val quizId = ObjectId("quiz123456")
    private val userId = ObjectId("user123456")
    private val allowedUsers = "user1, user2, user3"
    private val quizDto = QuizDto(
        id = quizId.value,
        title = "Test",
        user = userId.value,
        date = Instant.now().toString(),
        expiration = Instant.now().plus(1, ChronoUnit.DAYS).toString(),
        isPublic = false,
        allowedUsers = allowedUsers.split(", "),
        questions = listOf(
            QuestionDto.MultipleChoice(
                text = "Question 1",
                correctAnswer = 0,
                answers = listOf(
                    QuestionDto.MultipleChoice.Answer("answer 1"),
                    QuestionDto.MultipleChoice.Answer("answer 2")
                )
            ),
            QuestionDto.FillIn(
                text = "Question 2", correctAnswer = "Answer"
            )
        ),
        results = listOf("result123")
    )

    private lateinit var networkSource: FakeQuizNetworkSource
    private lateinit var quizRepository: QuizRepository

    // Runs a block of code on the current Editor UiState in the ViewModel.
    private fun <T> QuizEditorViewModel.runAsEditor(block: QuizEditorUiState.Editor.() -> T): T {
        return (this.uiState as QuizEditorUiState.Editor).block()
    }

    // Runs a block of code on the current QuizState in the ViewModel.
    private fun <T> QuizEditorViewModel.runAsQuizState(block: QuizState.() -> T): T {
        return (this.uiState as QuizEditorUiState.Editor).quizState.block()
    }

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        networkSource = FakeQuizNetworkSource(quizzes = listOf(quizDto))
        quizRepository = FakeQuizRepository(networkSource = networkSource)
        viewModel = QuizEditorViewModel(SavedStateHandle(), quizRepository, Dispatchers.Main)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `does not load an existing quiz when SavedStateHandle does not have id`() = runTest {
        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizEditorUiState.Editor::class.java)
        )
        assertThat(viewModel.runAsQuizState { questions }, `is`(empty()))
    }

    @Test
    fun `loads an existing quiz when SavedStateHandle has an id`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )

        advanceUntilIdle()
        assertThat(
            viewModel.uiState,
            IsInstanceOf(QuizEditorUiState.Editor::class.java)
        )

        viewModel.runAsQuizState {
            assertThat(title.text, `is`(quizDto.title))
            assertThat(expiration, `is`(quizDto.expiration.toInstant()))
            assertThat(questions, hasSize(2))
            assertThat(allowedUsers, `is`(allowedUsers))
            assertThat(questions[0], IsInstanceOf(QuestionState.MultipleChoice::class.java))
            assertThat(questions[1], IsInstanceOf(QuestionState.FillIn::class.java))
        }
    }

    @Test
    fun `uploadQuiz should set uploadStatus to Success when it succeeds`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )

        advanceUntilIdle()

        viewModel.uploadQuiz()
        advanceUntilIdle()

        assertThat(
            viewModel.runAsEditor { uploadStatus },
            IsInstanceOf(LoadingState.Success::class.java)
        )
    }

    @Test
    fun `uploadQuiz should set loading error when it fails with bad request`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()
        networkSource.failOnNextWith(
            NetworkResult.HttpError(
                400,
                errors = listOf(ApiError(message = "invalid"))
            )
        )

        viewModel.uploadQuiz()
        advanceUntilIdle()
        assertThat(
            viewModel.runAsEditor { uploadStatus },
            IsInstanceOf(LoadingState.Error::class.java)
        )
    }

    @Test
    fun `uploadQuiz should set uiState loading to Error when it fails with unauthorized`() =
        runTest {
            viewModel = QuizEditorViewModel(
                SavedStateHandle().apply {
                    set("quiz", quizId.value)
                }, quizRepository, Dispatchers.Main
            )

            advanceUntilIdle()

            networkSource.failOnNextWith(NetworkResult.HttpError(401))

            viewModel.uploadQuiz()
            advanceUntilIdle()
            assertThat(
                (viewModel.uiState as QuizEditorUiState.Editor).uploadStatus,
                IsInstanceOf(LoadingState.Error::class.java)
            )
        }

    @Test
    fun `uploadQuiz should revalidate and not submit when title empty`() = runTest {
        // empty fresh quiz, should fail because of empty title
        viewModel.uploadQuiz()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.NotStarted::class.java)
        )
        assertThat(
            viewModel.runAsQuizState { title.error },
            `is`(notNullValue())
        )
    }

    @Test
    fun `uploadQuiz should revalidate and not submit when allowedUsers are invalid`() = runTest {
        // Use existing quiz but modified for invalid allowedUsers
        val errorQuiz = quizDto.copy(allowedUsers = listOf("a".repeat(20), "u\$ername"))
        networkSource = FakeQuizNetworkSource(quizzes = listOf(errorQuiz))
        quizRepository = FakeQuizRepository(networkSource = networkSource)
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.uploadQuiz()
        advanceUntilIdle()
        assertThat(
            viewModel.runAsEditor { uploadStatus },
            IsInstanceOf(LoadingState.Error::class.java)
        )
        assertThat(
            viewModel.runAsQuizState { allowedUsersError },
            `is`(notNullValue())
        )
    }

    @Test
    fun `uploadQuiz should allow original expiration if unchanged`() = runTest {
        // Use existing quiz but modified for invalid date
        val errorQuiz =
            quizDto.copy(expiration = Instant.now().minus(10, ChronoUnit.DAYS).toString())

        networkSource = FakeQuizNetworkSource(quizzes = listOf(errorQuiz))
        quizRepository = FakeQuizRepository(networkSource = networkSource)
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.uploadQuiz()
        advanceUntilIdle()
        assertThat(
            viewModel.runAsQuizState { expirationError },
            `is`(nullValue())
        )
    }

    @Test
    fun `uploadQuiz should require at least one question and not submit when errors`() = runTest {
        // use fresh quiz with no questions
        viewModel.uploadQuiz()
        advanceUntilIdle()
        assertThat(
            viewModel.uiState.loading,
            IsInstanceOf(LoadingState.NotStarted::class.java)
        )
        // Should use questionsError to convey invalid number of questions
        assertThat(
            viewModel.runAsQuizState { questionsError },
            `is`(notNullValue())
        )
    }

    @Test
    fun `uploadQuiz should check MC has at least two answers and prompt not empty and answer text not empty and correctAnswer not null`() =
        runTest {
            // Use existing quiz but modified to be invalid
            val errorQuiz =
                quizDto.copy(
                    questions = quizDto.questions.toMutableList().apply {
                        this[0] = (this[0] as QuestionDto.MultipleChoice).copy(
                            text = "",
                            answers = listOf(
                                QuestionDto.MultipleChoice.Answer(""),
                            ),
                            correctAnswer = null,
                        )
                    }
                )

            networkSource = FakeQuizNetworkSource(quizzes = listOf(errorQuiz))
            quizRepository = FakeQuizRepository(networkSource = networkSource)
            viewModel = QuizEditorViewModel(
                SavedStateHandle().apply {
                    set("quiz", quizId.value)
                }, quizRepository, Dispatchers.Main
            )
            advanceUntilIdle()

            viewModel.uploadQuiz()
            advanceUntilIdle()
            assertThat(
                viewModel.runAsEditor { uploadStatus },
                IsInstanceOf(LoadingState.Error::class.java)
            )

            // Should have an "overall" error because MultipleChoice needs at least 2 answers
            assertThat(
                viewModel.runAsQuizState { questions[0].error },
                `is`(notNullValue())
            )

            // Each answer should have an error because they're empty
            viewModel.runAsQuizState {
                (questions[0] as QuestionState.MultipleChoice).answers.forEach {
                    assertThat(
                        it.text.error,
                        `is`(notNullValue())
                    )
                }
            }

            // Should have correctAnswerError because it was null
            assertThat(
                viewModel.runAsQuizState {
                    (questions[0] as QuestionState.MultipleChoice).correctAnswerError
                },
                `is`(notNullValue())
            )

            // Should have questionTextError because the prompt is empty
            assertThat(
                viewModel.runAsQuizState {
                    (questions[0] as QuestionState.MultipleChoice).prompt.error
                },
                `is`(notNullValue())
            )
        }

    @Test
    fun `uploadQuiz should check FillIn question has prompt and correctAnswer`() = runTest {
        // Use existing quiz but modified to be invalid
        val errorQuiz =
            quizDto.copy(
                questions = quizDto.questions.toMutableList().apply {
                    this[0] = QuestionDto.FillIn() // both empty/null
                }
            )

        networkSource = FakeQuizNetworkSource(quizzes = listOf(errorQuiz))
        quizRepository = FakeQuizRepository(networkSource = networkSource)
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.uploadQuiz()
        advanceUntilIdle()
        assertThat(
            viewModel.runAsEditor { uploadStatus },
            IsInstanceOf(LoadingState.Error::class.java)
        )

        // Should have correctAnswer error
        assertThat(
            viewModel.runAsQuizState {
                (questions[0] as QuestionState.FillIn).correctAnswer.error
            },
            `is`(notNullValue())
        )

        // Should have prompt error
        assertThat(
            viewModel.runAsQuizState {
                (questions[0] as QuestionState.FillIn).prompt.error
            },
            `is`(notNullValue())
        )
    }

    @Test
    fun `addQuestion should add question`() = runTest {
        viewModel.runAsQuizState {
            addQuestion()
            advanceUntilIdle()
            assertThat(questions, hasSize(1))
            assertThat(
                questions[0],
                IsInstanceOf(QuestionState.Empty::class.java)
            )
        }
    }

    @Test
    fun `addQuestion should not add question when editing`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()
        viewModel.runAsQuizState {
            addQuestion()
            advanceUntilIdle()
            assertThat(questions, hasSize(quizDto.questions.size))
        }
    }

    @Test
    fun `changeQuestionType should replace question with one of new type`() = runTest {
        advanceUntilIdle()
        viewModel.runAsQuizState {
            addQuestion()
            advanceUntilIdle()

            changeQuestionType(
                0,
                QuestionType.FillIn
            )

            advanceUntilIdle()
            assertThat(questions[0], IsInstanceOf(QuestionState.FillIn::class.java))

            changeQuestionType(
                0,
                QuestionType.MultipleChoice
            )
            advanceUntilIdle()
            assertThat(questions[0], IsInstanceOf(QuestionState.MultipleChoice::class.java))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `changeQuestionType should not allow changing to Empty type`() = runTest {
        viewModel.runAsQuizState {
            addQuestion()
            advanceUntilIdle()

            changeQuestionType(
                0,
                QuestionType.Empty
            )
            advanceUntilIdle()
        }
    }

    @Test
    fun `changeQuestionType should not do anything when editing`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()
        viewModel.runAsQuizState {
            changeQuestionType(
                0,
                QuestionType.FillIn
            )
            assertThat(
                questions[0],
                IsInstanceOf(QuestionState.MultipleChoice::class.java)
            )
        }
    }

    @Test
    fun `deleteQuestion should remove a question at index`() = runTest {
        viewModel.runAsQuizState {
            addQuestion()
            advanceUntilIdle()
            assertThat(
                questions,
                hasSize(1)
            )
            deleteQuestion(0)
            advanceUntilIdle()
            assertThat(
                questions,
                `is`(empty())
            )
        }

    }

    @Test
    fun `deleteQuestion should not throw on invalid index and instead do nothing`() = runTest {
        viewModel.runAsQuizState {
            deleteQuestion(0)
        }
    }

    @Test
    fun `deleteQuestion should not remove a question when editing`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()
        viewModel.runAsQuizState {
            deleteQuestion(0)
            assertThat(
                questions,
                hasSize(quizDto.questions.size)
            )
        }
    }

    @Test
    fun `setTitle should change the title`() = runTest {
        advanceUntilIdle()

        val title = "title123"
        viewModel.runAsQuizState {
            changeTitleText(title)
            advanceUntilIdle()
            assertThat(
                this.title.text,
                `is`(title)
            )
        }
    }

    @Test
    fun `setTitle should change the title when editing`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()
        val title = "title123"
        viewModel.runAsQuizState {
            changeTitleText(title)
            advanceUntilIdle()
            assertThat(
                this.title.text,
                `is`(title)
            )
        }
    }

    @Test
    fun `setExpiration should change the expiration`() = runTest {
        val expiration = Instant.now().plus(10, ChronoUnit.DAYS)
        viewModel.runAsQuizState {
            this.expiration = expiration
            advanceUntilIdle()
            assertThat(
                this.expiration,
                `is`(expiration)
            )
        }
    }

    @Test
    fun `setExpiration should ensure expiration is in the future`() = runTest {
        advanceUntilIdle()

        val expiration = Instant.now().minus(10, ChronoUnit.DAYS)
        viewModel.runAsQuizState {
            this.expiration = expiration
            advanceUntilIdle()
            assertThat(
                this.expirationError,
                `is`(notNullValue())
            )
        }
    }

    @Test
    fun `setExpiration should change the expiration when editing`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()
        val expiration = Instant.now().plus(10, ChronoUnit.DAYS)
        viewModel.runAsQuizState {
            this.expiration = expiration
            advanceUntilIdle()
            assertThat(
                this.expiration,
                `is`(expiration)
            )
        }
    }

    @Test
    fun `setIsPublic should change isPublic`() = runTest {
        viewModel.runAsQuizState {
            isPublic = false
            advanceUntilIdle()
            assertThat(
                isPublic,
                `is`(false)
            )
        }
    }

    @Test
    fun `setIsPublic should change isPublic when editing`() = runTest {
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()
        viewModel.runAsQuizState {
            isPublic = false
            advanceUntilIdle()
            assertThat(
                isPublic,
                `is`(false)
            )
        }
    }

    @Test
    fun `setAllowedUsers should change allowedUsers`() = runTest {
        advanceUntilIdle()

        var allowedUsers = "user1, name2, name3, name4, name5"
        viewModel.runAsQuizState {
            this.allowedUsers = allowedUsers
            advanceUntilIdle()

            assertThat(
                this.allowedUsers,
                `is`(allowedUsers)
            )

            // check trailing comma
            allowedUsers = "user1, "
            this.allowedUsers = allowedUsers
            advanceUntilIdle()

            assertThat(
                this.allowedUsers,
                `is`(allowedUsers)
            )
        }
    }

    @Test
    fun `setAllowedUsers should validate that the input is a comma-separated list`() = runTest {
        advanceUntilIdle()

        // a long username as only value
        var allowedUsers = "reallylongusernameeeeeeeeee"
        viewModel.runAsQuizState {
            this.allowedUsers = allowedUsers
            advanceUntilIdle()
            assertThat(
                this.allowedUsersError,
                `is`(notNullValue())
            )

            // a long username between other usernames
            allowedUsers = "username1, reallylongusername, username2"
            this.allowedUsers = allowedUsers
            advanceUntilIdle()
            assertThat(
                this.allowedUsersError,
                `is`(notNullValue())
            )

            // spaces in one of the names
            allowedUsers = "username1, us er nam e, username2"
            this.allowedUsers = allowedUsers
            advanceUntilIdle()
            assertThat(
                this.allowedUsersError,
                `is`(notNullValue())
            )
        }
    }

    @Test
    fun `setAllowedUsers should change allowedUsers when editing`() = runTest {
        quizRepository = FakeQuizRepository(networkSource = networkSource)
        viewModel = QuizEditorViewModel(
            SavedStateHandle().apply {
                set("quiz", quizId.value)
            }, quizRepository, Dispatchers.Main
        )
        advanceUntilIdle()

        viewModel.runAsQuizState {
            this.allowedUsers = "user1"
            advanceUntilIdle()
            assertThat(
                allowedUsers,
                `is`("user1")
            )
        }
    }

    @Test
    fun `UiState is NoQuiz if ViewModelState loading is Error`() =
        runTest {
            val state = QuizEditorViewModelState(
                quizState = TestQuizStateHolder(),
                loading = LoadingState.Error(FailureReason.UNKNOWN)
            )
            val uiState = QuizEditorUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizEditorUiState.NoQuiz::class.java)
            )
        }

    @Test
    fun `UiState is Creator if ViewModelState not loading or failedToLoad and quizId null`() =
        runTest {
            val state =
                QuizEditorViewModelState(quizState = TestQuizStateHolder(), loading = LoadingState.Success())
            val uiState = QuizEditorUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizEditorUiState.Editor::class.java)
            )
        }

    @Test
    fun `UiState is Editor if ViewModelState not loading or failedToLoad and quizId is not null`() =
        runTest {
            val state = QuizEditorViewModelState(
                quizState = TestQuizStateHolder(),
                loading = LoadingState.Success(),
                quizId = ""
            )
            val uiState = QuizEditorUiState.fromViewModelState(state)
            assertThat(
                uiState,
                IsInstanceOf(QuizEditorUiState.Editor::class.java)
            )
        }
}