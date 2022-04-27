package com.github.jtaylorsoftware.quizapp.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationState
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationStateSource
import com.github.jtaylorsoftware.quizapp.ui.navigation.AuthRouter
import com.github.jtaylorsoftware.quizapp.ui.navigation.NavActions
import com.github.jtaylorsoftware.quizapp.ui.navigation.NavGraph
import com.github.jtaylorsoftware.quizapp.ui.navigation.rememberNavActions
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import kotlin.math.min

/**
 * The root composable for the app.
 *
 * @param windowSizeClass The [WindowSizeClass] of the activity that can be used to determine
 * how best to layout the content of the app.
 *
 * @param authenticationStateSource Source of [AuthenticationState] for the app.
 *
 * @param onFinish Function that can be called to finish the activity.
 */
@Composable
fun QuizApp(
    windowSizeClass: WindowSizeClass,
    authenticationStateSource: AuthenticationStateSource,
    onFinish: () -> Unit,
    navController: NavHostController = rememberNavController(),
    navActions: NavActions = rememberNavActions(navController),
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    val configuration = LocalConfiguration.current

    // For now since the app just has a single-column list in every configuration, just prevent over-stretching
    // of content by setting a maximum width at the root. Individual screens use this to determine
    // how to set content sizes (such as Card width). Note: this could be applied to the content
    // of a root Scaffold in the future, but it would require viewModel-aware wrappers for
    // TopAppBars, FloatingActionButtons, for each possible screen. It would also require rewriting
    // tests to wrap the test content in a Scaffold where needed.
    val maxWidthDp = min(configuration.screenWidthDp, 600).dp

    QuizAppTheme {
        // Also we're passing down scaffoldState and maxWidthDp, instead of providing
        // one root Scaffold - screens currently are better at knowing when and how to put content
        // into Scaffold slots. This might be changeable later by using the current NavDestination
        // to calculate the correct current TopBar while using nav-scoped ViewModel to provide
        // needed state.
        NavGraph(
            onFinish = onFinish,
            navController = navController,
            navActions = navActions,
            scaffoldState = scaffoldState,
            maxWidthDp = maxWidthDp,
            windowSizeClass = windowSizeClass
        )

        AuthRouter(
            authenticationStateSource,
            onRequireAuthentication = navActions::navigateToLogIn,
            onAuthenticated = navActions::popLoginFlow
        )
    }
}
