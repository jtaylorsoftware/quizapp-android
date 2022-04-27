package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.util.toLocalizedString
import java.util.*
import kotlin.random.Random

/**
 * Displays the signed-in user's list of results for their created quizzes, in [QuizResultListing] format.
 *
 * @param navigateToDetails Callback invoked when the user presses on a listing.
 * It accepts the id of the quiz and id of the user for the selected listing.
 */
@Composable
fun QuizResultListScreen(
    uiState: QuizResultListUiState.ListForUser,
    navigateToDetails: (ObjectId, ObjectId) -> Unit,
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    ProfileList {
        items(uiState.data, key = { it.id.value }) {
            ProfileListCard(
                Modifier
                    .clickable { navigateToDetails(it.quiz, it.user) }
                    .width(maxWidthDp),
            ) {
                ResultListItemForCurrentUser(it)
            }
        }
    }
}

/**
 * Displays the list of results for a quiz, in [QuizResultListing] format.
 *
 * @param navigateToDetails Callback invoked when the user presses on a listing.
 * It accepts the id of the quiz and id of the user for the selected listing.
 */
@Composable
fun QuizResultListScreen(
    uiState: QuizResultListUiState.ListForQuiz,
    navigateToDetails: (ObjectId, ObjectId) -> Unit,
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    ProfileList(
        header = {
            Box(Modifier.width(maxWidthDp)) {
                ListHeader("Results for \"${uiState.quizTitle.ifBlank { "Quiz" }}\"")
            }
        }
    ) {
        items(uiState.data, key = { it.id.value }) {
            ProfileListCard(
                Modifier
                    .clickable { navigateToDetails(it.quiz, it.user) }
                    .width(maxWidthDp)
            ) {
                ResultListItemForQuiz(it)
            }
        }
    }
}

/**
 * Displays the header for the list, with a caption explaining navigation.
 */
@Composable
private fun ListHeader(text: String) {
    Column {
        Text(
            text,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Tap a Result to view graded questions",
            style = MaterialTheme.typography.subtitle1
        )
    }
}

/**
 * Displays the data for one [QuizResultListing] to a [Quiz] created by the signed-in user,
 * and a button to view the full details of the result.
 *
 * @param result The listing to display.
 */
@Composable
private fun ResultListItemForCurrentUser(result: QuizResultListing) {
    ResultListItemContent(
        left = {
            Text(
                "\"${result.quizTitle}\"",
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.h6
            )
            CompositionLocalProvider(
                LocalContentAlpha provides ContentAlpha.medium
            ) {
                Text(
                    "by ${result.createdBy}",
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.caption
                )
            }
        },
        right = {
            ScoreRingChart(result.score)
        }
    )
}

/**
 * Displays the data for one [QuizResultListing] by a user other than the signed-in user (because
 * the Quiz that the QuizResult belongs to is one created by the signed-in user).
 *
 * @param result The listing to display.
 */
@Composable
private fun ResultListItemForQuiz(result: QuizResultListing) {
    ResultListItemContent(
        left = {
            Text(
                "${result.username}'s results",
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.h6
            )
            CompositionLocalProvider(
                LocalContentAlpha provides ContentAlpha.medium
            ) {
                Text(
                    "taken on ${result.date.toLocalizedString()}",
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.caption
                )
            }
        },
        right = {
            ScoreRingChart(result.score)
        }
    )
}

/**
 * Structures the content of a list item into a two-column row layout.
 */
@Composable
private fun ResultListItemContent(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(0.80f)
                .fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween
        ) {
            left()
        }
        Column(
            Modifier
                .weight(0.2f)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            right()
        }
    }
}

@Preview
@Composable
private fun QuizResultListForUserPreview() {
    val uiState = QuizResultListUiState.ListForUser(
        loading = LoadingState.NotStarted,
        data = (0..3).map {
            QuizResultListing(
                id = ObjectId(UUID.randomUUID().toString()),
                username = "Username$it",
                quizTitle = "The Quiz $it",
                createdBy = "Quizcreator$it",
                score = Random.nextFloat()
            )
        }
    )
    QuizAppTheme {
        Surface {
            QuizResultListScreen(
                uiState = uiState,
                navigateToDetails = { _, _ -> },
                maxWidthDp = LocalConfiguration.current.screenWidthDp.dp
            )
        }
    }
}

@Preview
@Composable
private fun QuizResultListForQuizPreview() {
    val uiState = QuizResultListUiState.ListForQuiz(
        loading = LoadingState.NotStarted,
        data = (0..3).map {
            QuizResultListing(
                id = ObjectId(UUID.randomUUID().toString()),
                username = "Username$it",
                quizTitle = "The Quiz $it",
                createdBy = "Quizcreator$it",
                score = Random.nextFloat()
            )
        },
        "The Quiz"
    )
    QuizAppTheme {
        Surface {
            QuizResultListScreen(
                uiState = uiState,
                navigateToDetails = { _, _ -> },
                maxWidthDp = LocalConfiguration.current.screenWidthDp.dp
            )
        }
    }
}
