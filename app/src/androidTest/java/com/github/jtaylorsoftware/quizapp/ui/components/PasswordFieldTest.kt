package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysGivenText() {
        composeTestRule.setContent {
            PasswordField(TextFieldState(text = "mypassword"), {}, "Hint")
        }

        composeTestRule.onNodeWithContentDescription("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hint").assertIsDisplayed()
    }

    @Test
    fun whenNotDirtyWithErrors_doesNotShowError() {
        composeTestRule.setContent {
            PasswordField(
                TextFieldState(text = "mypassword", error = "Password too short", dirty = false),
                {},
                "Hint"
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
                "Hint"
            )
        }

        composeTestRule.onNodeWithText("Hint").assertDoesNotExist()
        composeTestRule.onNodeWithText("Password too short").assertIsDisplayed()
    }

    @Test
    fun iconTogglesPasswordVisibility() {
        val password = "mypassword"

        composeTestRule.setContent {
            PasswordField(TextFieldState(text = password), {}, "Hint")
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
            PasswordField(TextFieldState(text = ""), onChange, "Hint")
        }

        val inputPassword = "myinputpassword"
        composeTestRule.onNodeWithContentDescription("Password").performTextInput(inputPassword)

        verify { onChange(any()) }
        confirmVerified(onChange)

        assertEquals(inputPassword, password.captured)
    }
}