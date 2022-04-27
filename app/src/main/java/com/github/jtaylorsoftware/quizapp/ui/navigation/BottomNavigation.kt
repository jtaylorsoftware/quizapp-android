package com.github.jtaylorsoftware.quizapp.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.ui.theme.bottomAppBar
import com.github.jtaylorsoftware.quizapp.ui.theme.onBottomAppBar

/**
 * Wraps a [Screens] that has a bottom navigation action with its icon and label.
 */
sealed class BottomNavDestination(
    val screen: Screen,

    /**
     * Called to perform navigation to this item.
     */
    val onNavigate: (NavActions) -> Unit,

    /**
     * The resId of the icon to display for bottom navigation.
     *
     * [iconVector] is given priority over [iconRes] if both are set.
     */
    @DrawableRes val iconRes: Int? = null,

    /**
     * The [ImageVector] to display for bottom navigation.
     */
    val iconVector: ImageVector? = null,

    /**
     * The label to display for bottom navigation.
     */
    // TODO
    // @StringRes val labelRes: Int? = null,
    val label: String
) {
    object Profile :
        BottomNavDestination(
            Screens.Users.Profile,
            onNavigate = { it.navigateToProfile() },
            iconVector = Icons.Default.AccountCircle,
            label = "Profile"
        )

    object Quizzes :
        BottomNavDestination(
            Screens.Users.Quizzes.List,
            onNavigate = { it.navigateToProfileQuizzes() },
            iconRes = R.drawable.ic_format_list_numbered_24,
            label = "Quizzes"
        )

    object QuizResults :
        BottomNavDestination(
            Screens.Users.QuizResults,
            onNavigate = { it.navigateToProfileQuizResults() },
            iconRes = R.drawable.ic_assessment_24,
            label = "Results"
        )
}

/**
 * Map of [Screens] to [BottomNavDestination] that should have an icon in bottom navigation.
 */
val bottomNavDestinations = mapOf(
    BottomNavDestination.Quizzes.screen to BottomNavDestination.Quizzes,
    BottomNavDestination.Profile.screen to BottomNavDestination.Profile,
    BottomNavDestination.QuizResults.screen to BottomNavDestination.QuizResults
)

/**
 * Displays the [BottomNavigation] used for the app's top-level destinations, using a [NavActions]
 * to perform navigation.
 */
@Composable
fun BottomNavigation(navController: NavController, navActions: NavActions) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen by rememberUpdatedState(screens[backStackEntry?.destination?.route])
    val displayBottomNavigation by derivedStateOf {
        currentScreen in bottomNavDestinations
    }

    if (displayBottomNavigation) {
        BottomNavigation(
            Modifier.testTag("AppBottomNavigationBar"),
            backgroundColor = MaterialTheme.colors.bottomAppBar,
            contentColor = MaterialTheme.colors.onBottomAppBar,
        ) {
            bottomNavDestinations.forEach { (_, destination) ->
                BottomNavigationItem(
                    selected = currentScreen == destination.screen,
                    icon = {
                        when {
                            destination.iconVector != null -> {
                                Icon(destination.iconVector, contentDescription = null)
                            }
                            destination.iconRes != null -> {
                                Icon(
                                    painterResource(destination.iconRes),
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    label = { Text(destination.label) },
                    onClick = {
                        destination.onNavigate(navActions)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun AppBottomNavigationPreview() {
    QuizAppTheme {
        val navController = rememberNavController()
        val navActions = rememberNavActions(navController)
        Scaffold(bottomBar = {
            BottomNavigation(navController, navActions)
        }) {

        }
    }
}