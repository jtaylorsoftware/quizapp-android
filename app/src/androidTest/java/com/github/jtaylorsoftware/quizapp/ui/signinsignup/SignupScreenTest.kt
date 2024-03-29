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


class SignupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsRegisterForm() {
        val uiState = SignupUiState(
            registerStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
        )

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    uiState = uiState,
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onEmailChanged = {},
                    register = {},
                    navigateToLogin = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Up").assertHasClickAction()
        composeTestRule.onNodeWithText("Sign In").assertHasClickAction()
    }

    @Test
    fun canInputUserAndPassAndConfirmPassAndEmail() {
        val changeUser = mockk<(String) -> Unit>()
        val changePass = mockk<(String) -> Unit>()
        val changeEmail = mockk<(String) -> Unit>()

        val uiState = SignupUiState(
            registerStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
        )

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    uiState = uiState,
                    onUsernameChanged = changeUser,
                    onPasswordChanged = changePass,
                    onEmailChanged = changeEmail,
                    register = {},
                    navigateToLogin = {}
                )
            }
        }

        val user = slot<String>()
        val pass = slot<String>()
        val email = slot<String>()
        every { changeUser(capture(user)) } returns Unit
        every { changePass(capture(pass)) } returns Unit
        every { changeEmail(capture(email)) } returns Unit

        val inputUser = "testuser"
        val inputPass = "password"
        val inputEmail = "email@example.com"

        composeTestRule.onNodeWithText("Username").performTextInput(inputUser)
        composeTestRule.onNodeWithText("Email").performTextInput(inputEmail)
        composeTestRule.onNodeWithText("Password").performTextInput(inputPass)
        composeTestRule.onNodeWithText("Confirm password").performTextInput(inputPass)

        assertEquals(inputUser, user.captured)
        assertEquals(inputPass, pass.captured)
        assertEquals(inputEmail, email.captured)
    }

    @Test
    fun whenSignUpButtonPressed_CallsRegister() {
        val register = mockk<() -> Unit>()

        val uiState = SignupUiState(
            registerStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
        )

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    uiState = uiState,
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onEmailChanged = {},
                    register = register,
                    navigateToLogin = {}
                )
            }
        }

        every { register() } returns Unit

        composeTestRule.onNodeWithText("Sign Up").performClick()

        verify(exactly = 1) { register() }
        confirmVerified(register)
    }

    @Test
    fun whenSignInTextPressed_CallsNavigateToLogin() {
        val navigate = mockk<() -> Unit>()

        val uiState = SignupUiState(
            registerStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
        )

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    uiState = uiState,
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onEmailChanged = {},
                    register = {},
                    navigateToLogin = navigate
                )
            }
        }

        every { navigate() } returns Unit

        composeTestRule.onNodeWithText("Sign In").performClick()

        verify(exactly = 1) { navigate() }
        confirmVerified(navigate)
    }

    @Test
    fun whenStateHasErrors_DisplaysErrorMessages() {
        val uiState = SignupUiState(
            registerStatus = LoadingState.NotStarted,
            usernameState = TextFieldState(error = "Username taken", dirty = true),
            emailState = TextFieldState(error = "Email taken", dirty = true),
            passwordState = TextFieldState(error = "Password error", dirty = true),
        )

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    uiState = uiState,
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onEmailChanged = {},
                    register = {},
                    navigateToLogin = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Username taken").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm password").performTextInput("mismatch")
        composeTestRule.onNodeWithText("Passwords do not match.").assertIsDisplayed()
    }

    @Test
    fun buttonDisplaysProgressIndicator_whenLoadingInProgress() {
        val uiState = SignupUiState(
            registerStatus = LoadingState.InProgress,
            usernameState = TextFieldState(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
        )

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    uiState = uiState,
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onEmailChanged = {},
                    register = {},
                    navigateToLogin = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("progress", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Up").assertIsDisplayed()
    }
}