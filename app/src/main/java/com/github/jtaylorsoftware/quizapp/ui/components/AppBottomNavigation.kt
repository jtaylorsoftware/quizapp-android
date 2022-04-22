package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.ui.navigation.Navigator
import com.github.jtaylorsoftware.quizapp.ui.navigation.NavigatorState
import com.github.jtaylorsoftware.quizapp.ui.navigation.bottomNavDestinations
import com.github.jtaylorsoftware.quizapp.ui.navigation.mutableNavigatorState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * Displays the [BottomNavigation] used for the app's top-level destinations.
 */
@Composable
fun AppBottomNavigation(navigator: Navigator) {
    BottomNavigation(Modifier.testTag("AppBottomNavigationBar")) {
        bottomNavDestinations.forEach { destination ->
            BottomNavigationItem(
                selected = destination.screen.route == navigator.state.currentRoute,
                icon = {
                    when {
                        destination.iconVector != null -> {
                            Icon(destination.iconVector, contentDescription = null)
                        }
                        destination.iconRes != null -> {
                            Icon(painterResource(destination.iconRes), contentDescription = null)
                        }
                    }
                },
                label = { Text(destination.label) },
                onClick = { destination.onNavigate(navigator) }
            )
        }
    }
}

@Preview
@Composable
private fun AppBottomNavigationPreview() {
    QuizAppTheme {
        Scaffold(bottomBar = {
            AppBottomNavigation(navigator = previewNavActions)
        }) {

        }
    }
}

private val previewNavActions = object : Navigator {
    override val state: NavigatorState by mutableNavigatorState()

    override fun navigateToProfile() {
        TODO("Not yet implemented")
    }

    override fun navigateToProfileQuizzes() {
        TODO("Not yet implemented")
    }

    override fun onLoginBackPressed(finish: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun navigateToProfileQuizResults() {
        TODO("Not yet implemented")
    }

    override fun navigateToLogIn() {
        TODO("Not yet implemented")
    }

    override fun navigateToSignUp() {
        TODO("Not yet implemented")
    }

    override fun navigateToUser(userId: ObjectId) {
        TODO("Not yet implemented")
    }

    override fun navigateToUserQuizzes(userId: ObjectId) {
        TODO("Not yet implemented")
    }

    override fun navigateToUserResults(userId: ObjectId) {
        TODO("Not yet implemented")
    }

    override fun navigateToEditor(quizId: ObjectId?) {
        TODO("Not yet implemented")
    }

    override fun navigateToForm(quizId: ObjectId) {
        TODO("Not yet implemented")
    }

    override fun navigateToQuizResults(quizId: ObjectId) {
        TODO("Not yet implemented")
    }

    override fun navigateToQuizResultDetail(quizId: ObjectId, userId: ObjectId) {
        TODO("Not yet implemented")
    }

    override fun navigateUp() {
        TODO("Not yet implemented")
    }
}