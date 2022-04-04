package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit


class QuizScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val quizzes = listOf(
        // Expired, created 5 days ago
        QuizListing(
            id = ObjectId("123"),
            date = Instant.now().minus(5, ChronoUnit.DAYS),
            expiration = Instant.now().minus(3, ChronoUnit.DAYS),
            title = "Quiz 1",
            questionCount = 1,
            resultsCount = 1,
        ),
        // Not expired, created 5 months ago
        QuizListing(
            id = ObjectId("456"),
            date = LocalDateTime.now().minusMonths(5)
                .atZone(ZoneId.systemDefault()).toInstant(),
            title = "Quiz 2",
            questionCount = 2,
            resultsCount = 2,
        ),
        // Not expired, created 5 years ago
        QuizListing(
            id = ObjectId("789"),
            date = LocalDateTime.now().minusYears(5)
                .atZone(ZoneId.systemDefault()).toInstant(),
            title = "Quiz 3",
            questionCount = 3,
            resultsCount = 3,
        ),
    )

    @Test
    fun shouldDisplayHeader() {
        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes, {}, {}, {})
            }
        }

        composeTestRule.onNodeWithText("Your Quizzes").assertIsDisplayed()
    }

    @Test
    fun displaysEachQuizListing() {
        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes, {}, {}, {})
            }
        }

        quizzes.forEach { quiz ->
            // Listing data should display
            composeTestRule.onNodeWithText(quiz.title, substring = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("${quiz.questionCount} Question", substring = true)
                .assertIsDisplayed()
            composeTestRule.onNodeWithText("${quiz.resultsCount} Response", substring = true)
                .assertIsDisplayed()

            // Link text displayed
            // TODO - test if it's an actual link
            composeTestRule.onNodeWithText("/quizzes/${quiz.id}", substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun whenQuizExpired_showsText() {
        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), {}, {}, {})
            }
        }

        composeTestRule.onNodeWithText("Expired").assertIsDisplayed()
    }

    @Test
    fun whenQuizNotExpired_doesNotShowExpiredText() {
        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(1, 2), {}, {}, {})
            }
        }

        composeTestRule.onNodeWithText("Expired").assertDoesNotExist()
    }

    @Test
    fun whenQuizCreatedDaysAgo_displaysCreatedDaysAgo() {
        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), {}, {}, {})
            }
        }

        // First quiz - 5 days ago
        composeTestRule.onNodeWithText("Created 5 days ago").assertIsDisplayed()
    }

    @Test
    fun whenQuizCreatedMonthsAgo_displaysCreatedMonthsAgo() {
        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(1, 2), {}, {}, {})
            }
        }

        // Second quiz - 5 months ago
        composeTestRule.onNodeWithText("Created 5 months ago").assertIsDisplayed()
    }

    @Test
    fun whenQuizCreatedYearsAgo_displaysCreatedYearsAgo() {
        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(2, 3), {}, {}, {})
            }
        }

        // Third quiz - 5 years ago
        composeTestRule.onNodeWithText("Created 5 years ago").assertIsDisplayed()
    }

    @Test
    fun whenClickDeleteIcon_callsDeleteQuiz() {
        var quizId: ObjectId? = null
        val onDeleteQuiz:(ObjectId) -> Unit = {
            quizId = it
        }

        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), onDeleteQuiz = onDeleteQuiz, {}, {})
            }
        }

        // Press Delete button
        composeTestRule.onNodeWithContentDescription("Delete Quiz").performClick()

        assertEquals(quizzes[0].id, requireNotNull(quizId))
    }

    @Test
    fun whenClickEditIcon_callsNavigateToEditor() {
        var quizId: ObjectId? = null
        val navigateToEditor: (ObjectId) -> Unit = {
             quizId = it
        }

        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), {}, navigateToEditor = navigateToEditor, {})
            }
        }

        // Press Edit button
        composeTestRule.onNodeWithContentDescription("Edit Quiz").performClick()

        assertEquals(quizzes[0].id, requireNotNull(quizId))
    }

    @Test
    fun whenClickResultsIcon_callsNavigateToResults() {
        var quizId: ObjectId? = null
        val navigateToResults: (ObjectId) -> Unit = {
            quizId = it
        }

        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), {}, {}, navigateToResults = navigateToResults)
            }
        }

        // Press View Results button
        composeTestRule.onNodeWithContentDescription("View Results").performClick()

        assertEquals(quizzes[0].id, requireNotNull(quizId))
    }
}