package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.junit.Rule
import org.junit.Test

class QuizResultDetailRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysResultDetail_whenUiStateIsQuizResultDetail() {
        val uiState = QuizResultDetailUiState.QuizResultDetail(
            loading = LoadingState.Success(),
            quizResult = QuizResult(
                quizTitle = "TEST_TITLE",
                username = "TEST_USERNAME",
            ),
            quizForm = QuizForm()
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailRoute(
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("\"TEST_TITLE\"", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("TEST_USERNAME", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysLoadingProgress_whenUiStateIsNoQuizResult_andLoadingIsInProgress() {
        val uiState = QuizResultDetailUiState.NoQuizResult(
            loading = LoadingState.InProgress,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailRoute(
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Loading in progress").assertIsDisplayed()
    }

    @Test
    fun displaysError_whenUiStateIsNoQuizResult_andLoadingIsError() {
        val uiState = QuizResultDetailUiState.NoQuizResult(
            loading = LoadingState.Error(FailureReason.UNKNOWN),
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailRoute(
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText(FailureReason.UNKNOWN.value, substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysMessage_whenUiStateIsNoQuizResult_andLoadingIsSuccess() {
        val uiState = QuizResultDetailUiState.NoQuizResult(
            loading = LoadingState.Success(),
        )
        composeTestRule.setContent {
            QuizAppTheme {
                QuizResultDetailRoute(
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            "No Quiz Result Found",
            ignoreCase = true,
            substring = true
        ).assertIsDisplayed()
    }
}