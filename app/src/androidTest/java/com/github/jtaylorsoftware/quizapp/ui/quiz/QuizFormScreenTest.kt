package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jtaylorsoftware.quizapp.data.Question
import com.github.jtaylorsoftware.quizapp.data.QuizForm
import com.github.jtaylorsoftware.quizapp.data.Response
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class QuizFormScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val questions = listOf(
        Question.MultipleChoice(
            text = "MC Question 1", answers = listOf(
                Question.MultipleChoice.Answer("MC Answer 1-1"),
                Question.MultipleChoice.Answer("MC Answer 1-2"),
                Question.MultipleChoice.Answer("MC Answer 1-3"),
            )
        ),
        Question.FillIn(text = "Fill Question 1"),
        Question.FillIn(text = "Fill Question 2"),
        Question.MultipleChoice(
            text = "MC Question 2", answers = listOf(
                Question.MultipleChoice.Answer("MC Answer 2-1"),
                Question.MultipleChoice.Answer("MC Answer 2-2"),
                Question.MultipleChoice.Answer("MC Answer 2-3"),
            )
        ),
        Question.FillIn(text = "Fill Question 3"),
    )
    private val quiz = QuizForm(
        createdBy = "Username123",
        title = "Quiztitle123",
        questions = questions,
    )

    private lateinit var responses: SnapshotStateList<Response>
    private lateinit var responseErrors: SnapshotStateList<String?>

    @Before
    fun beforeEach() {
        responses = mutableStateListOf()
        questions.forEach {
            responseErrors.add(null)
            when (it) {
                is Question.MultipleChoice -> {
                    responses.add(Response.MultipleChoice())
                }
                is Question.FillIn -> {
                    responses.add(Response.FillIn())
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    @Test
    fun shouldDisplayHeader() {
        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                responseErrors,
                onChangeResponse = { _, _ -> },
                onSubmit = {})
        }

        composeTestRule.onNodeWithText(quiz.title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("by ${quiz.createdBy}", substring = true).assertIsDisplayed()
    }

    @Test
    fun shouldDisplayEachQuestion() {
        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                responseErrors,
                onChangeResponse = { _, _ -> },
                onSubmit = {})
        }

        questions.forEachIndexed { i, question ->
            composeTestRule.onNodeWithText("${i + 1}. ${question.text}:").assertIsDisplayed()
            when (question) {
                is Question.Empty -> {}
                is Question.MultipleChoice -> {
                    composeTestRule.onNodeWithText("Tap an answer to mark it as your choice")
                        .assertIsDisplayed()
                    question.answers.forEachIndexed { j, answer ->
                        composeTestRule.onNodeWithText("${j + 1}. ${answer.text}")
                            .assertHasClickAction()
                        composeTestRule.onNodeWithContentDescription("Select answer ${j + 1} to be your choice for question ${i + 1}")
                            .assertHasClickAction()
                    }
                }
                is Question.FillIn -> {
                    composeTestRule.onNodeWithContentDescription("Your answer to fill in question ${i + 1}")
                }
            }
        }
    }

    @Test
    fun canSelectMultipleChoiceAnswer() {
        val changeResponse: (Int, Response) -> Unit = { i, r ->
            responses[i] = r
        }
        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                responseErrors,
                onChangeResponse = changeResponse,
                onSubmit = {})
        }

        composeTestRule.onNodeWithContentDescription("Select answer 2 to be your choice to be your choice for question 1")
            .performClick()

        assertEquals(1, (responses[0] as Response.MultipleChoice).choice)
    }

    @Test
    fun canEditFillInAnswer() {
        val changeResponse: (Int, Response) -> Unit = { i, r ->
            responses[i] = r
        }
        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                responseErrors,
                onChangeResponse = changeResponse,
                onSubmit = {})
        }

        val answerText = "my answer"
        composeTestRule.onNodeWithContentDescription("Your answer to fill in question 1")
            .performTextInput(answerText)

        assertEquals(answerText, (responses[1] as Response.FillIn).answer)
    }

    @Test
    fun canSubmitResponses() {
        val onSubmit = mockk<() -> Unit>()
        justRun { onSubmit() }

        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                responseErrors,
                onChangeResponse = { _, _ -> },
                onSubmit = {})
        }

        composeTestRule.onNodeWithContentDescription("Submit responses").performClick()

        verify(exactly = 1) { onSubmit() }
        confirmVerified(onSubmit)
    }
}