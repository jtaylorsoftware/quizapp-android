package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.jtaylorsoftware.quizapp.data.User
import com.github.jtaylorsoftware.quizapp.util.toLocalizedString

/**
 * DashboardScreen displays a [User]'s non-sensitive data, such as username,
 * the number of quizzes, and the number of results.
 *
 * @param user User to read data from.
 * @param navigateToProfileScreen Callback invoked when user presses an "Edit Profile" button.
 */
@Composable
fun DashboardScreen(
    user: User,
    navigateToQuizScreen: () ->  Unit,
    navigateToResultScreen: () -> Unit,
    navigateToProfileScreen: () -> Unit
) {
    Column {
        ProfileCard(user, navigateToProfileScreen)
        Spacer(modifier = Modifier.fillMaxWidth())
        ListDescriptionCard(
            list = user.quizzes,
            action = "You've created",
            singular = "quiz",
            plural = "quizzes",
            onButtonClick = navigateToQuizScreen
        )
        Spacer(modifier = Modifier.fillMaxWidth())
        ListDescriptionCard(
            list = user.results,
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
            Button(onClick = navigateToProfileScreen) {
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
private fun ListDescriptionCard(list: List<Any>, action: String, singular: String, plural: String, onButtonClick: () -> Unit) {
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