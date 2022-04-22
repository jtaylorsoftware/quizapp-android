package com.github.jtaylorsoftware.quizapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationState
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationStateSource
import com.github.jtaylorsoftware.quizapp.ui.components.AppBottomNavigation
import com.github.jtaylorsoftware.quizapp.ui.dashboard.ProfileRoute
import com.github.jtaylorsoftware.quizapp.ui.dashboard.QuizListRoute
import com.github.jtaylorsoftware.quizapp.ui.dashboard.QuizResultDetailRoute
import com.github.jtaylorsoftware.quizapp.ui.dashboard.QuizResultListRoute
import com.github.jtaylorsoftware.quizapp.ui.login.LoginRoute
import com.github.jtaylorsoftware.quizapp.ui.quiz.QuizEditorRoute
import com.github.jtaylorsoftware.quizapp.ui.quiz.QuizFormRoute
import com.github.jtaylorsoftware.quizapp.ui.signup.SignupRoute

/**
 * The navigation graph of the app, containing all destinations.
 *
 * @param authenticationStateSource Source of [AuthenticationState] for the app.
 *
 * @param onFinish Function that can be called to finish the activity.
 */
@Composable
fun NavGraph(
    authenticationStateSource: AuthenticationStateSource,
    onFinish: () -> Unit,
    navController: NavHostController = rememberNavController(),
    navigator: Navigator = rememberNavigator(navController)
) {
    navigator.AuthHandler(authenticationStateSource)

    NavHost(navController, startDestination = Navigator.START_DESTINATION) {
        composable(Screens.LogIn.route) {
            LoginRoute(
                viewModel = hiltViewModel(),
                onBackPressed = { navigator.onLoginBackPressed { onFinish() } },
                navigateToSignUp = navigator::navigateToSignUp
            )
        }

        composable(Screens.SignUp.route) {
            SignupRoute(
                viewModel = hiltViewModel(),
                navigateToLogin = navigator::navigateToLogIn
            )
        }

        usersGraph(navigator)

        quizRoutes(navigator)

        quizResultDetailRoute()
    }
}

/**
 * Graph for an arbitrary user's profile data, including their created quizzes and results.
 */
private fun NavGraphBuilder.usersGraph(
    navigator: Navigator,
) {
    navigation(
        startDestination = Screens.Users.Profile.route,
        route = Screens.Users.baseRoute,
        arguments = Screens.Users.navArgs
    ) {
        composable(Screens.Users.Profile.route) {
            ProfileRoute(
                viewModel = hiltViewModel(),
                navigateToQuizScreen = navigator::navigateToProfileQuizzes,
                navigateToResultScreen = navigator::navigateToProfileQuizResults,
                bottomNavigation = { AppBottomNavigation(navigator) }
            )
        }
        composable(Screens.Users.Quizzes.route) {
            QuizListRoute(
                viewModel = hiltViewModel(),
                navigateToEditor = navigator::navigateToEditor,
                navigateToResults = navigator::navigateToQuizResults,
                bottomNavigation = { AppBottomNavigation(navigator) }
            )
        }
        composable(Screens.Users.QuizResults.route) {
            QuizResultListRoute(
                viewModel = hiltViewModel(),
                navigateToDetailScreen = navigator::navigateToQuizResultDetail,
                bottomNavigation = { AppBottomNavigation(navigator) }
            )
        }
    }
}

/**
 * Graph for a creating and editing quizzes. Also contains a route for getting a quiz as a form
 * to respond to and a route for viewing the results of a quiz.
 */
private fun NavGraphBuilder.quizRoutes(navigator: Navigator) {
    composable(Screens.Quizzes.Create.route) {
        QuizEditorRoute(
            viewModel = hiltViewModel(),
            onUploaded = navigator::navigateUp
        )
    }
    composable(Screens.Quizzes.Edit.route) {
        QuizEditorRoute(
            viewModel = hiltViewModel(),
            onUploaded = navigator::navigateUp
        )
    }
    composable(
        Screens.Quizzes.Form.route,
        deepLinks = Screens.Quizzes.Form.deepLinks
    ) {
        QuizFormRoute(
            viewModel = hiltViewModel(),
            onUploaded = navigator::navigateUp
        )
    }
    composable(Screens.Quizzes.Results.route) {
        QuizResultListRoute(
            viewModel = hiltViewModel(),
            navigateToDetailScreen = navigator::navigateToQuizResultDetail,
            bottomNavigation = {}
        )
    }
}

/**
 * Graph destination for viewing a single quiz result.
 */
private fun NavGraphBuilder.quizResultDetailRoute() {
    composable(Screens.QuizResultDetail.route) {
        QuizResultDetailRoute(viewModel = hiltViewModel())
    }
}
