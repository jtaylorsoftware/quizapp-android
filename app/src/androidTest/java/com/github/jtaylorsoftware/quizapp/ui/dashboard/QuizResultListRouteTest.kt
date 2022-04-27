package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.material.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.junit.Rule
import org.junit.Test

class QuizResultListRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysQuizResultList_whenUiStateIsListForUser() {
        val uiState = QuizResultListUiState.ListForUser(
            loading = LoadingState.NotStarted,
            data = listOf(QuizResultListing(quizTitle = "TEST"))
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListRoute(
                    uiState = uiState,
                    navigateToDetailScreen = { _, _ -> },
                    isRefreshing = false,
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onNodeWithText("TEST", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysQuizResultList_whenUiStateIsListForQuiz() {
        val uiState = QuizResultListUiState.ListForQuiz(
            loading = LoadingState.NotStarted,
            data = listOf(QuizResultListing(username = "TESTUSER")),
            quizTitle = "TEST"
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListRoute(
                    uiState = uiState,
                    navigateToDetailScreen = { _, _ -> },
                    isRefreshing = false,
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onNodeWithText("TESTUSER", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysLoadingProgress_whenUiStateIsNoQuizResults_andLoadingIsInProgress() {
        val uiState = QuizResultListUiState.NoQuizResults(
            loading = LoadingState.InProgress,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListRoute(
                    uiState = uiState,
                    navigateToDetailScreen = { _, _ -> },
                    isRefreshing = false,
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Loading in progress").assertIsDisplayed()
    }

    @Test
    fun displaysError_whenUiStateIsNoQuizResults_andLoadingIsError() {
        val uiState = QuizResultListUiState.NoQuizResults(
            loading = LoadingState.Error(FailureReason.UNKNOWN),
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListRoute(
                    uiState = uiState,
                    navigateToDetailScreen = { _, _ -> },
                    isRefreshing = false,
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onNodeWithText(FailureReason.UNKNOWN.value, substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysMessage_whenUiStateIsNoQuizResults_andLoadingIsSuccess() {
        val uiState = QuizResultListUiState.NoQuizResults(
            loading = LoadingState.Success(),
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListRoute(
                    uiState = uiState,
                    navigateToDetailScreen = { _, _ -> },
                    isRefreshing = false,
                    onRefresh = {},
                )
            }
        }

        composeTestRule.onNodeWithText(
            "No Quiz Results",
            ignoreCase = true,
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun displaysBottomNavigation() {
        val uiState = QuizResultListUiState.NoQuizResults(
            loading = LoadingState.InProgress,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultListRoute(
                    uiState = uiState,
                    navigateToDetailScreen = { _, _ -> },
                    isRefreshing = false,
                    onRefresh = {},
                    bottomNavigation = { Text("BottomNavigation") }
                )
            }
        }

        composeTestRule.onNodeWithText("BottomNavigation").assertIsDisplayed()
    }
}