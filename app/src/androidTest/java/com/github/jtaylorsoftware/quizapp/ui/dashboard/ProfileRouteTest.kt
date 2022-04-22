package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.material.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.confirmVerified
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class ProfileRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysProfileScreen_whenUiStateIsProfile() {
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(username = "TEST")
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onNodeWithText(uiState.data.username, substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysEditorScreen_whenUiStateIsEditor() {
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = "TESTEMAIL"),
            emailState = TextFieldState(text = "TESTEMAIL"),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onNodeWithText(uiState.data.email, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Change Email").assertIsDisplayed()
    }

    @Test
    fun callsOnRefresh_whenSubmitEmailStatusIsSuccess() {
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = "TESTEMAIL"),
            emailState = TextFieldState(text = "TESTEMAIL"),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.Success(),
            submitPasswordStatus = LoadingState.NotStarted
        )
        val onRefresh = mockk<() -> Unit>()
        justRun { onRefresh() }
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = onRefresh,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                )
            }
        }

        verify(exactly = 1) {
            onRefresh()
        }
        confirmVerified(onRefresh)
    }

    @Test
    fun callsOnRefresh_whenSubmitPasswordStatusIsSuccess() {
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = "TESTEMAIL"),
            emailState = TextFieldState(text = "TESTEMAIL"),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.Success(),
        )
        val onRefresh = mockk<() -> Unit>()
        justRun { onRefresh() }
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = onRefresh,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                )
            }
        }

        verify(exactly = 1) {
            onRefresh()
        }
        confirmVerified(onRefresh)
    }

    @Test
    fun displaysLoadingProgress_whenNoProfileAndLoadingInProgress() {
        val uiState = ProfileUiState.NoProfile(
            loading = LoadingState.InProgress,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Loading in progress").assertIsDisplayed()
    }

    @Test
    fun displaysErrorScreen_whenNoProfileAndLoadingError() {
        val uiState = ProfileUiState.NoProfile(
            loading = LoadingState.Error(FailureReason.UNKNOWN),
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText(FailureReason.UNKNOWN.value, substring = true)
            .assertCountEquals(2)
    }

    @Test
    fun displaysBottomNavigation_whenUiStateIsProfile() {
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(username = "TEST")
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                    bottomNavigation = { Text("BottomNavigation") }
                )
            }
        }

        composeTestRule.onNodeWithText("BottomNavigation").assertIsDisplayed()
    }

    @Test
    fun doesNotDisplayBottomNavigation_whenUiStateIsEditor() {
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizScreen = {},
                    navigateToResultScreen = {},
                    openEditor = {},
                    closeEditor = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("AppBottomNavigationBar").assertDoesNotExist()
    }
}