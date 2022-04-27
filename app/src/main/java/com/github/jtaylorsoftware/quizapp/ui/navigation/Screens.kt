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
        object Profile : Users("{user}") {
            /**
             * Builds a path string matching this screen's route by replacing
             * the route parameters with an actual value.
             */
            fun buildPath(user: String): String = "$baseRoute/$user"
        }

        sealed class Quizzes(route: String) : Users("{user}/$baseRoute/$route") {
            object List : Quizzes("view") {
                fun buildPath(user: String): String = "${buildUserPath(user)}/view"
            }

            object Create : Quizzes("create")

            object Edit : Quizzes("{quiz}/edit") {
                fun buildPath(quiz: String): String = "${buildUserPath()}/$quiz/edit"
            }

            companion object {
                const val baseRoute = "quizzes"
                private fun buildUserPath(user: String = "me"): String = "${Users.baseRoute}/$user/$baseRoute"
            }
        }

        object QuizResults : Users("{user}/results") {
            fun buildPath(user: String): String = "$baseRoute/$user/results"
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

val screens = mapOf(
    Screens.LogIn.route to Screens.LogIn,
    Screens.SignUp.route to Screens.SignUp,
    Screens.Users.Profile.route to Screens.Users.Profile,
    Screens.Users.Quizzes.List.route to Screens.Users.Quizzes.List,
    Screens.Users.Quizzes.Create.route to Screens.Users.Quizzes.Create,
    Screens.Users.Quizzes.Edit.route to Screens.Users.Quizzes.Edit,
    Screens.Users.QuizResults.route to Screens.Users.QuizResults,
    Screens.Quizzes.Form.route to Screens.Quizzes.Form,
    Screens.Quizzes.Results.route to Screens.Quizzes.Results,
    Screens.QuizResultDetail.route to Screens.QuizResultDetail,
)