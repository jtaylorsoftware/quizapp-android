package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsLoginForm() {
        composeTestRule.setContent {
            QuizAppTheme {
                LoginScreen(TextFieldState(), {}, TextFieldState(), {}, {}, {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Username").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertHasClickAction()
        composeTestRule.onNodeWithText("Not registered?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Up").assertHasClickAction()
    }

    @Test
    fun canInputUserAndPass() {
        val changeUser = mockk<(String) -> Unit>()
        val changePass = mockk<(String) -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                LoginScreen(
                    TextFieldState(error = null),
                    changeUser,
                    TextFieldState(error = null),
                    changePass,
                    {},
                    {}
                )
            }
        }

        val user = slot<String>()
        val pass = slot<String>()
        every { changeUser(capture(user)) } returns Unit
        every { changePass(capture(pass)) } returns Unit

        val inputUser = "testuser"
        val inputPass = "password"

        composeTestRule.onNodeWithContentDescription("Username").performTextInput(inputUser)
        composeTestRule.onNodeWithContentDescription("Password").performTextInput(inputPass)

        assertEquals(inputUser, user.captured)
        assertEquals(inputPass, pass.captured)
    }

    @Test
    fun whenSignInButtonPressed_CallsLogin() {
        val login = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                LoginScreen(
                    TextFieldState(error = null),
                    {},
                    TextFieldState(error = null),
                    {},
                    login,
                    {}
                )
            }
        }

        every { login() } returns Unit

        composeTestRule.onNodeWithText("Sign In").performClick()

        verify(exactly = 1) { login() }
        confirmVerified(login)
    }

    @Test
    fun whenSignupTextPressed_CallsRegister() {
        val register = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                LoginScreen(
                    TextFieldState(error = null),
                    {},
                    TextFieldState(error = null),
                    {},
                    login = {},
                    navigateToRegister = register
                )
            }
        }

        every { register() } returns Unit

        composeTestRule.onNodeWithText("Sign Up").performClick()

        verify(exactly = 1) { register() }
        confirmVerified(register)
    }

    @Test
    fun whenStateHasErrors_DisplaysErrorMessages() {
        composeTestRule.setContent {
            QuizAppTheme {
                LoginScreen(
                    usernameState = TextFieldState(error = "Username taken", dirty = true),
                    {},
                    passwordState = TextFieldState(error = "Password too short", dirty = true),
                    {},
                    {},
                    {}
                )
            }
        }

        composeTestRule.onNodeWithText("Username taken").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password too short").assertIsDisplayed()
    }
}