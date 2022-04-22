package com.github.jtaylorsoftware.quizapp.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationState
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationStateSource
import com.github.jtaylorsoftware.quizapp.ui.navigation.NavGraph
import com.github.jtaylorsoftware.quizapp.ui.navigation.Navigator
import com.github.jtaylorsoftware.quizapp.ui.navigation.rememberNavigator
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * The root composable for the app.
 *
 * @param authenticationStateSource Source of [AuthenticationState] for the app.
 *
 * @param onFinish Function that can be called to finish the activity.
 */
@Composable
fun QuizApp(
    authenticationStateSource: AuthenticationStateSource,
    onFinish: () -> Unit,
    navController: NavHostController = rememberNavController(),
    navigator: Navigator = rememberNavigator(navController)
) {
    QuizAppTheme {
        Surface {
            NavGraph(
                authenticationStateSource = authenticationStateSource,
                onFinish = onFinish,
                navController = navController,
                navigator = navigator
            )
        }
    }
}