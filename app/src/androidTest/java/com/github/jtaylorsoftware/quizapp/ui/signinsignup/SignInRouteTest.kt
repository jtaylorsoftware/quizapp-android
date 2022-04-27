package com.github.jtaylorsoftware.quizapp.ui.signinsignup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.junit.Rule
import org.junit.Test

class SignInRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsLoginForm() {
        val uiState = SignInUiState(
            loginStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            passwordState = TextFieldState()
        )
        composeTestRule.setContent {
            QuizAppTheme {
                SignInRoute(
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