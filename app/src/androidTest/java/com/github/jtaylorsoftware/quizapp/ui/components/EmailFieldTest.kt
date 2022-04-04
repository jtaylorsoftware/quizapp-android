package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test


class EmailFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysGivenText() {
        val state = TextFieldState(text = "email@email.com")
        composeTestRule.setContent {
            EmailField(
                state,
                {},
                fieldContentDescription = "Enter your email",
                hint = "Hint",
                hintContentDescription = "Email hint"
            )
        }

        composeTestRule.onNodeWithText(state.text).assertIsDisplayed()
        composeTestRule.onNodeWithText("Hint").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Enter your email").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Email hint").assertIsDisplayed()
    }

    @Test
    fun whenNotDirty_DoesNotDisplayError() {
        val state = TextFieldState(text = "email@email.com", error = "Bad Input", dirty = false)
        composeTestRule.setContent {
            EmailField(state, {}, hint = "Hint")
        }

        composeTestRule.onNodeWithText(state.error!!).assertDoesNotExist()
        composeTestRule.onNodeWithText("Hint").assertIsDisplayed()
    }

    @Test
    fun whenGivenErrorState_DisplaysErrorText() {
        val state = TextFieldState(text = "", error = "Bad input", dirty = true)
        composeTestRule.setContent {
            EmailField(state, {}, hint = "Hint")
        }

        composeTestRule.onNodeWithText(state.error!!).assertIsDisplayed()
        composeTestRule.onNodeWithText("Hint").assertDoesNotExist()
    }

    @Test
    fun whenTextInput_CallsOnChange() {
        val state = TextFieldState(text = "")
        val onChange = mockk<(String) -> Unit>()

        every { onChange(any()) } returns Unit

        composeTestRule.setContent {
            EmailField(state, onChange, hint = "Hint")
        }

        val expectedText = "testemail@email.com"

        composeTestRule.onNodeWithContentDescription("Email").performTextInput(expectedText)

        verify { onChange(expectedText) }
        confirmVerified(onChange)
    }
}