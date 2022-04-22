package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.*
import org.junit.Rule
import org.junit.Test

class ProfileEditorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayContent() {
        val email = "user123@email.com"
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            emailState = TextFieldState(text = email),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = {},
                )
            }
        }

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
    fun logOutButton_whenClicked_callsLogOut() {
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        val logOut = mockk<()->Unit>()
        justRun { logOut() }

        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = logOut,
                )
            }
        }

        composeTestRule.onNodeWithText("Log out").performClick()
        verify(exactly = 1){
            logOut()
        }
        confirmVerified(logOut)
    }

    @Test
    fun changeEmail_whenClicked_changesTextToField() {
        val email = "user123@email.com"
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            emailState = TextFieldState(text = email),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = {},
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
    fun submitEmailButton_whenSubmitStatusInProgress_displaysSpinner() {
        val email = "user123@email.com"
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            emailState = TextFieldState(text = email),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.InProgress,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = {},
                )
            }
        }

        // Do click
        composeTestRule.onNodeWithText("Change Email").performClick()

        // Should now be a field
        composeTestRule.onNodeWithText("Email: $email").assertDoesNotExist()
        composeTestRule.onNodeWithText(email).assertIsDisplayed()

        // Cancel should be disabled
        composeTestRule.onNodeWithText("Cancel").assertIsNotEnabled()

        // Submit should be spinner
        composeTestRule.onNodeWithContentDescription("Change email in progress").assertIsDisplayed()
    }

    @Test
    fun cancelEmailChanges_whenClicked_changesFieldToText_discardsChanges() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit
        val email = "prevemail@email.com"
        var emailState = TextFieldState(text = email)
        val onChangeEmail: (String) -> Unit = { emailState = TextFieldState(text = it, dirty = true) }

        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            emailState = emailState,
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = onChangeEmail,
                    onChangePassword = {},
                    onSubmitEmail = submit,
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = {},
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
        val onChangeEmail: (String) -> Unit = { emailState = TextFieldState(text = it, dirty = true) }

        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            emailState = emailState,
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = onChangeEmail,
                    onChangePassword = {},
                    onSubmitEmail = submit,
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = {},
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
    }

    @Test
    fun changePassword_whenClicked_showsForm() {
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Change Password").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Password").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Confirm Password", ignoreCase = true).assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Submit").assertIsDisplayed()
    }

    @Test
    fun submitPasswordButton_whenSubmitStatusInProgress_displaysSpinner() {
        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.InProgress
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    onClose = {},
                    onLogOut = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Change Password").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Password").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Confirm Password", ignoreCase = true).assertIsDisplayed()

        // Cancel should be disabled
        composeTestRule.onNodeWithText("Cancel").assertIsNotEnabled()

        // Submit should be spinner
        composeTestRule.onNodeWithContentDescription("Change password in progress").assertIsDisplayed()
    }

    @Test
    fun cancelPasswordChanges_whenClicked_hidesForm() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit

        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = submit,
                    onClose = {},
                    onLogOut = {},
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
        composeTestRule.onNodeWithContentDescription("Password").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Confirm Password", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun submitPasswordChanges_whenClicked_shouldCallSubmit() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit

        val uiState = ProfileUiState.Editor(
            loading = LoadingState.NotStarted,
            data = User(),
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileEditorScreen(
                    uiState,
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = submit,
                    onClose = {},
                    onLogOut = {},
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
    }
}