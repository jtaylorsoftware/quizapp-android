package com.github.jtaylorsoftware.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RestrictTo
import androidx.navigation.compose.rememberNavController
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationStateSource
import com.github.jtaylorsoftware.quizapp.ui.QuizApp
import com.github.jtaylorsoftware.quizapp.ui.calculateWindowSizeClass
import com.github.jtaylorsoftware.quizapp.ui.navigation.NavActions
import com.github.jtaylorsoftware.quizapp.ui.navigation.rememberNavActions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authStateSource: AuthenticationStateSource

    @RestrictTo(RestrictTo.Scope.TESTS)
    lateinit var navActions: NavActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val windowSizeClass = calculateWindowSizeClass(this)
            navActions = rememberNavActions(navController)
            QuizApp(
                windowSizeClass = windowSizeClass,
                authenticationStateSource = authStateSource,
                onFinish = { finish() },
                navController = navController,
                navActions = navActions
            )
        }
    }
}