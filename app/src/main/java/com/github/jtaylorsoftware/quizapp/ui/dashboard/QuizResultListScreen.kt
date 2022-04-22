package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.jtaylorsoftware.quizapp.data.domain.models.*

/**
 * Displays the signed-in user's list of results for their created quizzes, in [QuizResultListing] format.
 *
 * @param navigateToDetails Callback invoked when the user presses on a listing.
 * It accepts the id of the quiz and id of the user for the selected listing.
 */
@Composable
fun QuizResultListScreen(
    uiState: QuizResultListUiState.ListForUser,
    navigateToDetails: (ObjectId, ObjectId) -> Unit
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Text("Your Quiz Results")
            Text("Tap a Result to view graded questions")
        }

        items(uiState.data, key = { it.id.value }) { result ->
            ResultListItemForCurrentUser(
                result,
                navigateToDetails = { navigateToDetails(result.quiz, result.user) })
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
    navigateToDetails: (ObjectId, ObjectId) -> Unit
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Text("Results for quiz \"${uiState.quizTitle}\":")
            Text("Tap a Result to view graded questions")
        }

        items(uiState.data, key = { it.id.value }) { result ->
            ResultListItemForQuiz(
                result,
                navigateToDetails = { navigateToDetails(result.quiz, result.user) })
        }
    }
}

/**
 * Displays the data for one [QuizResultListing] to a [Quiz] created by the signed-in user,
 * and a button to view the full details of the result.
 *
 * @param result The listing to display.
 * @param navigateToDetails Callback invoked to navigate to the details of [result].
 */
@Composable
private fun ResultListItemForCurrentUser(result: QuizResultListing, navigateToDetails: () -> Unit) {
    Box(modifier = Modifier.clickable { navigateToDetails() }) {
        Column {
            Text("\"${result.quizTitle}\"")
            Text("by ${result.createdBy}")
            Text("Score: ${"%.2f".format(result.score * 100)}%")
        }
    }
}

/**
 * Displays the data for one [QuizResultListing] by a user other than the signed-in user (because
 * the Quiz that the QuizResult belongs to is one created by the signed-in user).
 *
 * @param result The listing to display.
 * @param navigateToDetails Callback invoked to navigate to the details of [result].
 */
@Composable
private fun ResultListItemForQuiz(result: QuizResultListing, navigateToDetails: () -> Unit) {
    Box(modifier = Modifier.clickable { navigateToDetails() }) {
        Column {
            Text("Results for ${result.username}:")
            Text("Score: ${"%.2f".format(result.score * 100)}%")
        }
    }
}

/**
 * Displays all the [graded answers][GradedAnswer] and overall score for the user's
 * response to a Question. It uses a [QuizForm] to supply the context for the user's [result].
 *
 * @param result The user's graded answers and score for their responses to a Quiz.
 * @param form The Quiz, [as a form][QuizForm], to use to supply context, such as [Question] text.
 */
@Composable
fun ResultDetail(result: QuizResult, form: QuizForm) {
    LazyColumn {
        item {
            Text("${result.username}'s results for \"${result.quizTitle}\"")
            Text("by ${result.createdBy}")
            Text("Overall score: ${"%.2f".format(result.score * 100)}%")
            Text("Graded questions:")
        }

        itemsIndexed(form.questions) { index, question ->
            GradedQuestion(index, question, result.answers[index])
        }
    }
}

/**
 * Displays one [Question] with its graded answer.
 *
 * @param index Index of the [Question].
 * @param question The [Question] to display. It should be the same type as [gradedAnswer].
 * @param gradedAnswer The [user's graded answer to the Question][GradedAnswer].
 */
@Composable
private fun GradedQuestion(index: Int, question: Question, gradedAnswer: GradedAnswer) {
    when (question) {
        is Question.FillIn -> {
            check(gradedAnswer is GradedAnswer.FillIn)
            GradedFillInQuestion(index, question, gradedAnswer)
        }
        is Question.MultipleChoice -> {
            check(gradedAnswer is GradedAnswer.MultipleChoice)
            GradedMultipleChoiceQuestion(index, question, gradedAnswer)
        }
        else -> throw IllegalArgumentException("Cannot display graded Empty Question")
    }
}

/**
 * Displays one [Fill-in-the-Blank Question][Question.FillIn] with its graded answer.
 *
 * @param index Index of the [Question].
 * @param question The [fill-in Question][Question.FillIn] to display.
 * @param gradedAnswer The [user's graded answer to the Question][GradedAnswer.FillIn].
 */
@Composable
private fun GradedFillInQuestion(
    index: Int,
    question: Question.FillIn,
    gradedAnswer: GradedAnswer.FillIn
) {
    Box {
        Column {
            Text("${index + 1}. ${question.text}")
            Row {
                if (gradedAnswer.isCorrect) {
                    Icon(Icons.Default.Check, "Correct answer")
                } else {
                    Icon(Icons.Default.Close, "Incorrect answer")
                }
                Text("Your answer: ${gradedAnswer.answer}")
            }
        }
    }
}

/**
 * Displays one [Multiple Choice Question][Question.MultipleChoice] with its graded answers.
 *
 * @param index Index of the [Question].
 * @param question The [multiple choice Question][Question.MultipleChoice] to display. It should have
 *                 the correct number of answer choices for the given [gradedAnswer].
 * @param gradedAnswer The [user's graded answer to the Question][GradedAnswer.MultipleChoice].
 */
@Composable
private fun GradedMultipleChoiceQuestion(
    index: Int,
    question: Question.MultipleChoice,
    gradedAnswer: GradedAnswer.MultipleChoice
) {
    Box {
        Column {
            Text("${index + 1}. ${question.text}")
            question.answers.forEachIndexed { index, answer ->
                GradedMultipleChoiceAnswer(
                    index,
                    answer.text,
                    gradedAnswer.choice == index,
                    gradedAnswer.isCorrect && index == gradedAnswer.choice
                )
            }
        }
    }
}

/**
 * Displays the text of a MultipleChoice [Question] answer with an Icon indicating if the user's choice is correct or incorrect.
 *
 * @param index Index of the answer.
 * @param text Text of the answer.
 * @param isChoice Flag indicating if this answer is the one the user had picked.
 * @param isCorrect Flag indicating if this answer is the correct one for the question.
 */
@Composable
private fun GradedMultipleChoiceAnswer(
    index: Int,
    text: String,
    isChoice: Boolean,
    isCorrect: Boolean,
) {
    Row {
        if (isCorrect) {
            Icon(Icons.Default.Check, "Correct answer")
        } else if (isChoice) {
            Icon(Icons.Default.Close, "Incorrect answer")
        }
        Text("${index + 1}. $text")
    }
}
