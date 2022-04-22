package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.IsInstanceOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class QuizEditorScreenTest {
    // whenCreating_ -> Test functionality when Editor is in "Create New" mode
    //  - Everything is editable
    // whenEditing_ -> Test functionality when Editor is in "Edit Existing" mode
    //  - Only certain things can be edited:
    //      - isPublic & allowedUsers
    //      - Expiration
    //      - Title
    //      - Any Question prompt
    //      - Any MC Answer text
    //  - This leaves the following as uneditable:
    //      - FillIn Correct Answer
    //      - MC Correct Choice
    //      - Number and order of questions
    //
    // Tests will be heavily duplicated because the editing mode flag is an argument to the composable
    // with no way to change it.

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenCreating_shouldHaveEditableQuizTitle() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        val title = "My Quiz"
        composeTestRule.onNodeWithContentDescription("Edit quiz title").performTextInput(title)

        assertEquals(title, quizState.title.text)
    }

    @Test
    fun whenEditing_shouldHaveEditableQuizTitle() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        val title = "My Quiz"
        composeTestRule.onNodeWithContentDescription("Edit quiz title").performTextInput(title)

        assertEquals(title, quizState.title.text)
    }

    @Test
    fun whenCreating_andIsPublic_shouldNotDisplayAllowedUsers() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Edit allowed users").assertDoesNotExist()
    }

    @Test
    fun whenEditing_andIsPublic_shouldNotDisplayAllowedUsers() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Edit allowed users").assertDoesNotExist()
    }

    @Test
    fun whenCreating_andIsNotPublic_canEditAllowedUsers() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Uncheck/switch off isPublic (set/on by default)
        composeTestRule.onNodeWithContentDescription("Toggle public quiz").performClick()

        val allowedUsers = "username123"
        composeTestRule.onNodeWithContentDescription("Edit allowed users")
            .performTextInput(allowedUsers)

        assertEquals(allowedUsers, quizState.allowedUsers)
    }

    @Test
    fun whenEditing_andIsNotPublic_canEditAllowedUsers() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Uncheck/switch off isPublic (set/on by default)
        composeTestRule.onNodeWithContentDescription("Toggle public quiz").performClick()

        val allowedUsers = "username123"
        composeTestRule.onNodeWithContentDescription("Edit allowed users")
            .performTextInput(allowedUsers)

        assertEquals(allowedUsers, quizState.allowedUsers)
    }

    @Test
    fun whenCreating_canEditExpiration() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Verify it exists and can be clicked
        composeTestRule.onNodeWithContentDescription("Change expiration date")
            .assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Change expiration time")
            .assertHasClickAction()
    }

    @Test
    fun whenEditing_canEditExpiration() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Verify it exists and can be clicked
        composeTestRule.onNodeWithContentDescription("Change expiration date")
            .assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Change expiration time")
            .assertHasClickAction()
    }

    @Test
    fun whenCreating_displaysFAB_andCanAddQuestion_andPickType() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Click FAB, adding question (which will be Empty type)
        composeTestRule.onNodeWithContentDescription("Add question").performClick()

        // Should have indicator for question index "Question 1:"
        composeTestRule.onNodeWithText("Question 1:")

        // Check for type options in added question
        // (a group of IconButtons where one can be selected at a time, like a radio group)
        composeTestRule.onNodeWithContentDescription("Multiple choice question")
            .assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question")
            .assertHasClickAction()
    }

    @Test
    fun submitQuizButtonDisplaysSpinner_whenUploadStatusIsInProgress() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.InProgress,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Uploading quiz").assertIsDisplayed()
    }

    @Test
    fun whenCreating_canAddMultipleEmptyQuestion() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Click FAB twice, adding two Empty questions
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
    }

    @Test
    fun whenEditing_doesNotDisplayFAB_andCannotAddQuestions() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // No FAB = no adding
        composeTestRule.onNodeWithContentDescription("Add question").assertDoesNotExist()

        assertThat(
            quizState.questions,
            hasSize(0)
        )
    }

    @Test
    fun whenCreating_oneTypeCanBeSelected() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Add question
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
        assertThat(
            quizState.questions,
            hasSize(1)
        )

        // Only one type should ever be selected (technically implicitly guaranteed by
        // how editing questions works)

        // First select MC
        composeTestRule.onNodeWithContentDescription("Multiple choice question").performClick()
        composeTestRule.onNodeWithContentDescription("Multiple choice question").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question")
            .assertIsNotSelected()

        assertThat(
            quizState.questions[0],
            `is`(IsInstanceOf(QuestionState.MultipleChoice::class.java))
        )

        // Then select FillIn
        composeTestRule.onNodeWithContentDescription("Fill in the blank question").performClick()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question")
            .assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Multiple choice question")
            .assertIsNotSelected()

        assertThat(
            quizState.questions[0],
            `is`(IsInstanceOf(QuestionState.FillIn::class.java))
        )
    }

    @Test
    fun whenCreating_canAddAndDeleteMultipleChoiceAnswers() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Add MC Question
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
        composeTestRule.onNodeWithContentDescription("Multiple choice question").performClick()

        // Should hint for correct answer
        composeTestRule.onNodeWithText("Tap an answer", substring = true).assertIsDisplayed()

        // Add answer
        composeTestRule.onNodeWithText("Add answer").performClick()
        composeTestRule.waitForIdle()

        assertThat(
            (quizState.questions[0] as QuestionState.MultipleChoice).data.answers,
            hasSize(1)
        )

        // Should have editable text, delete button, radio button
        composeTestRule.onNodeWithContentDescription("Pick answer 1").performClick()
        composeTestRule.waitForIdle()

        assertThat(
            (quizState.questions[0] as QuestionState.MultipleChoice).data.correctAnswer,
            `is`(0)
        )

        composeTestRule.onNodeWithContentDescription("Edit answer 1 text")
            .performTextInput("answer")
        composeTestRule.waitForIdle()

        assertThat(
            (quizState.questions[0] as QuestionState.MultipleChoice).data.answers[0].text,
            `is`("answer")
        )

        // Hide keyboard to ensure delete button is on screen & clickable
        composeTestRule.onNodeWithContentDescription("Edit answer 1 text").performImeAction()

        composeTestRule.onNodeWithContentDescription("Delete answer 1").performClick()
        composeTestRule.waitForIdle()

        assertThat(
            (quizState.questions[0] as QuestionState.MultipleChoice).data.answers,
            hasSize(0)
        )
    }

    @Test
    fun whenCreating_canAddFillInQuestion() {
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }
        // Add FillIn
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question").performClick()

        // Change answer
        val answer = "The correct answer"
        composeTestRule.onNodeWithContentDescription("Change correct answer", substring = true)
            .performTextInput(answer)
        composeTestRule.waitForIdle()

        assertThat(
            (quizState.questions[0] as QuestionState.FillIn).data.correctAnswer,
            `is`(answer)
        )
    }

    @Test
    fun whenEditing_cannotAddOrDeleteMultipleChoiceAnswers() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    ),
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Can't add answers
        composeTestRule.onNodeWithContentDescription("Add question").assertDoesNotExist()

        // Can't delete answers
        composeTestRule.onNodeWithContentDescription("Delete answer 1").assertDoesNotExist()

        val question = quizState.questions[0] as QuestionState.MultipleChoice

        assertThat(
            question.data.answers,
            hasSize(2)
        )
    }

    @Test
    fun whenCreating_canSelectCorrectMultipleChoiceAnswer() {
        val questionData = mutableStateListOf<QuestionState>(
           fromQuestion(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    ),
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }


        // Should have radio button for each one
        composeTestRule.onNodeWithContentDescription("Pick answer 1").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Pick answer 2").assertHasClickAction()

        // Pick 2
        composeTestRule.onNodeWithContentDescription("Pick answer 2").performClick()

        // Should now be the correct choice
        val question = quizState.questions[0] as QuestionState.MultipleChoice

        assertThat(
            question.data.correctAnswer,
            `is`(1)
        )
    }

    @Test
    fun whenCreating_canDeleteCorrectAnswer() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.MultipleChoice(
                    correctAnswer = 2, // start with "answer text 3" as correct
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2"),
                        Question.MultipleChoice.Answer("answer text 3"),
                    ),
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Remove answer text 3, which is currently selected
        composeTestRule.onNodeWithContentDescription("Delete answer 3").performClick()

        // Should now be one less
        assertEquals(
            1,
            (quizState.questions[0].data as Question.MultipleChoice).correctAnswer!!
        )

        // Now remove answer text 2
        composeTestRule.onNodeWithContentDescription("Delete answer 2").performClick()

        // Should now be 0
        assertEquals(
            0,
            (quizState.questions[0].data as Question.MultipleChoice).correctAnswer!!
        )

        // Remove the last answer
        composeTestRule.onNodeWithContentDescription("Delete answer 1").performClick()

        // Should not go negative
        assertEquals(
            0,
            (quizState.questions[0].data as Question.MultipleChoice).correctAnswer!!
        )
    }

    @Test
    fun whenCreating_canDeleteCorrectAnswerOutOfOrder() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.MultipleChoice(
                    correctAnswer = 1, // start with "answer text 2" as correct
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2"),
                        Question.MultipleChoice.Answer("answer text 3"),
                    ),
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Remove answer text 2, which is currently selected
        composeTestRule.onNodeWithContentDescription("Delete answer 2").performClick()

        // Should now be one less
        assertEquals(
            0,
            (quizState.questions[0].data as Question.MultipleChoice).correctAnswer!!
        )

        // Now pick answer 2
        composeTestRule.onNodeWithContentDescription("Pick answer 2").performClick()

        // Should now be set to 1 again
        assertEquals(
            1,
            (quizState.questions[0].data as Question.MultipleChoice).correctAnswer!!
        )

        // Now remove answer 2 (index 1)
        composeTestRule.onNodeWithContentDescription("Delete answer 2").performClick()

        // Should now be 0
        assertEquals(
            0,
            (quizState.questions[0].data as Question.MultipleChoice).correctAnswer!!
        )

        // Remove last answer
        composeTestRule.onNodeWithContentDescription("Delete answer 1").performClick()

        // Should not go negative
        assertEquals(
            0,
            (quizState.questions[0].data as Question.MultipleChoice).correctAnswer!!
        )
    }

    @Test
    fun whenEditing_cannotChangeMultipleChoiceCorrectAnswer() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        // Should not be clickable
        composeTestRule.onNodeWithContentDescription("Pick answer 1").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Pick answer 2").assertIsNotEnabled()
    }

    @Test
    fun whenEditing_cannotChangeQuestionType() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }


        // Should not exist
        composeTestRule.onNodeWithContentDescription("Multiple choice question")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question")
            .assertDoesNotExist()
    }


    @Test
    fun whenEditing_cannotChangeFillInAnswer() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.FillIn(correctAnswer = "The correct answer")
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }


        // Should not be editable
        composeTestRule.onNodeWithText("The correct answer").assertIsNotEnabled()
    }

    @Test
    fun whenCreating_canDeleteQuestion() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete question").performClick()

        assertThat(quizState.questions, hasSize(0))
    }

    @Test
    fun whenEditing_cannotDeleteQuestion() {
        val questionData = mutableStateListOf<QuestionState>(
            fromQuestion(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
            )
        )
        val quizState = TestQuizStateHolder(questions = questionData)
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete question").assertDoesNotExist()
    }

    @Test
    fun whenCreating_canSubmitQuiz() {
        val onSubmit = mockk<() -> Unit>()
        every { onSubmit() } returns Unit
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = false,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = onSubmit
            )
        }

        composeTestRule.onNodeWithContentDescription("Upload quiz").performClick()

        verify(exactly = 1) { onSubmit() }
        confirmVerified(onSubmit)
    }

    @Test
    fun whenEditing_canSubmitQuiz() {
        val onSubmit = mockk<() -> Unit>()
        every { onSubmit() } returns Unit
        val quizState = TestQuizStateHolder()
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.NotStarted,
            uploadStatus = LoadingState.NotStarted,
            isEditing = true,
            quizState = quizState,
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                uiState = uiState,
                onSubmit = onSubmit
            )
        }

        composeTestRule.onNodeWithContentDescription("Upload quiz").performClick()

        verify(exactly = 1) { onSubmit() }
        confirmVerified(onSubmit)
    }
}