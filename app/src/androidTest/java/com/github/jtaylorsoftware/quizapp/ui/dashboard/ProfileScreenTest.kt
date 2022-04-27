package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.util.toLocalizedString
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test


class ProfileScreenTest {
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
        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = user,
            settingsOpen = false
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    uiState = uiState,
                    navigateToQuizCreator = {},
                    navigateToQuizResults = {},
                )
            }
        }

        // User data card content
        composeTestRule.onNodeWithText("Hello, ${user.username}").assertIsDisplayed()
        composeTestRule.onNodeWithText("Joined: ${user.date.toLocalizedString()}").assertIsDisplayed()

        // Card showing number of quizzes created with button to create quiz
        composeTestRule.onNodeWithText("You've created $numQuizzes quizzes.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create quiz", ignoreCase = true).assertHasClickAction()

        // Card showing number of results with button to view list
        composeTestRule.onNodeWithText("You've taken $numResults quizzes.").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Results", ignoreCase = true).assertHasClickAction()
    }

    @Test
    fun createQuiz_whenClicked_shouldCallNavigateToQuizCreator() {
        val navigateToQuizCreator = mockk<() -> Unit>()
        every { navigateToQuizCreator() } returns Unit

        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = user,
            settingsOpen = false
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    uiState = uiState,
                    navigateToQuizCreator = navigateToQuizCreator,
                    navigateToQuizResults = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Create quiz", ignoreCase = true).performClick()

        verify(exactly = 1) { navigateToQuizCreator() }
        confirmVerified(navigateToQuizCreator)
    }

    @Test
    fun viewResults_whenClicked_shouldDisplayResultScreen() {
        val navigateToQuizResults = mockk<() -> Unit>()
        every { navigateToQuizResults() } returns Unit

        val uiState = ProfileUiState.Profile(
            loading = LoadingState.NotStarted,
            data = user,
            settingsOpen = false
        )
        composeTestRule.setContent {
            QuizAppTheme {
                ProfileScreen(
                    uiState = uiState,
                    navigateToQuizCreator = {},
                    navigateToQuizResults = navigateToQuizResults,
                )
            }
        }

        composeTestRule.onNodeWithText("View Results", ignoreCase = true).performClick()

        verify(exactly = 1) { navigateToQuizResults() }
        confirmVerified(navigateToQuizResults)
    }
}