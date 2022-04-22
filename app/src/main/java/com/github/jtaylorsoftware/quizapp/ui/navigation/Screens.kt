package com.github.jtaylorsoftware.quizapp.ui.navigation

import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

/**
 * Models a nav destination to visit in the app.
 */
sealed class Screen(
    /**
     * The path to this screen in the navigation graph.
     */
    val route: String
)

/**
 * The screens to visit in the app.
 */
object Screens {
    object LogIn : Screen("login")

    object SignUp : Screen("signup")

    sealed class Users(route: String) : Screen(
        route = "$baseRoute/$route",
    ) {
        /**
         * Builds a path string matching this screen's route by replacing
         * the route parameters with an actual value.
         */
        abstract fun buildPath(user: String): String

        object Profile : Users("{user}") {
            override fun buildPath(user: String): String = "$baseRoute/$user"
        }

        object Quizzes : Users("{user}/quizzes") {
            override fun buildPath(user: String): String = "$baseRoute/$user/quizzes"
        }

        object QuizResults : Users("{user}/results") {
            override fun buildPath(user: String): String = "$baseRoute/$user/results"
        }

        companion object {
            const val baseRoute = "users"
            val navArgs = listOf(navArgument("user") { defaultValue = "me" })
        }
    }

    sealed class Quizzes(
        route: String,
    ) : Screen(
        route = "$baseRoute/$route",
    ) {
        /**
         * Builds a path string matching this screen's route by replacing
         * the route parameters with an actual value.
         */
        abstract fun buildPath(quiz: String): String

        object Create : Quizzes("create") {
            override fun buildPath(quiz: String): String = "$baseRoute/create"
        }

        object Edit : Quizzes("{quiz}/edit") {
            override fun buildPath(quiz: String): String = "$baseRoute/$quiz/edit"
        }

        object Form : Quizzes("{quiz}/form") {
            override fun buildPath(quiz: String): String = "$baseRoute/$quiz/form"
            val deepLinks = listOf(navDeepLink {
                uriPattern = "$APP_URI/quizzes/{quiz}"
            })
        }

        object Results : Quizzes("{quiz}/results") {
            override fun buildPath(quiz: String): String = "$baseRoute/$quiz/results"
        }

        companion object {
            const val baseRoute = "quizzes"
        }
    }

    object QuizResultDetail : Screen("results?quiz={quiz}&user={user}") {
        /**
         * Builds a path string matching this screen's route by replacing
         * the route parameters with an actual value.
         */
        fun buildPath(quiz: String, user: String): String =
            "$baseRoute?quiz=$quiz&user=$user"

        const val baseRoute = "results"
    }
}

const val APP_URI = "http://www.makequizzes.online"