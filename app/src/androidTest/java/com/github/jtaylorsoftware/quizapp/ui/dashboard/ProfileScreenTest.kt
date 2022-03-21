package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayContent() {
        val email = "user123@email.com"
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    email,
                    TextFieldState(text = email),
                    {},
                    TextFieldState(),
                    {},
                    {}, {}
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Profile").assertIsDisplayed()

        // Current email with button to toggle changing email
        composeTestRule.onNodeWithText("Email: $email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Change Email").assertHasClickAction()

        // Button to show password inputs
        composeTestRule.onNodeWithText("Change Password").assertHasClickAction()

        // Notice that account deletion not available in app
        composeTestRule.onNodeWithText("Account deletion available when signed into the web app.")
            .assertIsDisplayed()
    }

    @Test
    fun changeEmail_whenClicked_changesTextToField() {
        val email = "user123@email.com"
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    email,
                    TextFieldState(text = email),
                    {},
                    TextFieldState(),
                    {},
                    {}, {}
                )
            }
        }

        // Do click
        composeTestRule.onNodeWithText("Change Email").performClick()

        // Should now be a field
        composeTestRule.onNodeWithText("Email: $email").assertDoesNotExist()
        composeTestRule.onNodeWithText(email).assertIsDisplayed()

        // Should have cancel and confirm
        composeTestRule.onNodeWithText("Cancel").assertHasClickAction()
        composeTestRule.onNodeWithText("Submit").assertHasClickAction()
    }

    @Test
    fun cancelEmailChanges_whenClicked_changesFieldToText_discardsChanges() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit
        val email = "prevemail@email.com"
        var emailState = TextFieldState(text = email)
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    email,
                    emailState,
                    onChangeEmail = { emailState = TextFieldState(text = it, dirty = true) },
                    TextFieldState(),
                    {},
                    onSubmitEmail = submit,
                    {}
                )
            }
        }

        // Do click
        composeTestRule.onNodeWithText("Change Email").performClick()
        composeTestRule.waitForIdle()

        // Input a new email
        val newEmail = "newemail@email.com"
        composeTestRule.onNodeWithText(email).performTextInput(newEmail)

        // Cancel changes
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Ensure submit not called with undesired changes
        verify(exactly = 0) { submit() }
        confirmVerified(submit)

        // Should now be plain text with original email display
        // TODO - hoist open/close state
        composeTestRule.onNodeWithText("Email: $email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email: $newEmail").assertDoesNotExist()
        composeTestRule.onNodeWithText(newEmail).assertDoesNotExist()
    }

    @Test
    fun submitEmailChanges_whenClicked_callsSubmit() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit
        val email = "prevemail@email.com"
        var emailState = TextFieldState(text = email)

        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    email,
                    emailState,
                    onChangeEmail = { emailState = TextFieldState(text = it, dirty = true) },
                    TextFieldState(),
                    {},
                    onSubmitEmail = submit,
                    {}
                )
            }
        }

        // Do click
        composeTestRule.onNodeWithText("Change Email").performClick()

        // Input a new email
        val newEmail = "newemail@email.com"
        composeTestRule.onNodeWithText(email).performTextInput(newEmail)

        // Click and verify call
        composeTestRule.onNodeWithText("Submit").performClick()

        verify(exactly = 1) { submit() }
        confirmVerified(submit)

        // TODO - hoist open/close state
//        composeTestRule.onNodeWithText("Email: $newEmail").assertIsDisplayed()
    }

    @Test
    fun changePassword_whenClicked_showsForm() {
        val email = "prevemail@email.com"
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    email,
                    TextFieldState(),
                    {},
                    TextFieldState(),
                    {},
                    {}, {}
                )
            }
        }

        composeTestRule.onNodeWithText("Change Password").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Password").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Confirm Password", ignoreCase = true).assertIsDisplayed()

        // TODO - hoist open/close state
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Submit").assertIsDisplayed()
    }

    @Test
    fun cancelPasswordChanges_whenClicked_hidesForm() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit
        var passwordState = TextFieldState()
        val email = "prevemail@email.com"
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    email,
                    TextFieldState(),
                    {},
                    passwordState,
                    onChangePassword = { passwordState = TextFieldState(text = it, dirty = true) },
                    {},
                    onSubmitPassword = submit
                )
            }
        }

        // Show form
        composeTestRule.onNodeWithText("Change Password").performClick()

        // Click Cancel
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Ensure not submitted discarded changes
        verify(exactly = 0) { submit() }
        confirmVerified(submit)

        // Form should be hidden
        // TODO - hoist open/close state
        composeTestRule.onNodeWithContentDescription("Password").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Confirm Password", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun submitPasswordChanges_whenClicked_hidesFormAndCallsSubmit() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit
        var passwordState = TextFieldState()
        val email = "prevemail@email.com"
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    email,
                    TextFieldState(text = email),
                    {},
                    passwordState,
                    onChangePassword = { passwordState = TextFieldState(text = it) },
                    {},
                    onSubmitPassword = submit
                )
            }
        }

        // Show form
        composeTestRule.onNodeWithText("Change Password").performClick()
        composeTestRule.waitForIdle()

        // Submit
        composeTestRule.onNodeWithText("Submit").performClick()
        verify(exactly = 1) { submit() }
        confirmVerified(submit)

        // Form should be hidden
        // TODO - hoist open/close state
//        composeTestRule.onNodeWithContentDescription("Password").assertDoesNotExist()
//        composeTestRule.onNodeWithContentDescription("Confirm Password", ignoreCase = true).assertDoesNotExist()
    }
}