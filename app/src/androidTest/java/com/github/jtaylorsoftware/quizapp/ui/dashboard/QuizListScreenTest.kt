package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class QuizListScreenTest {
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
    fun displaysEachQuizListing() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes,
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = {},
                )
            }
        }

        quizzes.forEach { quiz ->
            // Listing data should display
            composeTestRule.onNodeWithText(quiz.title, substring = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("${quiz.questionCount} questions", substring = true)
                .assertIsDisplayed()
            composeTestRule.onNodeWithText("${quiz.resultsCount} responses", substring = true)
                .assertIsDisplayed()

            // Link text displayed
            composeTestRule.onNodeWithText("Link: quizzes/${quiz.id.value}", useUnmergedTree = true).run {
                assertIsDisplayed()
                performTouchInput {
                    click(percentOffset(0.5f, 0.2f)) // Click on the clickable URL part of the text
                }
            }
        }
    }

    @Test
    fun whenQuizExpired_showsText() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(0, 1),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Expired").assertIsDisplayed()
    }

    @Test
    fun whenQuizNotExpired_doesNotShowExpiredText() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(1, 2),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Expired").assertDoesNotExist()
    }

    @Test
    fun whenQuizCreatedDaysAgo_displaysCreatedDaysAgo() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(0, 1),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = {},
                )
            }
        }

        // First quiz - 5 days ago
        composeTestRule.onNodeWithText("Created 5 days ago").assertIsDisplayed()
    }

    @Test
    fun whenQuizCreatedMonthsAgo_displaysCreatedMonthsAgo() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(1, 2),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = {},
                )
            }
        }

        // Second quiz - 5 months ago
        composeTestRule.onNodeWithText("Created 5 months ago").assertIsDisplayed()
    }

    @Test
    fun whenQuizCreatedYearsAgo_displaysCreatedYearsAgo() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(2, 3),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = {},
                )
            }
        }

        // Third quiz - 5 years ago
        composeTestRule.onNodeWithText("Created 5 years ago").assertIsDisplayed()
    }

    @Test
    fun whenClickDeleteIcon_callsDeleteQuiz() {
        var quizId: ObjectId? = null
        val onDeleteQuiz: (ObjectId) -> Unit = {
            quizId = it
        }

        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(0, 1),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = onDeleteQuiz,
                    navigateToEditor = {},
                    navigateToResults = {},
                )
            }
        }

        // Press Delete button
        composeTestRule.onNodeWithContentDescription("Delete Quiz").performClick()

        // Confirm dialog
        composeTestRule.onNodeWithText("Delete quiz", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()

        assertEquals(quizzes[0].id, requireNotNull(quizId))
    }

    @Test
    fun whenClickEditIcon_callsNavigateToEditor() {
        var quizId: ObjectId? = null
        val navigateToEditor: (ObjectId?) -> Unit = {
            quizId = it
        }

        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(0, 1),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = navigateToEditor,
                    navigateToResults = {},
                )
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

        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = quizzes.subList(0, 1),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListScreen(
                    uiState = uiState,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = navigateToResults,
                )
            }
        }

        // Press View Results button
        composeTestRule.onNodeWithContentDescription("View Results").performClick()

        assertEquals(quizzes[0].id, requireNotNull(quizId))
    }
}