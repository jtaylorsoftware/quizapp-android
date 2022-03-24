package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jtaylorsoftware.quizapp.data.Question
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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
        val onChangeQuizState = mockk<(QuizState) -> Unit>()
        val quizState = slot<QuizState>()
        every { onChangeQuizState(capture(quizState)) } returns Unit

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                emptyList(),
                onChangeQuizState = onChangeQuizState,
                onChangeQuestionType = { _, _ -> },
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {}
            )
        }

        val title = "My Quiz"
        composeTestRule.onNodeWithContentDescription("Edit quiz title").performTextInput(title)

        verify { onChangeQuizState(any()) }
        confirmVerified(onChangeQuizState)

        assertEquals(title, quizState.captured.title.text)
    }

    @Test
    fun whenEditing_shouldHaveEditableQuizTitle() {
        val onChangeQuizState = mockk<(QuizState) -> Unit>()
        val quizState = slot<QuizState>()
        every { onChangeQuizState(capture(quizState)) } returns Unit

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                emptyList(),
                onChangeQuizState = onChangeQuizState,
                onChangeQuestionType = { _, _ -> },
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }

        val title = "My Quiz"
        composeTestRule.onNodeWithContentDescription("Edit quiz title").performTextInput(title)

        verify { onChangeQuizState(any()) }
        confirmVerified(onChangeQuizState)

        assertEquals(title, quizState.captured.title.text)
    }

    @Test
    fun whenCreating_andIsPublic_shouldNotDisplayAllowedUsers() {
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Edit allowed users").assertDoesNotExist()
    }

    @Test
    fun whenEditing_andIsPublic_shouldNotDisplayAllowedUsers() {
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }

        composeTestRule.onNodeWithContentDescription("Edit allowed users").assertDoesNotExist()
    }

    @Test
    fun whenCreating_andIsNotPublic_canEditAllowedUsers() {
        var quizState: QuizState by mutableStateOf(QuizState())
        val onChangeQuizState = spyk<(QuizState) -> Unit>({
            quizState = it.copy()
        })

        composeTestRule.setContent {
            QuizEditorScreen(
                quizState,
                emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = onChangeQuizState,
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {}
            )
        }

        // Uncheck/switch off isPublic (set/on by default)
        composeTestRule.onNodeWithContentDescription("Toggle public quiz").performClick()

        val allowedUsers = "username123"
        composeTestRule.onNodeWithContentDescription("Edit allowed users").performTextInput(allowedUsers)

        verify { onChangeQuizState(any()) }
        confirmVerified(onChangeQuizState)

        assertEquals(allowedUsers, quizState.allowedUsers)
    }

    @Test
    fun whenEditing_andIsNotPublic_canEditAllowedUsers() {
        var quizState: QuizState by mutableStateOf(QuizState())
        val onChangeQuizState = spyk<(QuizState) -> Unit>({
            quizState = it.copy()
        })

        composeTestRule.setContent {
            QuizEditorScreen(
                quizState,
                emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = onChangeQuizState,
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }

        // Uncheck/switch off isPublic (set/on by default)
        composeTestRule.onNodeWithContentDescription("Toggle public quiz").performClick()

        val allowedUsers = "username123"
        composeTestRule.onNodeWithContentDescription("Edit allowed users")
            .performTextInput(allowedUsers)

        verify { onChangeQuizState(any()) }
        confirmVerified(onChangeQuizState)

        assertEquals(allowedUsers, quizState.allowedUsers)
    }

    @Test
    fun whenCreating_canEditExpiration() {
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
            )
        }

        // Verify it exists and can be clicked
        composeTestRule.onNodeWithContentDescription("Change expiration date").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Change expiration time").assertHasClickAction()
    }

    @Test
    fun whenEditing_canEditExpiration() {
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }

        // Verify it exists and can be clicked
        composeTestRule.onNodeWithContentDescription("Change expiration date").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Change expiration time").assertHasClickAction()
    }

    @Test
    fun whenCreating_displaysFAB_andCanAddQuestion_andPickType() {
        val questions = mutableStateListOf<QuestionState>()
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = addQuestion,
                onDeleteQuestion = {},
                onSubmit = {},
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
    fun whenCreating_canAddMultipleEmptyQuestion() {
        val questions = mutableStateListOf<QuestionState>()
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = addQuestion,
                onDeleteQuestion = {},
                onSubmit = {},
            )
        }

        // Click FAB twice, adding two Empty questions
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
    }

    @Test
    fun whenEditing_doesNotDisplayFAB_andCannotAddQuestions() {
        val questions = mutableStateListOf<QuestionState>()
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = addQuestion,
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }

        // No FAB = no adding
        composeTestRule.onNodeWithContentDescription("Add question").assertDoesNotExist()

        verify(exactly = 0) { addQuestion() }
        confirmVerified(addQuestion)
    }

    @Test
    fun whenCreating_oneTypeCanBeSelected() {
        val questions = mutableStateListOf<QuestionState>()
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })
        val changeQuestionType = spyk<(Int, Question.Type) -> Unit>({ index, type ->
            questions[index] = when (type) {
                Question.Type.Empty -> throw IllegalArgumentException("Cannot change to empty")
                Question.Type.FillIn -> QuestionState.FillIn()
                Question.Type.MultipleChoice -> QuestionState.MultipleChoice()
            }
        })
        val editQuestion = spyk<(Int, QuestionState) -> Unit>({ index, question ->
            questions[index] = question
        })
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = changeQuestionType,
                onChangeQuizState = {},
                onEditQuestion = editQuestion,
                onAddQuestion = addQuestion,
                onDeleteQuestion = {},
                onSubmit = {},
            )
        }

        // Add question
        composeTestRule.onNodeWithContentDescription("Add question").performClick()

        // Only one type should ever be selected (technically implicitly guaranteed by
        // how editing questions works)

        // First select MC
        composeTestRule.onNodeWithContentDescription("Multiple choice question").performClick()
        composeTestRule.onNodeWithContentDescription("Multiple choice question").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question")
            .assertIsNotSelected()

        // Then select FillIn
        composeTestRule.onNodeWithContentDescription("Fill in the blank question").performClick()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question")
            .assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Multiple choice question")
            .assertIsNotSelected()

        verify {
            addQuestion()
            changeQuestionType(0, Question.Type.MultipleChoice)
            changeQuestionType(0, Question.Type.FillIn)
        }

        confirmVerified(addQuestion, changeQuestionType)
    }

    @Test
    fun whenCreating_canAddAndDeleteMultipleChoiceAnswers() {
        val questions = mutableStateListOf<QuestionState>()
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })
        val changeQuestionType = spyk<(Int, Question.Type) -> Unit>({ index, type ->
            questions[index] = when (type) {
                Question.Type.Empty -> throw IllegalArgumentException("Cannot change to empty")
                Question.Type.FillIn -> QuestionState.FillIn()
                Question.Type.MultipleChoice -> QuestionState.MultipleChoice()
            }
        })
        val editQuestion = spyk<(Int, QuestionState) -> Unit>({ index, question ->
            questions[index] = question
        })
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuizState = {},
                onChangeQuestionType = changeQuestionType,
                onEditQuestion = editQuestion,
                onAddQuestion = addQuestion,
                onDeleteQuestion = {},
                onSubmit = {},
            )
        }

        // Add MC Question
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
        composeTestRule.onNodeWithContentDescription("Multiple choice question").performClick()

        // Should hint for correct answer
        composeTestRule.onNodeWithText("Tap an answer", substring = true).assertIsDisplayed()

        // Add answer
        composeTestRule.onNodeWithText("Add answer").performClick()

        // Should have editable text, delete button, radio button
        composeTestRule.onNodeWithContentDescription("Pick answer 1")
            .assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Edit answer 1 text")
            .performTextInput("answer")
        composeTestRule.onNodeWithContentDescription("Edit answer 1 text").performImeAction()

        composeTestRule.onNodeWithContentDescription("Delete answer 1").performClick()

        verify {
            addQuestion()
        }

        // Select Type, Add Answer, Edit Answer
        verify(exactly = 3) {
            editQuestion(any(), any())
        }

        confirmVerified(addQuestion, editQuestion)
    }

    @Test
    fun whenCreating_canAddFillInQuestion() {
        val questions = mutableStateListOf<QuestionState>()
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })
        val changeQuestionType = spyk<(Int, Question.Type) -> Unit>({ index, type ->
            questions[index] = when (type) {
                Question.Type.Empty -> throw IllegalArgumentException("Cannot change to empty")
                Question.Type.FillIn -> QuestionState.FillIn()
                Question.Type.MultipleChoice -> QuestionState.MultipleChoice()
            }
        })
        val editQuestion = spyk<(Int, QuestionState) -> Unit>({ index, question ->
            questions[index] = question
        })
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = changeQuestionType,
                onChangeQuizState = {},
                onEditQuestion = editQuestion,
                onAddQuestion = addQuestion,
                onDeleteQuestion = {},
                onSubmit = {},
            )
        }

        // Add FillIn
        composeTestRule.onNodeWithContentDescription("Add question").performClick()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question").performClick()

        // Add answer
        val answer = "The correct answer"
        composeTestRule.onNodeWithContentDescription("Change correct answer", substring = true)
            .performTextInput(answer)

        verify {
            addQuestion()
            editQuestion(0, any())
        }
        confirmVerified(addQuestion, editQuestion)

        assertEquals(answer, (questions[0].question as Question.FillIn).correctAnswer!!)
    }

    @Test
    fun whenEditing_cannotAddOrDeleteMultipleChoiceAnswers() {
        val questions = mutableStateListOf<QuestionState>(
            QuestionState.MultipleChoice(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    ),
                ),
                answerErrors = listOf(null, null)
            )
        )
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })
        val editQuestion = spyk<(Int, QuestionState) -> Unit>({ index, question ->
            questions[index] = question
        })
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = {_,_->},
                onChangeQuizState = {},
                onEditQuestion = editQuestion,
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }

        // Can't add answers
        composeTestRule.onNodeWithContentDescription("Add question").assertDoesNotExist()

        // Can't delete answers
        composeTestRule.onNodeWithContentDescription("Delete answer 1").assertDoesNotExist()

        verify(exactly = 0) {
            addQuestion()
            editQuestion(any(), any())
        }
        confirmVerified(addQuestion)
    }

    @Test
    fun whenCreating_canSelectCorrectMultipleChoiceAnswer() {
        val questions = mutableStateListOf<QuestionState>(
            QuestionState.MultipleChoice(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    ),
                ),
                answerErrors = listOf(null, null)
            )
        )
        val addQuestion = spyk<() -> Unit>({
            questions.add(QuestionState.Empty())
        })
        val editQuestion = spyk<(Int, QuestionState) -> Unit>({ index, question ->
            questions[index] = question
        })
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = editQuestion,
                onAddQuestion = addQuestion,
                onDeleteQuestion = {},
                onSubmit = {},
            )
        }


        // Should have radio button for each one
        composeTestRule.onNodeWithContentDescription("Pick answer 1").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Pick answer 2").assertHasClickAction()

        // Pick 2
        composeTestRule.onNodeWithContentDescription("Pick answer 2").performClick()

        // Should now be the correct choice
        assertEquals(1, (questions[0].question as Question.MultipleChoice).correctAnswer!!)
    }

    @Test
    fun whenEditing_cannotChangeMultipleChoiceCorrectAnswer() {
        val questions = mutableStateListOf<QuestionState>(
            QuestionState.MultipleChoice(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
                answerErrors = listOf(null, null)
            )
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }


        // Should not be clickable
        composeTestRule.onNodeWithContentDescription("Pick answer 1").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Pick answer 2").assertIsNotEnabled()
    }

    @Test
    fun whenEditing_cannotChangeQuestionType() {
        val questions = mutableStateListOf<QuestionState>(
            QuestionState.MultipleChoice(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
                answerErrors = listOf(null, null)
            )
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }


        // Should not exist
        composeTestRule.onNodeWithContentDescription("Multiple choice question").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Fill in the blank question").assertDoesNotExist()
    }


    @Test
    fun whenEditing_cannotChangeFillInAnswer() {
        val questions = mutableStateListOf<QuestionState>(
            QuestionState.FillIn(
                Question.FillIn(correctAnswer = "The correct answer")
            )
        )
        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }


        // Should not be editable
        composeTestRule.onNodeWithText("The correct answer").assertIsNotEnabled()
    }

    @Test
    fun whenCreating_canDeleteQuestion() {
        val questions = mutableStateListOf<QuestionState>(
            QuestionState.MultipleChoice(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
                answerErrors = listOf(null, null)
            )
        )

        val onDelete = mockk<(Int) -> Unit>()
        every { onDelete(any()) } returns Unit

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = onDelete,
                onSubmit = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete question").performClick()

        verify(exactly = 1) { onDelete(0) }
        confirmVerified(onDelete)
    }

    @Test
    fun whenEditing_cannotDeleteQuestion() {
        val questions = mutableStateListOf<QuestionState>(
            QuestionState.MultipleChoice(
                Question.MultipleChoice(
                    answers = listOf(
                        Question.MultipleChoice.Answer("answer text 1"),
                        Question.MultipleChoice.Answer("answer text 2")
                    )
                ),
                answerErrors = listOf(null, null)
            )
        )

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = questions,
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = {},
                isEditing = true,
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete question").assertDoesNotExist()
    }

    @Test
    fun whenCreating_canSubmitQuiz() {
        val onSubmit = mockk<() -> Unit>()
        every { onSubmit() } returns Unit

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = onSubmit,
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

        composeTestRule.setContent {
            QuizEditorScreen(
                QuizState(),
                questions = emptyList(),
                onChangeQuestionType = { _, _ -> },
                onChangeQuizState = {},
                onEditQuestion = { _, _ -> },
                onAddQuestion = {},
                onDeleteQuestion = {},
                onSubmit = onSubmit,
                isEditing = true,
            )
        }

        composeTestRule.onNodeWithContentDescription("Upload quiz").performClick()

        verify(exactly = 1) { onSubmit() }
        confirmVerified(onSubmit)
    }
}