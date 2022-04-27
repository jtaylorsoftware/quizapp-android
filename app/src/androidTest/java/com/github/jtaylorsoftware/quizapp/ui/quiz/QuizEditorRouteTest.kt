package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import io.mockk.confirmVerified
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class QuizEditorRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsLoadingIndicator_whenUiStateIsNoQuiz_andLoadingIsInProgress() {
        val uiState = QuizEditorUiState.NoQuiz(
            loading = LoadingState.InProgress
        )
        composeTestRule.setContent {
            QuizEditorRoute(
                uiState = uiState,
                onSubmit = {},
                onUploaded = {},
            )
        }

        // Progress indicator shows
        composeTestRule.onNodeWithContentDescription("Loading", substring = true)
            .assertIsDisplayed()

        // Text shows
        composeTestRule.onNodeWithText("Loading quiz", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun showsErrorScreen_whenUiStateIsNoQuiz_andLoadingIsError() {
        val uiState = QuizEditorUiState.NoQuiz(
            loading = LoadingState.Error(FailureReason.NOT_FOUND)
        )

        composeTestRule.setContent {
            QuizEditorRoute(
                uiState = uiState,
                onSubmit = {},
                onUploaded = {},
            )
        }

        // Error text shows
        composeTestRule.onNodeWithText("Not found", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun showsAddButton_whenUiStateIsCreator() {
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.Success(),
            uploadStatus = LoadingState.NotStarted,
            quizState = TestQuizStateHolder(),
            isEditing = false,
        )

        composeTestRule.setContent {
            QuizEditorRoute(
                uiState = uiState,
                onSubmit = {},
                onUploaded = {},
            )
        }

        composeTestRule.onNodeWithText("Add question").assertIsDisplayed()
    }

    @Test
    fun doesNotShowAddButton_whenUiStateIsEditor() {
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.Success(),
            uploadStatus = LoadingState.NotStarted,
            quizState = TestQuizStateHolder(),
            isEditing = true,
        )

        composeTestRule.setContent {
            QuizEditorRoute(
                uiState = uiState,
                onSubmit = {},
                onUploaded = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Add question").assertDoesNotExist()
    }

    @Test
    fun callsOnUploaded_whenUploadStatusIsSuccess() {
        val uiState = QuizEditorUiState.Editor(
            loading = LoadingState.Success(),
            uploadStatus = LoadingState.Success(),
            quizState = TestQuizStateHolder(),
            isEditing = false,
        )
        val navigate = mockk<() -> Unit>()
        justRun { navigate() }

        composeTestRule.setContent {
            QuizEditorRoute(
                uiState = uiState,
                onSubmit = {},
                onUploaded = navigate,
            )
        }

        composeTestRule.mainClock.advanceTimeBy(2000)
        verify(exactly = 1) {
            navigate()
        }
        confirmVerified(navigate)
    }
}