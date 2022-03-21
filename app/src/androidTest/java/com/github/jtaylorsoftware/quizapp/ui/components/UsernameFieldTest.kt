package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsernameFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysGivenText() {
        val state = TextFieldState(text = "Hello World")
        composeTestRule.setContent {
            UsernameField(state, {}, "Hint")
        }

        composeTestRule.onNodeWithText(state.text).assertIsDisplayed()
        composeTestRule.onNodeWithText("Hint").assertIsDisplayed()
    }

    @Test
    fun whenNotDirty_DoesNotDisplayError() {
        val state = TextFieldState(text = "Hello World", error = "Bad Input", dirty = false)
        composeTestRule.setContent {
            UsernameField(state, {}, "Hint")
        }

        composeTestRule.onNodeWithText(state.error!!).assertDoesNotExist()
        composeTestRule.onNodeWithText("Hint").assertIsDisplayed()
    }

    @Test
    fun whenGivenErrorState_DisplaysErrorText() {
        val state = TextFieldState(text = "", error = "Bad input", dirty = true)
        composeTestRule.setContent {
            UsernameField(state, {}, "Hint")
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
            UsernameField(state, onChange, "Hint")
        }

        val expectedText = "mylogin"

        composeTestRule.onNodeWithContentDescription("Username").performTextInput(expectedText)

        verify { onChange(expectedText) }
        confirmVerified(onChange)
    }
}