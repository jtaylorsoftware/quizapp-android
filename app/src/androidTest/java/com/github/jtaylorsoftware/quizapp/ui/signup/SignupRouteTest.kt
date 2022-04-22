package com.github.jtaylorsoftware.quizapp.ui.signup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import org.junit.Rule
import org.junit.Test

class SignupRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsSignupForm_whenUiStateNotSignedUp() {
        val uiState = SignupUiState(
            registerStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            emailState = TextFieldState(),
            passwordState = TextFieldState()
        )
        composeTestRule.setContent {
            QuizAppTheme {
                SignupRoute(
                    uiState = uiState,
                    onUsernameChanged = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    register = {},
                    navigateToLogin = {})
            }
        }

        composeTestRule.onNodeWithText("Sign Up").assertIsDisplayed()
    }
}