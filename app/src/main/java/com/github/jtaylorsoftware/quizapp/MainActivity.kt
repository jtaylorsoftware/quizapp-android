package com.github.jtaylorsoftware.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.jtaylorsoftware.quizapp.data.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.dashboard.ProfileScreen
import com.github.jtaylorsoftware.quizapp.ui.dashboard.QuizScreen
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    private val quizzes = listOf(
        // Expired, created 5 days ago
        QuizListing(
            id = "123",
            date = Instant.now().minus(5, ChronoUnit.DAYS),
            expiration = Instant.now().minus(3, ChronoUnit.DAYS),
            title = "Quiz 1",
            questionCount = 1,
            resultsCount = 1,
        ),
        // Not expired, created 5 months ago
        QuizListing(
            id = "456",
            date = LocalDateTime.now().minusMonths(5)
                .atZone(ZoneId.systemDefault()).toInstant(),
            title = "Quiz 2",
            questionCount = 2,
            resultsCount = 2,
        ),
        // Not expired, created 5 years ago
        QuizListing(
            id = "789",
            date = LocalDateTime.now().minusYears(5)
                .atZone(ZoneId.systemDefault()).toInstant(),
            title = "Quiz 3",
            questionCount = 3,
            resultsCount = 3,
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    QuizScreen(
                        quizzes = quizzes,
                        onDeleteQuiz = {},
                        navigateToEditor = {},
                        navigateToResults = {}
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    QuizAppTheme {
        Greeting("Android")
    }
}