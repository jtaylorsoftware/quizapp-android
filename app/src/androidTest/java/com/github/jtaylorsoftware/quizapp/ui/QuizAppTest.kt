package com.github.jtaylorsoftware.quizapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.jtaylorsoftware.quizapp.MainActivity
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationState
import com.github.jtaylorsoftware.quizapp.testdata.loggedInUserUsername
import com.github.jtaylorsoftware.quizapp.ui.navigation.Screens
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@HiltAndroidTest
class QuizAppTest {
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 0)
    val hiltAndroidRule = HiltAndroidRule(this)

    @Before
    fun beforeEach() {
        hiltAndroidRule.inject()
    }

    @Test
    fun shouldRenderWithoutCrashing() {
    }

    @Test
    fun shouldStartAtProfileRoute() {
        composeTestRule.onNodeWithTag("ProfileRoute").assertIsDisplayed()
    }

    @Test
    fun tapBottomNavQuizzes_shouldNavigateToQuizList() {
        // onChild().onChildren() looks weird, but the BottomNavigation composable
        // passes its Modifier to a Surface, which then renders a Row, which then renders the icons
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Quizzes")).performClick()

        composeTestRule.onNodeWithTag("QuizListRoute").assertIsDisplayed()
    }

    @Test
    fun tapBottomNavResults_shouldNavigateToQuizResultList() {
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Results")).performClick()

        composeTestRule.onNodeWithTag("QuizResultListRoute").assertIsDisplayed()
    }

    @Test
    fun tapBottomNavProfile_shouldNavigateToProfile() {
        // Navigate away from profile
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Results")).performClick()
        // Navigate back to profile
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Profile")).performClick()

        composeTestRule.onNodeWithText("Hello, $loggedInUserUsername").assertIsDisplayed()
    }

    @Test
    fun quizListFab_ShouldNavigateToQuizEditor() {
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Quizzes")).performClick()
        composeTestRule.onNodeWithContentDescription("Create quiz").performClick()

        composeTestRule.onNodeWithTag("QuizEditorScreen").assertIsDisplayed()
    }

    @Test
    fun quizListFab_TapViewResultsIcon_ShouldNavigateToQuizResultList() {
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Quizzes")).performClick()

        composeTestRule.onNodeWithContentDescription("View Results").performClick()

        composeTestRule.onNodeWithTag("QuizResultListRoute").assertIsDisplayed()
    }

    @Test
    fun tapSignOut_shouldNavigateToLogin() {
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Profile")).performClick()

        composeTestRule.onNodeWithTag("ProfileRoute").assertIsDisplayed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Log out").performClick()

        // Confirm dialog
        composeTestRule.onNodeWithText("Confirm").performClick()

        // Should sign out
        composeTestRule.onNodeWithTag("LoginRoute").assertIsDisplayed()
    }

    @Test
    fun tapSettingsIcon_shouldOpenSettingsMenu() {
        composeTestRule.onNodeWithTag("AppBottomNavigationBar").onChild().onChildren()
            .filterToOne(hasText("Profile")).performClick()

        composeTestRule.onNodeWithTag("ProfileRoute").assertIsDisplayed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Profile Settings").assertIsDisplayed()
    }
}