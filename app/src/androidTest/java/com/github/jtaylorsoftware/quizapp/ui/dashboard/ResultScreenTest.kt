package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.models.*
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.confirmVerified
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class ResultScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val listings = listOf(
        QuizResultListing(
            id = ObjectId("123"),
            quiz = ObjectId("123"),
            quizTitle = "Quiz 1",
            score = 0.5f,
            createdBy = "user1",
        ),
        QuizResultListing(
            id = ObjectId("456"),
            quiz = ObjectId("456"),
            quizTitle = "Quiz 2",
            score = 1.0f,
            createdBy = "user2",
        ),
    )

    private val results = listOf(
        QuizResult(
            id = ObjectId("result123"),
            quiz = ObjectId("quiz123"),
            quizTitle = "Quiz 1",
            user = ObjectId("user123"),
            username = "username123",
            createdBy = "username456",
            score = 0.5f,
            answers = listOf(
                GradedAnswer.MultipleChoice(isCorrect = true, choice = 1, correctAnswer = 1),
                GradedAnswer.FillIn(isCorrect = false, answer = "xyz", correctAnswer = "abcdef")
            )
        )
    )

    private val quizzes = listOf(
        QuizForm(
            id = ObjectId("quiz123"),
            createdBy = "username456",
            title = "Quiz 1",
            questions = listOf(
                Question.MultipleChoice(
                    "Question Prompt 1", correctAnswer = 1, answers = listOf(
                        Question.MultipleChoice.Answer("Answer 1"),
                        Question.MultipleChoice.Answer("Answer 2")
                    )
                ),
                Question.FillIn(text = "Question Prompt 2", correctAnswer = "abcdef")
            )
        )
    )

    @Test
    fun resultList_shouldDisplayHeader() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultScreen(listings, {})
            }
        }

        composeTestRule.onNodeWithText("Your Quiz Results").assertIsDisplayed()
    }

    @Test
    fun resultList_hasResults_displaysAllResults() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultScreen(listings, {})
            }
        }

        listings.forEach {
            // Should display title, quiz creator, and score
            composeTestRule.onNodeWithText(it.quizTitle, substring = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("by ${it.createdBy}").assertIsDisplayed()
            composeTestRule.onNodeWithText("%.2f".format(it.score * 100), substring = true)
                .assertIsDisplayed()

            // Entire item should also be clickable
            composeTestRule.onNodeWithText(it.quizTitle, substring = true).assertHasClickAction()
        }
    }

    @Test
    fun resultList_whenClickDetails_callsNavigateToResultDetails() {
        var resultId: ObjectId? = null
        val navigateToDetails: (ObjectId) -> Unit = {
            resultId = it
        }

        composeTestRule.setContent {
            QuizAppTheme {
                ResultScreen(listings.subList(0, 1), navigateToDetails)
            }
        }

        // Press listing for details
        composeTestRule.onNodeWithText(listings[0].quizTitle, substring = true).performClick()

        assertEquals(listings[0].quiz, requireNotNull(resultId))
    }

    @Test
    fun resultDetail_displaysResultData() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Check header displayed
        composeTestRule.onNodeWithText("${result.username}'s results for \"${result.quizTitle}\"")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Overall score: ${"%.2f".format(result.score * 100)}%")
            .assertIsDisplayed()
    }

    @Test
    fun resultDetail_displaysQuestions() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Check the question prompts are displayed
        composeTestRule.onNodeWithText("1. ${form.questions[0].text}").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. ${form.questions[1].text}").assertIsDisplayed()
    }

    @Test
    fun resultDetail_whenQuestionMultipleChoice_displaysMultipleAnswers() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("1. ${form.questions[0].text}").assertIsDisplayed()

        // Should display each answer, with choice graded and correct answer indicated
        for ((index, answer) in (form.questions[0] as Question.MultipleChoice).answers.withIndex()) {
            composeTestRule.onNodeWithText("${index + 1}. ${answer.text}").assertIsDisplayed()
            // Only the choice and the correct answer get an icon
            if (result.answers[index].isCorrect) {
                // Correct answer, even if not the choice gets a "Correct" icon
                composeTestRule.onNodeWithContentDescription("Correct answer").assertIsDisplayed()
            } else if ((result.answers[0] as GradedAnswer.MultipleChoice).choice == index) {
                // Incorrect answer choice should get an "Incorrect" icon
                composeTestRule.onNodeWithContentDescription("Incorrect answer").assertIsDisplayed()
            }
        }
    }

    @Test
    fun resultDetail_whenQuestionFillInAndIncorrect_displaysGradedAnswerWithIcon() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("1. ${form.questions[0].text}").assertIsDisplayed()

        // Should display the user's answer and an "Incorrect" icon
        composeTestRule.onNodeWithText("Your answer: ${(result.answers[1] as GradedAnswer.FillIn).answer}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Incorrect answer").assertIsDisplayed()
    }

    @Test
    fun resultDetail_whenQuestionFillInAndCorrect_displaysGradedAnswerWithIcon() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("1. ${form.questions[0].text}").assertIsDisplayed()

        // Should display the user's answer and a "Correct" icon
        composeTestRule
            .onNodeWithText("Your answer: ${(result.answers[1] as GradedAnswer.FillIn).answer}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Correct answer").assertIsDisplayed()
    }
}