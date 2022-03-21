package com.github.jtaylorsoftware.quizapp.ui.register

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.*
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsRegisterForm() {
        composeTestRule.setContent {
            QuizAppTheme {
                RegisterScreen(
                    TextFieldState(error = null),
                    {},
                    TextFieldState(error = null),
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
    fun canInputUserAndPassAndConfirmPass() {
        val changeUser = mockk<(String) -> Unit>()
        val changePass = mockk<(String) -> Unit>()
        val changeConfirmPass = mockk<(String) -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                RegisterScreen(
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
        composeTestRule.onNodeWithContentDescription("Confirm password").performTextInput(inputPass)

        Assert.assertEquals(inputUser, user.captured)
        Assert.assertEquals(inputPass, pass.captured)
    }

    @Test
    fun whenSignUpButtonPressed_CallsRegister() {
        val register = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                RegisterScreen(
                    TextFieldState(error = null),
                    {},
                    TextFieldState(error = null),
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
    fun whenSignInTextPressed_CallsLogin() {
        val login = mockk<() -> Unit>()

        composeTestRule.setContent {
            QuizAppTheme {
                RegisterScreen(
                    TextFieldState(error = null),
                    {},
                    TextFieldState(error = null),
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
                RegisterScreen(
                    usernameState = TextFieldState(error = "Username taken", dirty = true),
                    {},
                    passwordState = TextFieldState(error = "Password error", dirty = true),
                    {},
                    {},
                    {}
                )
            }
        }

        composeTestRule.onNodeWithText("Username taken").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }
}