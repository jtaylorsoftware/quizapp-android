package com.github.jtaylorsoftware.quizapp.ui.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.github.jtaylorsoftware.quizapp.ui.WindowSizeClass
import com.github.jtaylorsoftware.quizapp.ui.dashboard.*
import com.github.jtaylorsoftware.quizapp.ui.quiz.QuizEditorRoute
import com.github.jtaylorsoftware.quizapp.ui.quiz.QuizFormRoute
import com.github.jtaylorsoftware.quizapp.ui.signinsignup.SignInRoute
import com.github.jtaylorsoftware.quizapp.ui.signinsignup.SignupRoute

/**
 * The navigation graph of the app, containing all destinations.
 *
 * @param onFinish Function that can be called to finish the activity.
 *
 * @param navActions The single [NavActions] instance for the app.
 *
 * @param maxWidthDp The maximum allowed width of child layouts.
 */
@Composable
fun NavGraph(
    onFinish: () -> Unit,
    navController: NavHostController = rememberNavController(),
    navActions: NavActions = rememberNavActions(navController),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    maxWidthDp: Dp,
    windowSizeClass: WindowSizeClass,
) {
    NavHost(navController, startDestination = NavActions.START_DESTINATION) {
        composable(Screens.LogIn.route) {
            SignInRoute(
                viewModel = hiltViewModel(),
                onBackPressed = onFinish,
                navigateToSignUp = navActions::navigateToSignUp,
                scaffoldState = scaffoldState,
            )
        }

        composable(Screens.SignUp.route) {
            SignupRoute(
                viewModel = hiltViewModel(),
                navigateToLogin = navController::navigateUp,
                scaffoldState = scaffoldState,
            )
        }

        usersGraph(navController, navActions, scaffoldState, maxWidthDp, windowSizeClass)

        quizRoutes(navController, navActions, onFinish, scaffoldState, maxWidthDp)

        quizResultDetailRoute(scaffoldState, maxWidthDp)
    }
}

/**
 * Graph for an arbitrary user's profile data, including their created quizzes and results.
 */
private fun NavGraphBuilder.usersGraph(
    navController: NavHostController,
    navActions: NavActions,
    scaffoldState: ScaffoldState,
    maxWidthDp: Dp,
    windowSizeClass: WindowSizeClass,
) {
    navigation(
        startDestination = Screens.Users.Profile.route,
        route = Screens.Users.baseRoute,
        arguments = Screens.Users.navArgs
    ) {
        composable(Screens.Users.Profile.route) {
            ProfileRoute(
                viewModel = hiltViewModel(),
                navigateToQuizCreator = navActions::navigateToEditor,
                navigateToQuizResults = navActions::navigateToProfileQuizResults,
                bottomNavigation = { BottomNavigation(navController, navActions) },
                scaffoldState = scaffoldState,
                maxWidthDp = maxWidthDp,
                windowSizeClass = windowSizeClass,
            )
        }
        navigation(
            startDestination = Screens.Users.Quizzes.List.route,
            route = Screens.Users.Quizzes.baseRoute,
        ) {
            composable(Screens.Users.Quizzes.List.route) { backStackEntry ->
                QuizListRoute(
                    viewModel = navController.getParentViewModel(
                        backStackEntry = backStackEntry,
                        route = Screens.Users.baseRoute
                    ),
                    navigateToEditor = navActions::navigateToEditor,
                    navigateToResultsForQuiz = navActions::navigateToQuizResults,
                    bottomNavigation = { BottomNavigation(navController, navActions) },
                    scaffoldState = scaffoldState,
                    maxWidthDp = maxWidthDp
                )
            }
            composable(Screens.Users.Quizzes.Create.route) { backStackEntry ->
                val quizListViewModel = navController.getParentViewModel<QuizListViewModel>(
                    backStackEntry = backStackEntry,
                    route = Screens.Users.baseRoute
                )
                QuizEditorRoute(
                    viewModel = hiltViewModel(),
                    onUploaded = {
                        quizListViewModel.refresh()
                        navController.navigateUp()
                    },
                    scaffoldState = scaffoldState,
                    maxWidthDp = maxWidthDp
                )
            }
            composable(Screens.Users.Quizzes.Edit.route) { backStackEntry ->
                val quizListViewModel = navController.getParentViewModel<QuizListViewModel>(
                    backStackEntry = backStackEntry,
                    route = Screens.Users.baseRoute
                )
                QuizEditorRoute(
                    viewModel = hiltViewModel(),
                    onUploaded = {
                        quizListViewModel.refresh()
                        navController.navigateUp()
                    },
                    scaffoldState = scaffoldState,
                    maxWidthDp = maxWidthDp
                )
            }
        }

        composable(Screens.Users.QuizResults.route) {
            QuizResultListRoute(
                viewModel = hiltViewModel(),
                navigateToDetailScreen = navActions::navigateToQuizResultDetail,
                bottomNavigation = { BottomNavigation(navController, navActions) },
                scaffoldState = scaffoldState,
                maxWidthDp = maxWidthDp
            )
        }
    }
}

/**
 * Graph for a creating and editing quizzes. Also contains a route for getting a quiz as a form
 * to respond to and a route for viewing the results of a quiz.
 */
private fun NavGraphBuilder.quizRoutes(
    navController: NavHostController,
    navActions: NavActions,
    onFinish: () -> Unit,
    scaffoldState: ScaffoldState,
    maxWidthDp: Dp,
) {
    composable(
        Screens.Quizzes.Form.route,
        deepLinks = Screens.Quizzes.Form.deepLinks
    ) {
        QuizFormRoute(
            viewModel = hiltViewModel(),
            // User can only open forms in new activities (deep navigation)
            onUploaded = onFinish,
            onBackPressed = onFinish,
            scaffoldState = scaffoldState,
            maxWidthDp = maxWidthDp
        )
    }
    composable(Screens.Quizzes.Results.route) {
        QuizResultListRoute(
            viewModel = hiltViewModel(),
            navigateToDetailScreen = navActions::navigateToQuizResultDetail,
            bottomNavigation = {},
            scaffoldState = scaffoldState,
            maxWidthDp = maxWidthDp
        )
    }
}

/**
 * Graph destination for viewing a single quiz result.
 */
private fun NavGraphBuilder.quizResultDetailRoute(
    scaffoldState: ScaffoldState,
    maxWidthDp: Dp,
) {
    composable(Screens.QuizResultDetail.route) {
        QuizResultDetailRoute(
            viewModel = hiltViewModel(),
            scaffoldState = scaffoldState,
            maxWidthDp = maxWidthDp
        )
    }
}

/**
 * Gets a [ViewModel] scoped to the given parent [route]. It uses [backStackEntry] to
 * remember the [NavBackStackEntry] of the parent.
 */
@Composable
inline fun <reified V : ViewModel> NavController.getParentViewModel(
    backStackEntry: NavBackStackEntry,
    route: String
): V {
    val parentEntry = remember(backStackEntry) {
        getBackStackEntry(route)
    }
    return hiltViewModel(parentEntry)
}