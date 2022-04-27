package com.github.jtaylorsoftware.quizapp.ui.signinsignup

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class SignInScreenTest {
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
                SignInScreen(uiState, {}, {}, {}, {})
            }
        }

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertHasClickAction()
        composeTestRule.onNodeWithText("Sign Up").assertHasClickAction()
    }

    @Test
    fun canInputUserAndPass() {
        val uiState = SignInUiState(
            loginStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            passwordState = TextFieldState()
        )
        val changeUser = mockk<(String) -> Unit>()
        val changePass = mockk<(String) -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                SignInScreen(uiState, changeUser, changePass, {}, {})
            }
        }

        val user = slot<String>()
        val pass = slot<String>()
        every { changeUser(capture(user)) } returns Unit
        every { changePass(capture(pass)) } returns Unit

        val inputUser = "testuser"
        val inputPass = "password"

        composeTestRule.onNodeWithText("Username").performTextInput(inputUser)
        composeTestRule.onNodeWithText("Password").performTextInput(inputPass)

        assertEquals(inputUser, user.captured)
        assertEquals(inputPass, pass.captured)
    }

    @Test
    fun whenSignInButtonPressed_CallsLogin() {
        val uiState = SignInUiState(
            loginStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            passwordState = TextFieldState()
        )
        val login = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                SignInScreen(uiState, {}, {}, login, {})
            }
        }

        every { login() } returns Unit

        composeTestRule.onNodeWithText("Sign In").performClick()

        verify(exactly = 1) { login() }
        confirmVerified(login)
    }

    @Test
    fun whenSignupTextPressed_CallsRegister() {
        val uiState = SignInUiState(
            loginStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            passwordState = TextFieldState()
        )
        val register = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                SignInScreen(uiState, {}, {}, login = {}, navigateToSignup = register)
            }
        }

        every { register() } returns Unit

        composeTestRule.onNodeWithText("Sign Up").performClick()

        verify(exactly = 1) { register() }
        confirmVerified(register)
    }

    @Test
    fun whenStateHasErrors_StateDisplaysErrorMessages() {
        val uiState = SignInUiState(
            loginStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(error = "Username taken", dirty = true),
            passwordState = TextFieldState(error = "Password too short", dirty = true)
        )
        composeTestRule.setContent {
            QuizAppTheme {
                SignInScreen(uiState, {}, {}, {}, {})
            }
        }

        composeTestRule.onNodeWithText("Username taken").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password too short").assertIsDisplayed()
    }

    @Test
    fun buttonDisplaysProgressIndicator_whenLoadingInProgress() {
        val uiState = SignInUiState(
            loginStatus = LoadingState.InProgress,
            usernameState = TextFieldState(),
            passwordState = TextFieldState()
        )
        composeTestRule.setContent {
            QuizAppTheme {
                SignInScreen(uiState, {}, {}, {}, {})
            }
        }

        composeTestRule.onNodeWithContentDescription("progress", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }
}