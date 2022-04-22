package com.github.jtaylorsoftware.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RestrictTo
import androidx.navigation.compose.rememberNavController
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationStateSource
import com.github.jtaylorsoftware.quizapp.ui.QuizApp
import com.github.jtaylorsoftware.quizapp.ui.navigation.Navigator
import com.github.jtaylorsoftware.quizapp.ui.navigation.rememberNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authStateSource: AuthenticationStateSource

    @RestrictTo(RestrictTo.Scope.TESTS)
    lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            navigator = rememberNavigator(navController)
            QuizApp(
                authenticationStateSource = authStateSource,
                onFinish = { finish() },
                navController = navController,
                navigator = navigator
            )
        }
    }
}