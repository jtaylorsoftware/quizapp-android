package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jtaylorsoftware.quizapp.data.QuizResultListing
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.*
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val results = listOf(
        QuizResultListing(
            id = "123",
            quiz = "123",
            quizTitle = "Quiz 1",
            score = 0.5f,
            createdBy = "user1",
        ),
        QuizResultListing(
            id = "456",
            quiz = "456",
            quizTitle = "Quiz 2",
            score = 1.0f,
            createdBy = "user2",
        ),
    )

    @Test
    fun resultList_shouldDisplayHeader() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultScreen(results, {})
            }
        }

        composeTestRule.onNodeWithText("Your Quiz Results").assertIsDisplayed()
    }

    @Test
    fun resultList_hasResults_displaysAllResults() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultScreen(results, {})
            }
        }

        results.forEach {
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
        val navigateToDetails = mockk<(String) -> Unit>()
        val quizId = slot<String>()
        every { navigateToDetails(capture(quizId)) } returns Unit

        composeTestRule.setContent {
            QuizAppTheme {
                ResultScreen(results.subList(0, 1), navigateToDetails)
            }
        }

        // Press listing for details
        composeTestRule.onNodeWithText(results[0].quizTitle, substring = true).performClick()

        // Should've called with id
        verify(exactly = 1) { navigateToDetails(any()) }
        confirmVerified(navigateToDetails)

        Assert.assertEquals(results[0].quiz, quizId.captured)
    }

    @Test
    fun resultDetail_displaysResultData() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Check header displayed
        composeTestRule.onNodeWithText("\"${result.username}\"'s results for \"${result.quizTitle}\"")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Overall score: ${result.score}%").assertIsDisplayed()
    }

    @Test
    fun resultDetail_displaysQuestions() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Check the question prompts are displayed
        composeTestRule.onNodeWithText(form.questions[0].text).assertIsDisplayed()
        composeTestRule.onNodeWithText(form.questions[1].text).assertIsDisplayed()
    }

    @Test
    fun resultDetail_whenQuestionMultipleChoice_displaysMultipleAnswers() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("1. ${form.questions[0].text}").assertIsDisplayed()

        // Should display each answer, with choice graded and correct answer indicated
        for ((index, answer) in form.questions[0].answers.withIndex()) {
            composeTestRule.onNodeWithText("$index. ${answer.text}").assertIsDisplayed()
            // Only the choice and the correct answer get an icon
            if (results.answers[index].isCorrect) {
                // Correct answer, even if not the choice gets a "Correct" icon
                composeTestRule.onNodeWithContentDescription("Correct answer").assertIsDisplayed()
            } else if (form.questions[0].choice == index) {
                // Incorrect answer choice should get an "Incorrect" icon
                composeTestRule.onNodeWithContentDescription("Incorrect answer").assertIsDisplayed()
            }
        }
    }

    @Test
    fun resultDetail_whenQuestionFillInAndIncorrect_displaysGradedAnswerWithIcon() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("1. ${form.questions[0].text}").assertIsDisplayed()

        // Should display the user's answer and an "Incorrect" icon
        composeTestRule.onNodeWithText("Your answer: ${form.questions[0].answer}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Incorrect answer").assertIsDisplayed()
    }

    @Test
    fun resultDetail_whenQuestionFillInAndCorrect_displaysGradedAnswerWithIcon() {
        composeTestRule.setContent {
            QuizAppTheme {
                ResultDetail(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("1. ${form.questions[0].text}").assertIsDisplayed()

        // Should display the user's answer and a "Correct" icon
        composeTestRule
            .onNodeWithText("Your answer: ${form.questions[0].answer}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Correct answer").assertIsDisplayed()
    }
}