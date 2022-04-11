package com.github.jtaylorsoftware.quizapp.ui.signup

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class RegisterScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsRegisterForm() {
        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    TextFieldState(),
                    {},
                    TextFieldState(),
                    {},
                    TextFieldState(),
                    {},
                    {},
                    {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Username").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Password").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Confirm password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Up").assertHasClickAction()
        composeTestRule.onNodeWithText("Already registered?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertHasClickAction()
    }

    @Test
    fun canInputUserAndPassAndConfirmPassAndEmail() {
        val changeUser = mockk<(String) -> Unit>()
        val changePass = mockk<(String) -> Unit>()
        val changeEmail = mockk<(String) -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    TextFieldState(),
                    changeUser,
                    TextFieldState(),
                    changePass,
                    TextFieldState(),
                    changeEmail,
                    {},
                    {}
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

        composeTestRule.onNodeWithContentDescription("Username").performTextInput(inputUser)
        composeTestRule.onNodeWithContentDescription("Email").performTextInput(inputEmail)
        composeTestRule.onNodeWithContentDescription("Password").performTextInput(inputPass)
        composeTestRule.onNodeWithContentDescription("Confirm password").performTextInput(inputPass)

        assertEquals(inputUser, user.captured)
        assertEquals(inputPass, pass.captured)
        assertEquals(inputEmail, email.captured)
    }

    @Test
    fun whenSignUpButtonPressed_CallsRegister() {
        val register = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    TextFieldState(),
                    {},
                    TextFieldState(),
                    {},
                    TextFieldState(),
                    {},
                    navigateToLogin = {},
                    register = register
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
        val login = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    TextFieldState(),
                    {},
                    TextFieldState(),
                    {},
                    TextFieldState(),
                    {},
                    navigateToLogin = login,
                    register = {}
                )
            }
        }

        every { login() } returns Unit

        composeTestRule.onNodeWithText("Sign In").performClick()

        verify(exactly = 1) { login() }
        confirmVerified(login)
    }

    @Test
    fun whenStateHasErrors_DisplaysErrorMessages() {
        composeTestRule.setContent {
            QuizAppTheme {
                SignupScreen(
                    usernameState = TextFieldState(error = "Username taken", dirty = true),
                    {},
                    passwordState = TextFieldState(error = "Password error", dirty = true),
                    {},
                    TextFieldState(),
                    {},
                    {},
                    {}
                )
            }
        }

        composeTestRule.onNodeWithText("Username taken").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password error").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Confirm password").performTextInput("mismatch")
        composeTestRule.onNodeWithText("Passwords do not match.").assertIsDisplayed()
    }
}