package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


class PasswordFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysGivenText() {
        composeTestRule.setContent {
            PasswordField(
                TextFieldState(text = "mypassword"),
                {},
                fieldContentDescription = "Password",
                hint = "Hint",
                hintContentDescription = "Password hint"
            )
        }

        composeTestRule.onNodeWithContentDescription("Password").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Password hint").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hint").assertIsDisplayed()
    }

    @Test
    fun whenNotDirtyWithErrors_doesNotShowError() {
        composeTestRule.setContent {
            PasswordField(
                TextFieldState(text = "mypassword", error = "Password too short", dirty = false),
                {},
                hint = "Hint"
            )
        }

        composeTestRule.onNodeWithText("Hint").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password too short").assertDoesNotExist()
    }

    @Test
    fun whenDirtyWithErrors_showsError() {
        composeTestRule.setContent {
            PasswordField(
                TextFieldState(text = "mypassword", error = "Password too short", dirty = true),
                {},
                hint = "Hint"
            )
        }

        composeTestRule.onNodeWithText("Hint").assertDoesNotExist()
        composeTestRule.onNodeWithText("Password too short").assertIsDisplayed()
    }

    @Test
    fun iconTogglesPasswordVisibility() {
        val password = "mypassword"

        composeTestRule.setContent {
            PasswordField(TextFieldState(text = password), {}, hint = "Hint")
        }

        // By default the password is not displayed
        val mask = '\u2022'
        val maskedPassword = mask.toString().repeat(password.length)
        composeTestRule.onNodeWithText(maskedPassword).assertIsDisplayed()

        // Click icon to toggle it to shown
        composeTestRule.onNodeWithContentDescription("Show password").performClick()

        // After clicking, it is shown
        composeTestRule.onNodeWithText(password).assertIsDisplayed()

        // Click icon to hide it again
        composeTestRule.onNodeWithContentDescription("Hide password").performClick()
        composeTestRule.onNodeWithText(maskedPassword).assertIsDisplayed()
    }

    @Test
    fun whenTextInput_callsOnChange() {
        val onChange = mockk<(String) -> Unit>()
        val password = slot<String>()
        every { onChange(capture(password)) } returns Unit

        composeTestRule.setContent {
            PasswordField(TextFieldState(text = ""), onChange, hint = "Hint")
        }

        val inputPassword = "myinputpassword"
        composeTestRule.onNodeWithContentDescription("Password").performTextInput(inputPassword)

        verify { onChange(any()) }
        confirmVerified(onChange)

        assertEquals(inputPassword, password.captured)
    }
}