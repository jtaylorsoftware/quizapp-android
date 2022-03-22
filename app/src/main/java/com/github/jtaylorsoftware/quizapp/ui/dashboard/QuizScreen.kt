package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.QuizListing
import com.github.jtaylorsoftware.quizapp.util.describeMax
import com.github.jtaylorsoftware.quizapp.util.isInPast
import com.github.jtaylorsoftware.quizapp.util.periodBetweenNow
import java.time.Instant

/**
 * Displays the signed-in user's list of created quizzes, in [QuizListing] format.
 *
 * @param quizzes The user's QuizListings.
 * @param onDeleteQuiz Callback invoked when the user presses "Delete" on a QuizListing. It should
 *                     accept the `id` of the Quiz to be deleted.
 * @param navigateToEditor Callback invoked when the user presses "Edit" on a QuizListing. It should
 *                         accept the `id` of the Quiz to be edited.
 * @param navigateToResults Callback invoked when the user presses "View Results" on a QuizListing. It should
 *                          accept the `id` of the Quiz to view the results of.
 */
@Composable
fun QuizScreen(
    quizzes: List<QuizListing>,
    onDeleteQuiz: (String) -> Unit,
    navigateToEditor: (String) -> Unit,
    navigateToResults: (String) -> Unit
) {
    LazyColumn {
        item {
            Text("Your Quizzes")
        }

        items(quizzes, key = { it.id }) { quiz ->
            QuizListItem(quiz, onDeleteQuiz, navigateToEditor, navigateToResults)
        }
    }
}

/**
 * Displays the data for one one [QuizListing] and provides buttons for the user
 * to edit, delete, or view the results of the Quiz that the QuizListing is for.
 *
 * @param quiz The listing to display.
 * @param onDeleteQuiz Callback invoked when the user presses the "Delete" button.
 * @param navigateToEditor Callback invoked when the user presses the "Edit" button.
 * @param navigateToResults Callback invoked when the user presses the "View Results" button.
 */
@Composable
private fun QuizListItem(
    quiz: QuizListing,
    onDeleteQuiz: (String) -> Unit,
    navigateToEditor: (String) -> Unit,
    navigateToResults: (String) -> Unit
) {
    val questionNoun = rememberCapitalizedNoun(quiz.questionCount, "question", "questions")
    val responseNoun = rememberCapitalizedNoun(quiz.resultsCount, "response", "responses")

    Box {
        Column {
            // Title     [Delete] [Edit] [Results]
            Row {
                Text("\"${quiz.title}\"")
                IconButton(onClick = { onDeleteQuiz(quiz.id) }, modifier = Modifier.semantics {
                    contentDescription = "Delete Quiz"
                }) {
                    Icon(Icons.Default.Delete, null)
                }
                IconButton(onClick = { navigateToEditor(quiz.id) }, modifier = Modifier.semantics {
                    contentDescription = "Edit Quiz"
                }) {
                    Icon(Icons.Default.Edit, null)
                }
                IconButton(onClick = { navigateToResults(quiz.id) }, modifier = Modifier.semantics {
                    contentDescription = "View Results"
                }) {
                    Icon(painter = painterResource(R.drawable.ic_assessment_24), null)
                }
            }
            Text("${quiz.questionCount} $questionNoun")
            Text("${quiz.resultsCount} $responseNoun")
            Text("Link: /quizzes/${quiz.id}")
            Row {
                CreationTimestamp(quiz.date)
                Expiration(quiz.expiration)
            }
        }
    }
}

/**
 * Displays a timestamp in the form of "Created N D/M/Y ago"
 * depending on how long ago the timestamp is.
 *
 * @param date The timestamp to check, as an Instant.
 */
@Composable
private fun CreationTimestamp(date: Instant) {
    val timestamp: String by remember {
        derivedStateOf {
            date.periodBetweenNow().describeMax(" ago")
        }
    }

    Text("Created $timestamp")
}

/**
 * Displays text containing "Expired" if the given Instant can be considered expired.
 *
 * @param expiration The timestamp to check, as an Instant.
 */
@Composable
private fun Expiration(expiration: Instant) {
    val expired: Boolean by remember { derivedStateOf { expiration.isInPast() } }
    if (expired) {
        Text("Expired")
    }
}

/**
 * Computes and remembers the correct form of a noun and returns it with the first letter capitalized.
 *
 * @param count The number of the noun that exists.
 * @param singular The singular form of the noun.
 * @param plural The plural form of the noun.
 */
@Composable
private fun rememberCapitalizedNoun(count: Int, singular: String, plural: String): String {
    val noun: String by remember { derivedStateOf { if (count == 1) singular else plural } }
    val capitalizedNoun: String by remember { derivedStateOf { noun.replaceFirstChar { it.uppercase() } } }
    return capitalizedNoun
}