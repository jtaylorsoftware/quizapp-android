package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import io.mockk.confirmVerified
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test


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

    private lateinit var responses: SnapshotStateList<ResponseState>

    @Before
    fun beforeEach() {
        responses = mutableStateListOf()
        questions.forEach {
            when (it) {
                is Question.MultipleChoice -> {
                    responses.add(ResponseState(QuestionResponse.MultipleChoice()))
                }
                is Question.FillIn -> {
                    responses.add(ResponseState(QuestionResponse.FillIn()))
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
                onChangeResponse = { _, _ -> },
                onSubmit = {})
        }

        composeTestRule.onAllNodesWithText("Tap an answer to mark it as your choice").assertCountEquals(
            questions.count { it is Question.MultipleChoice }
        )

        questions.forEachIndexed { i, question ->
            composeTestRule.onNodeWithText("${i + 1}. ${question.text}:").assertIsDisplayed()
            when (question) {
                is Question.Empty -> {}
                is Question.MultipleChoice -> {
                    question.answers.forEachIndexed { j, answer ->
                        composeTestRule.onNodeWithText("${j + 1}. ${answer.text}")
                            .assertHasClickAction()
                        composeTestRule
                            .onNodeWithTag("Select answer ${j + 1} for question ${i + 1}", useUnmergedTree = true)
                            .assertHasClickAction()
                    }
                }
                is Question.FillIn -> {
                    composeTestRule.onNodeWithTag("Fill in answer for question ${i + 1}")
                }
            }
        }
    }

    @Test
    fun canSelectMultipleChoiceAnswer() {
        val changeResponse: (Int, ResponseState) -> Unit = { i, r ->
            responses[i] = r
        }
        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                onChangeResponse = changeResponse,
                onSubmit = {})
        }

        composeTestRule.onNodeWithTag("Select answer 2 for question 1", useUnmergedTree = true)
            .performClick()

        assertEquals(1, (responses[0].response as QuestionResponse.MultipleChoice).choice)
    }

    @Test
    fun canEditFillInAnswer() {
        val changeResponse: (Int, ResponseState) -> Unit = { i, r ->
            responses[i] = r
        }
        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                onChangeResponse = changeResponse,
                onSubmit = {})
        }

        val answerText = "my answer"
        composeTestRule.onNodeWithTag("Fill in answer for question 2")
            .performTextInput(answerText)

        assertEquals(answerText, (responses[1].response as QuestionResponse.FillIn).answer)
    }

    @Test
    fun canSubmitResponses() {
        val onSubmit = mockk<() -> Unit>()
        justRun { onSubmit() }

        composeTestRule.setContent {
            QuizFormScreen(
                quiz,
                responses,
                onChangeResponse = { _, _ -> },
                onSubmit = onSubmit)
        }

        composeTestRule.onNodeWithContentDescription("Submit responses").performClick()

        verify(exactly = 1) { onSubmit() }
        confirmVerified(onSubmit)
    }
}