package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.WindowSizeClass
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class ProfileSettingsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayContent() {
        val email = "user123@email.com"
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            settingsOpen = true,
            emailState = TextFieldState(text = email),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
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
    fun changeEmail_whenClicked_changesTextToField() {
        val email = "user123@email.com"
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            settingsOpen = true,
            data = User(email = email),
            emailState = TextFieldState(text = email),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
                )
            }
        }

        // Do click
        composeTestRule.onNodeWithText("Change Email").performClick()

        // Should show a field that can take text input
        composeTestRule.onNodeWithText(email).performTextInput("test@example.com")

        // Should have cancel and confirm
        composeTestRule.onNodeWithText("Cancel").assertHasClickAction()
        composeTestRule.onNodeWithText("Submit").assertHasClickAction()
    }

    @Test
    fun submitEmailButton_whenSubmitStatusInProgress_displaysSpinner() {
        val email = "user123@email.com"
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            settingsOpen = true,
            emailState = TextFieldState(text = email),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.InProgress,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
                )
            }
        }

        // Do click
        composeTestRule.onNodeWithText("Change Email").performClick()

        // Cancel should be disabled
        composeTestRule.onNodeWithText("Cancel").assertIsNotEnabled()

        // Submit should be spinner
        composeTestRule.onNodeWithContentDescription("Change Email is in progress").assertIsDisplayed()
    }

    @Test
    fun cancelEmailChanges_whenClicked_changesFieldToText_discardsChanges() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit
        val email = "prevemail@email.com"
        var emailState = TextFieldState(text = email)
        val onChangeEmail: (String) -> Unit = { emailState = TextFieldState(text = it, dirty = true) }

        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            settingsOpen = true,
            emailState = emailState,
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = onChangeEmail,
                    onChangePassword = {},
                    onSubmitEmail = submit,
                    onSubmitPassword = {},
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
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

        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(email = email),
            settingsOpen = true,
            emailState = emailState,
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = onChangeEmail,
                    onChangePassword = {},
                    onSubmitEmail = submit,
                    onSubmitPassword = {},
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
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
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(),
            settingsOpen = true,
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
                )
            }
        }

        composeTestRule.onNodeWithText("Change Password").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password", ignoreCase = true).assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Submit").assertIsDisplayed()
    }

    @Test
    fun submitPasswordButton_whenSubmitStatusInProgress_displaysSpinner() {
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(),
            settingsOpen = true,
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.InProgress
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = {},
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
                )
            }
        }

        composeTestRule.onNodeWithText("Change Password").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password", ignoreCase = true).assertIsDisplayed()

        // Cancel should be disabled
        composeTestRule.onNodeWithText("Cancel").assertIsNotEnabled()

        // Submit should be spinner
        composeTestRule.onNodeWithContentDescription("Change Password is in progress").assertIsDisplayed()
    }

    @Test
    fun cancelPasswordChanges_whenClicked_hidesForm() {
        val submit = mockk<() -> Unit>()
        every { submit() } returns Unit

        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(),
            settingsOpen = true,
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = submit,
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
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

        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = User(),
            settingsOpen = true,
            emailState = TextFieldState(),
            passwordState = TextFieldState(),
            submitEmailStatus = LoadingState.NotStarted,
            submitPasswordStatus = LoadingState.NotStarted
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileSettingsDialog(
                    uiState = uiState,
                    onClose = {},
                    onChangeEmail = {},
                    onChangePassword = {},
                    onSubmitEmail = {},
                    onSubmitPassword = submit,
                    maxWidthDp = 480.dp,
                    windowSizeClass = WindowSizeClass.Compact
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