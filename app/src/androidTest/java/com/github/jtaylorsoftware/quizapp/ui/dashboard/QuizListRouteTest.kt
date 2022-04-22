package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.material.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.confirmVerified
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class QuizListRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysQuizListScreen_whenUiStateIsQuizList() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = listOf(QuizListing(title = "TESTQUIZ")),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    navigateToResults = {},
                    navigateToEditor = {},
                    onDeleteQuiz = {},
                )
            }
        }

        composeTestRule.onNodeWithText("TESTQUIZ", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysMessage_whenUiStateNoQuizzesAndLoadingIsSuccess() {
        val uiState = QuizListUiState.NoQuizzes(
            loading = LoadingState.Success(),
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    navigateToResults = {},
                    navigateToEditor = {},
                    onDeleteQuiz = {},
                )
            }
        }

        composeTestRule.onNodeWithText(
            "No Quizzes",
            ignoreCase = true,
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun displaysLoadingProgress_whenUiStateNoQuizzesAndLoadingInProgress() {
        val uiState = QuizListUiState.NoQuizzes(
            loading = LoadingState.InProgress,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    navigateToResults = {},
                    navigateToEditor = {},
                    onDeleteQuiz = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Loading in progress").assertIsDisplayed()
    }

    @Test
    fun displaysErrorScreen_whenUiStateNoQuizzesAndLoadingError() {
        val uiState = QuizListUiState.NoQuizzes(
            loading = LoadingState.Error(FailureReason.UNKNOWN),
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    navigateToResults = {},
                    navigateToEditor = {},
                    onDeleteQuiz = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText(FailureReason.UNKNOWN.value, substring = true)
            .assertCountEquals(2)
    }

    @Test
    fun callsOnRefresh_whenDeleteQuizStatusIsSuccess() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = listOf(QuizListing(title = "TESTQUIZ")),
            deleteQuizStatus = LoadingState.Success(),
        )
        val onRefresh = mockk<() -> Unit>()
        justRun { onRefresh() }
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = onRefresh,
                    navigateToResults = {},
                    navigateToEditor = {},
                    onDeleteQuiz = {},
                    bottomNavigation = { Text("BottomNavigation") }
                )
            }
        }

        verify(exactly = 1) {
            onRefresh()
        }
        confirmVerified(onRefresh)
    }

    @Test
    fun displaysBottomNavigation() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = listOf(QuizListing(title = "TESTQUIZ")),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    navigateToResults = {},
                    navigateToEditor = {},
                    onDeleteQuiz = {},
                    bottomNavigation = { Text("BottomNavigation") }
                )
            }
        }

        composeTestRule.onNodeWithText("BottomNavigation").assertIsDisplayed()
    }

    @Test
    fun displaysNewQuizFAB() {
        val uiState = QuizListUiState.QuizList(
            loading = LoadingState.NotStarted,
            data = listOf(QuizListing(title = "TESTQUIZ")),
            deleteQuizStatus = LoadingState.NotStarted,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizListRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    navigateToResults = {},
                    navigateToEditor = {},
                    onDeleteQuiz = {},
                    bottomNavigation = { Text("BottomNavigation") }
                )
            }
        }

        composeTestRule.onNodeWithText("BottomNavigation").assertIsDisplayed()
    }

}