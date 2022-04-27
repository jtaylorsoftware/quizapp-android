package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.models.*
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test


class QuizResultListScreenTest {
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
    fun resultListForQuiz_shouldDisplayHeader() {
        val uiState = QuizResultListUiState.ListForQuiz(
            loading = LoadingState.NotStarted,
            data = listings,
            quizTitle = "TESTQUIZ"
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListScreen(
                    uiState = uiState,
                    navigateToDetails = { _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText("Results for \"TESTQUIZ\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap a Result to view graded questions", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun resultList_hasResults_displaysAllResults() {
        val uiState = QuizResultListUiState.ListForUser(
            loading = LoadingState.NotStarted,
            data = listings,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListScreen(
                    uiState = uiState,
                    navigateToDetails = { _, _ -> },
                )
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
        var quizId: ObjectId? = null
        var userId: ObjectId? = null
        val navigateToDetails: (ObjectId, ObjectId) -> Unit = { quiz, user ->
            quizId = quiz
            userId = user
        }

        val uiState = QuizResultListUiState.ListForUser(
            loading = LoadingState.NotStarted,
            data = listings,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListScreen(
                    uiState = uiState,
                    navigateToDetails = navigateToDetails,
                )
            }
        }

        // Press listing for details
        composeTestRule.onNodeWithText(listings[0].quizTitle, substring = true).performClick()

        assertThat(quizId, `is`(listings[0].quiz))
        assertThat(userId, `is`(listings[0].user))
    }

    @Test
    fun resultDetail_displaysResultData() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailScreen(result, form)
            }
        }

        // Check header displayed
        composeTestRule.onNodeWithText("${result.username}'s results").assertIsDisplayed()
        composeTestRule.onNodeWithText("for \"${result.quizTitle}\"").assertIsDisplayed()
        composeTestRule.onNodeWithText("${"%.2f".format(result.score * 100)}%")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Score ${"%.2f".format(result.score * 100)}%")
            .assertIsDisplayed()
    }

    @Test
    fun resultDetail_displaysQuestions() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailScreen(result, form)
            }
        }

        // Check the question prompts are displayed
        composeTestRule.onNodeWithText("Question 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText(form.questions[0].text).assertIsDisplayed()
        composeTestRule.onNodeWithText("Question 2.").assertIsDisplayed()
        composeTestRule.onNodeWithText(form.questions[1].text).assertIsDisplayed()
    }

    @Test
    fun resultDetail_whenQuestionMultipleChoice_displaysMultipleAnswers() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailScreen(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("Question 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText(form.questions[0].text).assertIsDisplayed()

        // Should display each answer, with choice graded and correct answer indicated
        for ((index, answer) in (form.questions[0] as Question.MultipleChoice).answers.withIndex()) {
            composeTestRule.onNodeWithText("${index + 1}.").assertIsDisplayed()
            composeTestRule.onNodeWithText(answer.text).assertIsDisplayed()
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
                QuizResultDetailScreen(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("Question 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText(form.questions[0].text).assertIsDisplayed()

        // Should display the user's answer and an "Incorrect" icon
        composeTestRule.onNodeWithText((result.answers[1] as GradedAnswer.FillIn).answer)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Incorrect answer").assertIsDisplayed()
    }

    @Test
    fun resultDetail_whenQuestionFillInAndCorrect_displaysGradedAnswerWithIcon() {
        val result = results[0]
        val form = quizzes[0]
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailScreen(result, form)
            }
        }

        // Should display prompt
        composeTestRule.onNodeWithText("Question 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText(form.questions[0].text).assertIsDisplayed()

        // Should display the user's answer and a "Correct" icon
        composeTestRule
            .onNodeWithText((result.answers[1] as GradedAnswer.FillIn).answer)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Correct answer").assertIsDisplayed()
    }
}