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
            data = User(username = "TEST"),
            settingsOpen = false,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                    openSettings = {},
                    closeSettings = {},
                    onLogOut = {}
                )
            }
        }

        composeTestRule.onNodeWithText(uiState.data.username, substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysSettings_whenUiStateSettingsOpen() {
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(email = "TESTEMAIL"),
            settingsOpen = true,
            emailState = TextFieldState(text = "TESTEMAIL"),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                    openSettings = {},
                    closeSettings = {},
                    onLogOut = {}
                )
            }
        }

        composeTestRule.onNodeWithText(uiState.data.email, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Change Email").assertIsDisplayed()
    }

    @Test
    fun displaysLoadingProgress_whenNoProfileAndLoadingInProgress() {
        val uiState = ProfileUiState.NoProfile(
            loading = LoadingState.InProgress,
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileRoute(
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                    openSettings = {},
                    closeSettings = {},
                    onLogOut = {}
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
                    uiState = uiState,
                    isRefreshing = false,
                    onRefresh = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                    openSettings = {},
                    closeSettings = {},
                    onLogOut = {}
                )
            }
        }

        composeTestRule.onNodeWithText(FailureReason.UNKNOWN.value, substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysBottomNavigation_whenUiStateIsProfile() {
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(username = "TEST"),
            settingsOpen = false,
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
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                    openSettings = {},
                    closeSettings = {},
                    onLogOut = {},
                    bottomNavigation = { Text("BottomNavigation") }
                )
            }
        }

        composeTestRule.onNodeWithText("BottomNavigation").assertIsDisplayed()
    }

    @Test
    fun doesNotDisplayBottomNavigation_whenUiStateIsSettings() {
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(),
            settingsOpen = true,
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
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                    openSettings = {},
                    closeSettings = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("AppBottomNavigationBar").assertDoesNotExist()
    }

    @Test
    fun logOutButton_whenClicked_callsLogOut() {
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(),
            settingsOpen = false,
        )
        val logOut = mockk<()->Unit>()
        justRun { logOut() }

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
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                    openSettings = {},
                    closeSettings = {},
                    onLogOut = logOut,
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Log out").performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()


        verify(exactly = 1){
            logOut()
        }
        confirmVerified(logOut)
    }
}