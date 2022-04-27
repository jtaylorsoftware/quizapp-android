package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
            )
        }

        composeTestRule.onNodeWithText(state.text).assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("email@email.com").assertIsDisplayed()
    }

    @Test
    fun whenNotDirty_DoesNotDisplayError() {
        val state = TextFieldState(text = "email@email.com", error = "Bad Input", dirty = false)
        composeTestRule.setContent {
            EmailField(
                state,
                {},
            )
        }

        composeTestRule.onNodeWithText(state.error!!).assertDoesNotExist()
    }

    @Test
    fun whenDirtyWithError_DisplaysErrorText() {
        val state = TextFieldState(text = "", error = "Bad input", dirty = true)
        composeTestRule.setContent {
            EmailField(
                state,
                {},
            )
        }

        composeTestRule.onNodeWithText(state.error!!).assertIsDisplayed()
    }

    @Test
    fun whenTextInput_CallsOnChange() {
        val state = TextFieldState(text = "")
        val onChange = mockk<(String) -> Unit>()

        every { onChange(any()) } returns Unit

        composeTestRule.setContent {
            EmailField(
                state,
                onChange,
            )
        }

        val expectedText = "testemail@email.com"

        composeTestRule.onNodeWithText("Email").performTextInput(expectedText)

        verify { onChange(expectedText) }
        confirmVerified(onChange)
    }
}