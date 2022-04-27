package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.util.describeMax
import com.github.jtaylorsoftware.quizapp.util.isInPast
import com.github.jtaylorsoftware.quizapp.util.periodBetweenNow
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

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
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    ProfileList {
        items(uiState.data, key = { it.id.value }) {
            ProfileListCard(Modifier.width(maxWidthDp)) {
                QuizListItem(it, onDeleteQuiz, navigateToEditor, navigateToResults, scaffoldState)
            }
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalTextApi::class)
@Composable
private fun QuizListItem(
    quiz: QuizListing,
    onDeleteQuiz: (ObjectId) -> Unit,
    navigateToEditor: (ObjectId?) -> Unit,
    navigateToResults: (ObjectId) -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
) {
    val scope = rememberCoroutineScope()

    QuizTitle(
        title = quiz.title,
        onDelete = {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    "Deleted quiz \"${quiz.title.truncated()}\""
                )
            }
            onDeleteQuiz(quiz.id)
        },
        onEdit = { navigateToEditor(quiz.id) },
        onViewResults = { navigateToResults(quiz.id) }
    )
    Text(
        "${quiz.questionCount} questions",
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        "${quiz.resultsCount} responses",
        modifier = Modifier.padding(bottom = 8.dp)
    )
    QuizLinkText(quiz.id, scaffoldState)
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .padding(bottom = 4.dp)
    ) {
        CreationTimestamp(quiz.date)
        Spacer(Modifier.weight(1.0f, fill = true))
        Expiration(quiz.expiration)
    }
}

@Composable
private fun QuizTitle(
    title: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onViewResults: () -> Unit
) {
    // Show title and buttons at top, in one line if possible
    FlowRow(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        mainAxisAlignment = FlowMainAxisAlignment.SpaceBetween,
        crossAxisSpacing = 8.dp,
        crossAxisAlignment = FlowCrossAxisAlignment.Center
    ) {
        Text(
            title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.h6
        )
        IconButtonRow(
            quizTitle = title,
            onDelete = onDelete,
            onEdit = onEdit,
            onViewResults = onViewResults
        )
    }
}

/**
 * Truncates a [String] to a maximum of [maxLength] characters, replacing overflow with
 * ellipses. The ellipses are counted against the maximum length.
 */
private fun String.truncated(maxLength: Int = 12): String {
    val overflow = "..."
    val minLengthToOverflow = maxLength - overflow.length
    return if (length <= maxLength) {
        this
    } else {
        replaceRange(minLengthToOverflow until length, overflow)
    }
}

@Preview(widthDp = 320)
@Composable
private fun QuizListItemPreview() {
    val listing = QuizListing(
        resultsCount = 1,
        questionCount = 1,
        title = "My Quiz".repeat(10),
    )
    QuizAppTheme {
        Surface {
            Column {
                QuizListItem(
                    quiz = listing,
                    onDeleteQuiz = {},
                    navigateToEditor = {},
                    navigateToResults = {})
            }
        }
    }
}

/**
 * The row of delete, edit, and "view results" buttons to display next to the quiz title.
 */
@Composable
private fun IconButtonRow(
    quizTitle: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onViewResults: () -> Unit,
) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    // Show dialog to prevent accidental deletion of quiz
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogOpen = false
                    onDelete()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            },
            text = { Text("Delete quiz \"$quizTitle\"? This cannot be undone.") },
        )
    }

    Row(horizontalArrangement = Arrangement.End) {
        IconButton(
            onClick = { dialogOpen = true },
        ) {
            Icon(Icons.Default.Delete, "Delete Quiz", tint = MaterialTheme.colors.error)
        }
        IconButton(
            onClick = onEdit,
        ) {
            Icon(Icons.Default.Edit, "Edit Quiz", tint = MaterialTheme.colors.secondary)
        }
        IconButton(
            onClick = onViewResults,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assessment_24),
                "View Results",
                tint = MaterialTheme.colors.secondaryVariant
            )
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
        Text("Created $timestamp", style = MaterialTheme.typography.caption)
    }
}

/**
 * Displays the text that the user can click to copy the link to the quiz form.
 */
@Composable
private fun QuizLinkText(quizId: ObjectId, scaffoldState: ScaffoldState = rememberScaffoldState()) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // The actual link to copy to clipboard
    val quizLink =
        remember { AnnotatedString("http://makequizzes.online/quizzes/${quizId.value}") }

    // The styled text to present to the user
    val quizLinkStyledText = run {
        val linkColor = MaterialTheme.colors.onSurface
        val clickableLinkColor = MaterialTheme.colors.secondary
        remember {
            buildAnnotatedString {
                withStyle(SpanStyle(linkColor)) {
                    append("Link: ")
                }
                withStyle(
                    SpanStyle(
                        clickableLinkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("quizzes/${quizId.value}")
                }
            }
        }
    }

    ClickableText(
        quizLinkStyledText,
        modifier = Modifier.padding(bottom = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onClick = { offset ->
            if (offset > "Link: ".length) {
                clipboardManager.setText(quizLink)
                scope.launch {
                    scaffoldState.snackbarHostState.showSnackbar("Copied quiz link to clipboard")
                }
            }
        })
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
        Text(
            "Expired",
            modifier = Modifier.padding(end = 12.dp), // Line up with row of IconButton
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.caption
        )
    }
}

@Preview
@Composable
private fun QuizListScreenPreview() {
    val uiState = QuizListUiState.QuizList(
        loading = LoadingState.NotStarted,
        deleteQuizStatus = LoadingState.NotStarted,
        data = (0..3).map {
            QuizListing(
                id = ObjectId(UUID.randomUUID().toString()),
                resultsCount = it,
                questionCount = it,
                title = "My Quiz ${it + 1}",
                date = Instant.now().minus(3, ChronoUnit.DAYS),
                expiration = Instant.now().minus((it + 1).toLong(), ChronoUnit.DAYS),
            )
        }
    )
    QuizAppTheme {
        Surface {
            QuizListScreen(
                uiState = uiState,
                onDeleteQuiz = {},
                navigateToEditor = {},
                navigateToResults = {},
                maxWidthDp = LocalConfiguration.current.screenWidthDp.dp
            )
        }
    }
}