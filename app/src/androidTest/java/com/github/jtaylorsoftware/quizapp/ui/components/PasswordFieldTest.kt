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
    fun whenNotDirtyWithErrors_doesNotShowError() {
        val state = TextFieldState(text = "mypassword", error = "Password too short", dirty = false)
        composeTestRule.setContent {
            PasswordField(
                state,
                {}
            )
        }

        composeTestRule.onNodeWithText(state.error!!).assertDoesNotExist()
    }

    @Test
    fun whenDirtyWithError_showsError() {
        val state = TextFieldState(text = "mypassword", error = "Password too short", dirty = true)
        composeTestRule.setContent {
            PasswordField(
                state,
                {}
            )
        }

        composeTestRule.onNodeWithText(state.error!!).assertIsDisplayed()
    }

    @Test
    fun iconTogglesPasswordVisibility() {
        val password = "mypassword"

        composeTestRule.setContent {
            PasswordField(TextFieldState(text = password), {})
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
            PasswordField(TextFieldState(text = ""), onChange)
        }

        val inputPassword = "myinputpassword"
        composeTestRule.onNodeWithText("Password").performTextInput(inputPassword)

        verify { onChange(any()) }
        confirmVerified(onChange)

        assertEquals(inputPassword, password.captured)
    }
}