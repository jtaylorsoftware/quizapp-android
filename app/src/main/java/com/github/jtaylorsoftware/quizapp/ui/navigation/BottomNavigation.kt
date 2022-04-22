package com.github.jtaylorsoftware.quizapp.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.jtaylorsoftware.quizapp.R

/**
 * Wraps a [Screens] that has a bottom navigation action with its icon and label.
 */
sealed class BottomNavDestination(
    val screen: Screen,

    /**
     * Called to perform navigation to this item.
     */
    val onNavigate: (Navigator) -> Unit,

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
//        @StringRes val labelRes: Int? = null,
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
            Screens.Users.Quizzes,
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
 * Set of [Screens] that should have an icon in bottom navigation.
 */
val bottomNavDestinations = setOf(
    BottomNavDestination.Quizzes,
    BottomNavDestination.Profile,
    BottomNavDestination.QuizResults
)

/**
 * Set of routes of the [Screens] that should have an icon in bottom navigation.
 */
val bottomNavRoutes = bottomNavDestinations.map { it.screen.route }.toSet()