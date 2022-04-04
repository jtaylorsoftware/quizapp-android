package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.util.toLocalizedString
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test


class DashboardScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val user = User(
        username = "user123",
        email = "user123@email.com",
        quizzes = listOf(ObjectId("quiz1"), ObjectId("quiz2")),
        results = listOf(ObjectId("result1"), ObjectId("result2"))
    )
    private val numQuizzes = user.quizzes.size
    private val numResults = user.results.size

    @Test
    fun shouldDisplayContent() {
        composeTestRule.setContent {
            QuizAppTheme { DashboardScreen(user, {}, {}, {}) }
        }

        // Topmost ("Profile") card content
        composeTestRule.onNodeWithText("Hello, ${user.username}").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email: ${user.email}").assertIsDisplayed()
        composeTestRule.onNodeWithText("Joined: ${user.date.toLocalizedString()}").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Profile").assertHasClickAction()

        // Card showing number of quizzes created with button to view list
        composeTestRule.onNodeWithText("You've created $numQuizzes quizzes.").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Quizzes").assertHasClickAction()

        // Card showing number of results with button to view list
        composeTestRule.onNodeWithText("You have $numResults results.").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Results").assertHasClickAction()
    }

    @Test
    fun editProfile_whenClicked_shouldDisplayProfileScreen() {
        val navigateToProfileScreen = mockk<() -> Unit>()
        every { navigateToProfileScreen() } returns Unit

        composeTestRule.setContent {
            QuizAppTheme {
                DashboardScreen(user, {}, {}, navigateToProfileScreen)
            }
        }

        composeTestRule.onNodeWithText("Edit Profile").performClick()

        verify(exactly = 1) { navigateToProfileScreen() }
        confirmVerified(navigateToProfileScreen)
    }

    @Test
    fun viewQuizzes_whenClicked_shouldDisplayQuizScreen() {
        val navigateToQuizScreen = mockk<() -> Unit>()
        every { navigateToQuizScreen() } returns Unit

        composeTestRule.setContent {
            QuizAppTheme {
                DashboardScreen(user, navigateToQuizScreen, {}, {})
            }
        }

        composeTestRule.onNodeWithText("View Quizzes").performClick()

        verify(exactly = 1) { navigateToQuizScreen() }
        confirmVerified(navigateToQuizScreen)
    }

    @Test
    fun viewResults_whenClicked_shouldDisplayResultScreen() {
        val navigateToResultScreen = mockk<() -> Unit>()
        every { navigateToResultScreen() } returns Unit

        composeTestRule.setContent {
            QuizAppTheme {
                DashboardScreen(user, {}, navigateToResultScreen, {})
            }
        }

        composeTestRule.onNodeWithText("View Results").performClick()

        verify(exactly = 1) { navigateToResultScreen() }
        confirmVerified(navigateToResultScreen)
    }
}