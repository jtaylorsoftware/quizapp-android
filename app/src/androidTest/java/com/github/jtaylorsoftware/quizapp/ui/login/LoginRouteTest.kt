package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.junit.Rule
import org.junit.Test

class LoginRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsLoginForm() {
        val uiState = LoginUiState(
            loginStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            passwordState = TextFieldState()
        )
        composeTestRule.setContent {
            QuizAppTheme {
                LoginRoute(
                    uiState = uiState,
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    login = {},
                    navigateToSignUp = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }
}