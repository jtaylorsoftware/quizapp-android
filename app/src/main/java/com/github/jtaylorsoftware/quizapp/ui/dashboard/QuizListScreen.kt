package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.util.describeMax
import com.github.jtaylorsoftware.quizapp.util.isInPast
import com.github.jtaylorsoftware.quizapp.util.periodBetweenNow
import java.time.Instant

/**
 * Displays the signed-in user's list of created quizzes, in [QuizListing] format.
 *
 * @param onDeleteQuiz Callback invoked when the user presses "Delete" on a QuizListing. It should
 * accept the `id` of the Quiz to be deleted.
 *
 * @param navigateToEditor Callback invoked when the user presses "Edit" on a QuizListing. It should
 * accept the `id` of the Quiz to be edited.
 *
 * @param navigateToResults Callback invoked when the user presses "View Results" on a QuizListing. It should
 * accept the `id` of the Quiz to view the results of.
 */
@Composable
fun QuizListScreen(
    uiState: QuizListUiState.QuizList,
    onDeleteQuiz: (ObjectId) -> Unit,
    navigateToEditor: (ObjectId?) -> Unit,
    navigateToResults: (ObjectId) -> Unit,
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Text("Your Quizzes")
        }

        items(uiState.data, key = { it.id.value }) { quiz ->
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuizListItem(
    quiz: QuizListing,
    onDeleteQuiz: (ObjectId) -> Unit,
    navigateToEditor: (ObjectId?) -> Unit,
    navigateToResults: (ObjectId) -> Unit
) {
    val questionNoun = rememberCapitalizedNoun(quiz.questionCount, "question", "questions")
    val responseNoun = rememberCapitalizedNoun(quiz.resultsCount, "response", "responses")
    val clipboardManager = LocalClipboardManager.current

    Box {
        Column {
            // Title     [Delete] [Edit] [Results]
            Row {
                Text("\"${quiz.title}\"")
                IconButton(onClick = { onDeleteQuiz(quiz.id) }) {
                    Icon(Icons.Default.Delete, "Delete Quiz", tint = MaterialTheme.colors.error)
                }
                IconButton(onClick = { navigateToEditor(quiz.id) }) {
                    Icon(Icons.Default.Edit, "Edit Quiz", tint = MaterialTheme.colors.secondary)
                }
                IconButton(onClick = { navigateToResults(quiz.id) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assessment_24),
                        "View Results",
                        tint = MaterialTheme.colors.secondaryVariant
                    )
                }
            }
            Text("${quiz.questionCount} $questionNoun")
            Text("${quiz.resultsCount} $responseNoun")
            Text("Link: ")
            Text(
                "quizzes/${quiz.id.value}",
                modifier = Modifier.combinedClickable(
                    onClick = { navigateToResults(quiz.id) },
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString("http://makequizzes.online/quizzes/${quiz.id.value}"))
                    }),
                color = MaterialTheme.colors.secondary
            )
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

    CompositionLocalProvider(
        LocalContentAlpha provides ContentAlpha.medium
    ) {
        Text("Created $timestamp")
    }
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
        Text("Expired", color = MaterialTheme.colors.error)
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