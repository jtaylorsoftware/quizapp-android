package com.github.jtaylorsoftware.quizapp.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationState
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationStateSource
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Models navigation actions for the app, making it easier to navigate to
 * the correct route paths while passing their expected arguments.
 */
interface Navigator {
    val state: NavigatorState

    /**
     * Navigates to the login screen.
     */
    fun navigateToLogIn()

    /**
     * Exits the app when back button is pressed and on login screen.
     *
     * @param finish Function that can be called to exit the app.
     */
    fun onLoginBackPressed(finish: () -> Unit)

    /**
     * Navigates to the sign-up screen.
     */
    fun navigateToSignUp()

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
     * Pops the current screen off the top of the backstack but will finish the activity
     * if navigated from a deep link.
     */
    fun navigateUp()

    companion object {
        /**
         * Initial destination in the app.
         */
        const val START_DESTINATION = Screens.Users.baseRoute
    }
}

/**
 * Models cross-cutting navigation-related concerns at the top-level of the application, including
 * authentication status and the current route.
 *
 * This can be passed down for screens to consume and produce navigation events, such as
 * redirection when authentication is required.
 */
data class NavigatorState(val currentRoute: String)

/**
 * Creates a [MutableState] of [NavigatorState].
 */
fun mutableNavigatorState(
    currentRoute: String = Navigator.START_DESTINATION
): MutableState<NavigatorState> = mutableStateOf(NavigatorState(currentRoute))

/**
 * Creates and remembers a [Navigator]. This uses [navController] as a key to [remember],
 * so a new [Navigator] will be created when [navController] changes. However, it reuses
 * the [initialState].
 *
 * @param initialState The initial state to create the [Navigator] with.
 */
@Composable
fun rememberNavigator(
    navController: NavHostController,
    initialState: MutableState<NavigatorState> = mutableNavigatorState()
): Navigator =
    remember(navController) {
        NavigatorImpl(navController, initialState)
    }

/**
 * Main [Navigator] for the app.
 */
private class NavigatorImpl(
    private val navController: NavHostController,
    initialState: MutableState<NavigatorState> = mutableNavigatorState()
) : Navigator {
    private var _state by initialState
    override val state: NavigatorState
        get() = _state

    init {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            _state = _state.copy(currentRoute = requireNotNull(destination.route) {
                "navController destination.route was null"
            })
        }
    }

    override fun navigateToLogIn() {
        navController.navigate(Screens.LogIn.route) {
//            popUpTo(Screens.LogIn.route)
            launchSingleTop = true
        }
    }

    override fun onLoginBackPressed(finish: () -> Unit) {
        check(state.currentRoute == Screens.LogIn.route) {
            "Can only call onLoginBackPressed from LogIn"
        }
        finish()
    }

    override fun navigateToSignUp() {
        check(state.currentRoute == Screens.LogIn.route) {
            "Can only navigate to SignUp from LogIn"
        }
        navController.navigate(Screens.SignUp.route) {
//            popUpTo(Screens.LogIn.route)
            launchSingleTop = true
        }
    }

    override fun navigateToProfile() {
        navController.navigate(Screens.Users.Profile.buildPath("me")) {
            // Pop until the /users root
            popUpTo(Screens.Users.baseRoute) {
                // Only save state if it's a route in bottom navigation options (a profile route)
                navController.currentDestination?.route?.let { currentRoute ->
                    saveState = currentRoute in bottomNavRoutes
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun navigateToProfileQuizzes() {
        navController.navigate(Screens.Users.Quizzes.buildPath("me")) {
            // Pop until the /users root
            popUpTo(Screens.Users.baseRoute) {
                // Only save state if it's a route in bottom navigation options (a profile route)
                navController.currentDestination?.route?.let { currentRoute ->
                    saveState = currentRoute in bottomNavRoutes
                }
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
                navController.currentDestination?.route?.let { currentRoute ->
                    saveState = currentRoute in bottomNavRoutes
                }
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
        navController.navigate(Screens.Users.Quizzes.buildPath(userId.value)) {
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
            navController.navigate(Screens.Quizzes.Create.route) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(Screens.Quizzes.Edit.buildPath(quizId.value)) {
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

    override fun navigateUp() {
        navController.navigateUp()
    }
}

/**
 * Installs a [Composable] that will use this [Navigator] to navigate based on
 * [authenticationStateSource].
 */
@OptIn(InternalCoroutinesApi::class)
@Composable
fun Navigator.AuthHandler(authenticationStateSource: AuthenticationStateSource) {
    LaunchedEffect(authenticationStateSource) {
        snapshotFlow { authenticationStateSource.state }
            .map { it is AuthenticationState.RequireAuthentication }
            .distinctUntilChanged()
            .collect { requireAuthentication ->
                if (requireAuthentication) {
                    if (state.currentRoute != Screens.LogIn.route) {
                        navigateToLogIn()
                    }
                } else {
                    if (state.currentRoute == Screens.LogIn.route) {
                        navigateUp()
                    }
                }
            }
    }
}