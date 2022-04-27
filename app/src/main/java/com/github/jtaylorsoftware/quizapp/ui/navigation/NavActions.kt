package com.github.jtaylorsoftware.quizapp.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationState
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationStateSource
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Models navigation actions for the app, making it easier to navigate to
 * the correct route paths while passing their expected arguments.
 *
 * This only provides methods where there are expectations about the current
 * navigation state which must be met before navigation, or when navigation
 * must be performed in a specific way every time.
 */
interface NavActions {
    /**
     * Navigates to the login screen.
     */
    fun navigateToLogIn()

    /**
     * Navigates to the sign-up screen.
     */
    fun navigateToSignUp()

    /**
     * Pops the auth flow off the backstack.
     */
    fun popLoginFlow()

    /**
     * Navigates to the logged-in user's profile.
     */
    fun navigateToProfile()

    /**
     * Navigates to the logged-in user's quizzes.
     */
    fun navigateToProfileQuizzes()

    /**
     * Navigates to the logged-in user's quiz results.
     */
    fun navigateToProfileQuizResults()

    /**
     * Navigates to a user's profile page by using their id.
     *
     * (Note: API currently disallows access to any but your own (/me)).
     */
    fun navigateToUser(userId: ObjectId)

    /**
     * Navigates to a user's list of created quizzes by using their id.
     *
     * (Note: API currently disallows access to any but your own (/me)).
     */
    fun navigateToUserQuizzes(userId: ObjectId)

    /**
     * Navigates to a user's list of quiz results by using their id.
     *
     * (Note: API currently disallows access to any but your own (/me)).
     */
    fun navigateToUserResults(userId: ObjectId)

    /**
     * Navigates to either the quiz creator or editor, depending on whether
     * [quizId] is null.
     *
     * (Note: technically the same final composable in both situations, but the functionality
     * is very different when creating vs editing.)
     */
    fun navigateToEditor(quizId: ObjectId? = null)

    /**
     * Navigates to the screen for taking a quiz.
     */
    fun navigateToForm(quizId: ObjectId)

    /**
     * Navigates to the list of results for a quiz.
     *
     * (Note: API currently only allows the user who created a quiz to view the results.)
     */
    fun navigateToQuizResults(quizId: ObjectId)

    /**
     * Navigates to a single result for a quiz using the id of the quiz and user.
     *
     * (Note: API allows either the user who created either the result or the quiz
     * to view the result.)
     */
    fun navigateToQuizResultDetail(quizId: ObjectId, userId: ObjectId)

    /**
     * Gets the parent [NavBackStackEntry] with the given route.
     */
    fun getBackStackEntry(route: String): NavBackStackEntry

    companion object {
        /**
         * Initial destination in the app.
         */
        const val START_DESTINATION = Screens.Users.baseRoute
    }
}

/**
 * Creates and remembers a [NavActions]. This uses [navController] as a key to [remember],
 * so a new [NavActions] will be created when [navController] changes.
 */
@Composable
fun rememberNavActions(
    navController: NavHostController,
): NavActions =
    remember(navController) {
        NavActionsImpl(navController)
    }

/**
 * Main [NavActions] for the app.
 */
private class NavActionsImpl(
    private val navController: NavHostController,
) : NavActions {
    private val screen: Screen?
        get() = screens[navController.currentDestination?.route]

    override fun navigateToLogIn() {
        if (screen in authScreens) {
            // Do not navigate to LogIn if already in the Auth flow (either LogIn or SignUp)
            return
        }

        navController.navigate(Screens.LogIn.route) {
            // Screens.LogIn is the start of the Auth flow
            popUpTo(Screens.LogIn.route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    override fun navigateToSignUp() {
        check(screen == Screens.LogIn) {
            "Can only navigate to SignUp from LogIn"
        }
        navController.navigate(Screens.SignUp.route) {
            popUpTo(Screens.LogIn.route)
            launchSingleTop = true
        }
    }

    override fun popLoginFlow() {
        if (screen !in authScreens) {
            // Do not navigate to if not in Auth flow
            return
        }
        navController.popBackStack(Screens.LogIn.route, inclusive = true)
    }

    override fun navigateToProfile() {
        navController.navigate(Screens.Users.Profile.buildPath("me")) {
            // Pop until the /users root
            popUpTo(Screens.Users.baseRoute) {
                // Only save state if it's a route in bottom navigation options (a profile route)
                saveState = screen in bottomNavDestinations
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun navigateToProfileQuizzes() {
        navController.navigate(Screens.Users.Quizzes.List.buildPath("me")) {
            // Pop until the /users root
            popUpTo(Screens.Users.baseRoute) {
                // Only save state if it's a route in bottom navigation options (a profile route)
                saveState = screen in bottomNavDestinations
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun navigateToProfileQuizResults() {
        navController.navigate(Screens.Users.QuizResults.buildPath("me")) {
            // Pop until the /users root
            popUpTo(Screens.Users.baseRoute) {
                // Only save state if it's a route in bottom navigation options (a profile route)
                saveState = screen in bottomNavDestinations
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun navigateToUser(userId: ObjectId) {
        navController.navigate(Screens.Users.Profile.buildPath(userId.value)) {
            launchSingleTop = true
        }
    }

    override fun navigateToUserQuizzes(userId: ObjectId) {
        navController.navigate(Screens.Users.Quizzes.List.buildPath(userId.value)) {
            launchSingleTop = true
        }
    }

    override fun navigateToUserResults(userId: ObjectId) {
        navController.navigate(Screens.Users.QuizResults.buildPath(userId.value)) {
            launchSingleTop = true
        }
    }

    override fun navigateToEditor(quizId: ObjectId?) {
        if (quizId == null) {
            navController.navigate(Screens.Users.Quizzes.Create.route) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(Screens.Users.Quizzes.Edit.buildPath(quizId.value)) {
                launchSingleTop = true
            }
        }
    }

    override fun navigateToForm(quizId: ObjectId) {
        navController.navigate(Screens.Quizzes.Form.buildPath(quizId.value)) {
            launchSingleTop = true
        }
    }

    override fun navigateToQuizResults(quizId: ObjectId) {
        navController.navigate(Screens.Quizzes.Results.buildPath(quizId.value)) {
            launchSingleTop = true
        }
    }

    override fun navigateToQuizResultDetail(quizId: ObjectId, userId: ObjectId) {
        navController.navigate(Screens.QuizResultDetail.buildPath(quizId.value, userId.value)) {
            launchSingleTop = true
        }
    }

    override fun getBackStackEntry(route: String): NavBackStackEntry {
        return navController.getBackStackEntry(route)
    }
}

/**
 * Handles calling navigation actions when [authenticationStateSource]'s current [AuthenticationState]
 * changes.
 */
@Composable
fun AuthRouter(
    authenticationStateSource: AuthenticationStateSource,
    onRequireAuthentication: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnRequireAuthentication by rememberUpdatedState(onRequireAuthentication)

    LaunchedEffect(authenticationStateSource) {
        snapshotFlow { authenticationStateSource.state }
            .map { it is AuthenticationState.RequireAuthentication }
            .distinctUntilChanged()
            .collect { requireAuthentication ->
                if (requireAuthentication) {
                    currentOnRequireAuthentication()
                } else {
                    currentOnAuthenticated()
                }
            }
    }
}

private val authScreens = setOf(Screens.LogIn, Screens.SignUp)