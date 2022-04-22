package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.util.toLocalizedString

/**
 * Displays a [User]'s non-sensitive data, such as username,
 * the number of quizzes, and the number of results.
 *
 * @param navigateToQuizScreen Function called when user taps the "created quizzes" section.
 * Should perform the same action as tapping the bottom navigation icon.
 *
 * @param navigateToResultScreen Function called when user taps the "your results" section.
 * Should perform the same action as tapping the bottom navigation icon.
 *
 * @param navigateToProfileEditor Callback invoked when user presses the "Edit Profile" button.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState.Profile,
    navigateToQuizScreen: () -> Unit,
    navigateToResultScreen: () -> Unit,
    navigateToProfileEditor: () -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ProfileCard(uiState.data, navigateToProfileEditor)
        Spacer(modifier = Modifier.fillMaxWidth())
        ListDescriptionCard(
            list = uiState.data.quizzes,
            action = "You've created",
            singular = "quiz",
            plural = "quizzes",
            onButtonClick = navigateToQuizScreen
        )
        Spacer(modifier = Modifier.fillMaxWidth())
        ListDescriptionCard(
            list = uiState.data.results,
            action = "You have",
            singular = "result",
            plural = "results",
            onButtonClick = navigateToResultScreen
        )
    }
}

/**
 * A card displaying a brief summary of a [User]'s non-sensitive data, and
 * a button that represents a navigation action to a profile editing screen.
 *
 * @param user User to read data from.
 * @param navigateToProfileScreen Callback invoked when "Edit Profile" button is pressed.
 */
@Composable
private fun ProfileCard(user: User, navigateToProfileScreen: () -> Unit) {
    Card {
        Column {
            Text("Hello, ${user.username}")
            Text("Email: ${user.email}")
            Text("Joined: ${user.date.toLocalizedString()}")
            Button(onClick = navigateToProfileScreen, modifier = Modifier.testTag("EditProfile")) {
                Text("Edit Profile")
            }
        }
    }
}

/**
 * A card displaying the description of a list, such as "You've created 1 quiz,"
 * and a button to view details. The button always takes the form "View `$noun`".
 * The form of the noun rendered varies depending on the length of [list].
 *
 * @param list The list to compute noun form from.
 * @param action The action in relation to the noun, such as "You've created."
 * @param singular The singular form of the noun.
 * @param plural The plural form of the noun.
 */
@Composable
private fun ListDescriptionCard(
    list: List<Any>,
    action: String,
    singular: String,
    plural: String,
    onButtonClick: () -> Unit
) {
    val noun: String by remember { derivedStateOf { if (list.size == 1) singular else plural } }
    val capitalizedNoun: String by remember { derivedStateOf { noun.replaceFirstChar { it.uppercase() } } }
    Card {
        Column {
            Text("$action ${list.size} $noun.")
            Button(onClick = onButtonClick) {
                Text("View $capitalizedNoun")
            }
        }
    }
}