package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jtaylorsoftware.quizapp.data.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.util.isInPast
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class QuizScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val quizzes = listOf(
        // Expired, created 5 days ago
        QuizListing(
            id = "123",
            date = Instant.now().minus(5, ChronoUnit.DAYS),
            expiration = Instant.now().minus(3, ChronoUnit.DAYS),
            title = "Quiz 1",
            questionCount = 1,
            resultsCount = 1,
        ),
        // Not expired, created 5 months ago
        QuizListing(
            id = "456",
            date = LocalDateTime.now().minusMonths(5)
                .atZone(ZoneId.systemDefault()).toInstant(),
            title = "Quiz 2",
            questionCount = 2,
            resultsCount = 2,
        ),
        // Not expired, created 5 years ago
        QuizListing(
            id = "789",
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
        val onDeleteQuiz = mockk<(String) -> Unit>()
        val quizId = slot<String>()
        every { onDeleteQuiz(capture(quizId)) } returns Unit

        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), onDeleteQuiz = onDeleteQuiz, {}, {})
            }
        }

        // Press Delete button
        composeTestRule.onNodeWithContentDescription("Delete Quiz").performClick()

        // Should've called with id
        verify(exactly = 1) { onDeleteQuiz(any()) }
        confirmVerified(onDeleteQuiz)

        assertEquals(quizzes[0].id, quizId.captured)
    }

    @Test
    fun whenClickEditIcon_callsNavigateToEditor() {
        val navigateToEditor = mockk<(String) -> Unit>()
        val quizId = slot<String>()
        every { navigateToEditor(capture(quizId)) } returns Unit

        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), {}, navigateToEditor = navigateToEditor, {})
            }
        }

        // Press Edit button
        composeTestRule.onNodeWithContentDescription("Edit Quiz").performClick()

        // Should've called with id
        verify(exactly = 1) { navigateToEditor(any()) }
        confirmVerified(navigateToEditor)

        assertEquals(quizzes[0].id, quizId.captured)
    }

    @Test
    fun whenClickResultsIcon_callsNavigateToResults() {
        val navigateToResults = mockk<(String) -> Unit>()
        val quizId = slot<String>()
        every { navigateToResults(capture(quizId)) } returns Unit

        composeTestRule.setContent {
            QuizAppTheme {
                QuizScreen(quizzes.subList(0, 1), {}, {}, navigateToResults = navigateToResults)
            }
        }

        // Press View Results button
        composeTestRule.onNodeWithContentDescription("View Results").performClick()

        // Should've called with id
        verify(exactly = 1) { navigateToResults(any()) }
        confirmVerified(navigateToResults)

        assertEquals(quizzes[0].id, quizId.captured)
    }
}