package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.github.jtaylorsoftware.quizapp.data.*

@Composable
fun ResultScreen(results: List<QuizResultListing>, navigateToDetails: (String) -> Unit) {
    LazyColumn {
        item {
            Text("Your Quiz Results")
            Text("Tap a Result to view graded questions")
        }

        items(results, key = { it.id }) { result ->
            ResultListItem(result, navigateToDetails)
        }
    }
}

@Composable
private fun ResultListItem(result: QuizResultListing, navigateToDetails: (String) -> Unit) {
    Box(modifier = Modifier.clickable { navigateToDetails(result.id) }) {
        Column {
            Text("\"${result.quizTitle}\"")
            Text("by ${result.createdBy}")
            Text("Score: ${"%.2f".format(result.score * 100)}%")
        }
    }
}

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
    }
}

@Composable
private fun GradedFillInQuestion(
    index: Int,
    question: Question.FillIn,
    gradedAnswer: GradedAnswer.FillIn
) {
    Box {
        Column {
            Text("$index. ${question.text}")
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

@Composable
private fun GradedMultipleChoiceQuestion(
    index: Int,
    question: Question.MultipleChoice,
    gradedAnswer: GradedAnswer.MultipleChoice
) {
    Box {
        Column {
            Text("$index. ${question.text}")
            question.answers.forEachIndexed { index, answer ->
                GradedMultipleChoiceAnswer(
                    index,
                    answer,
                    gradedAnswer.choice == index,
                    gradedAnswer.isCorrect && index == gradedAnswer.choice
                )
            }
        }
    }
}

@Composable
private fun GradedMultipleChoiceAnswer(
    index: Int,
    answer: Question.MultipleChoice.Answer,
    isChoice: Boolean,
    isCorrect: Boolean,
) {

}
