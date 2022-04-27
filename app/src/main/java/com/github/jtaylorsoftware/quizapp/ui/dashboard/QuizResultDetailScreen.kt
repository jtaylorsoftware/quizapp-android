package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.data.domain.models.GradedAnswer
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.ui.theme.correct
import kotlin.random.Random


/**
 * Displays all the [graded answers][GradedAnswer] and overall score for the user's
 * response to a Question. It uses a [QuizForm] to supply the context for the user's [result].
 *
 * @param result The user's graded answers and score for their responses to a Quiz.
 * @param form The Quiz, [as a form][QuizForm], to use to supply context, such as [Question] text.
 */
@Composable
fun QuizResultDetailScreen(
    result: QuizResult,
    form: QuizForm,
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    ProfileList(
        header = {
            Box(Modifier.width(maxWidthDp)) {
                ResultDetailHeader(result)
            }
        },
    ) {
        itemsIndexed(form.questions) { index, question ->
            ProfileListCard(Modifier.width(maxWidthDp)) {
                GradedQuestion(index, question, result.answers[index])
            }
        }
    }
}

@Composable
private fun ResultDetailHeader(result: QuizResult) {
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
    ) {
        Column(Modifier.weight(1.0f)) {
            Text(
                "${result.username}'s results",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.h5
            )
            Text(
                "for \"${result.quizTitle}\"",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.h5
            )
            Text(
                "by ${result.createdBy}",
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.h6
            )
        }
        Box(
            Modifier
                .padding(8.dp)
                .fillMaxHeight(), contentAlignment = Alignment.Center
        ) {
            ScoreRingChart(score = result.score)
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
    QuestionContent(index = index, prompt = question.text) {
        AnswerRow {
            Text(
                gradedAnswer.answer,
                modifier = Modifier
                    .weight(0.9f)
                    .alignBy(FirstBaseline)
            )
            AnswerGrade(isCorrect = gradedAnswer.isCorrect, isChoice = !gradedAnswer.isCorrect)
        }
    }
}

/**
 * Displays one [Multiple Choice Question][Question.MultipleChoice] with its graded answers.
 *
 * @param index Index of the [Question].
 *
 * @param question The [multiple choice Question][Question.MultipleChoice] to display. It should have
 * the correct number of answer choices for the given [gradedAnswer].
 *
 * @param gradedAnswer The user's [graded answer][GradedAnswer.MultipleChoice] to the Question.
 */
@Composable
private fun GradedMultipleChoiceQuestion(
    index: Int,
    question: Question.MultipleChoice,
    gradedAnswer: GradedAnswer.MultipleChoice
) {
    QuestionContent(index = index, prompt = question.text) {
        question.answers.forEachIndexed { answerIndex, answer ->
            GradedMultipleChoiceAnswer(
                answerIndex,
                answer.text,
                gradedAnswer.choice == answerIndex,
                gradedAnswer.isCorrect && answerIndex == gradedAnswer.choice
            )
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
    AnswerRow {
        Text(
            "${index + 1}.",
            modifier = Modifier
                .weight(0.1f)
                .alignBy(FirstBaseline)
        )
        Text(
            text,
            modifier = Modifier
                .weight(0.8f)
                .alignBy(FirstBaseline)
        )
        AnswerGrade(isCorrect = isCorrect, isChoice = isChoice)
    }
}

/**
 * Structures the layout of a question.
 */
@Composable
private fun QuestionContent(index: Int, prompt: String, answerContent: @Composable () -> Unit) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
    )
    Text(
        "Question ${index + 1}.",
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onBackground
    )
    Text(prompt, style = MaterialTheme.typography.h6)
    Divider(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        thickness = Dp.Hairline
    )
    answerContent()
}

@Composable
private fun RowScope.AnswerGrade(isCorrect: Boolean, isChoice: Boolean) {
    Box(
        Modifier
            .padding(bottom = 4.dp)
            .weight(0.1f)
            .alignBy(FirstBaseline)
    ) {
        if (isCorrect) {
            Icon(Icons.Default.Check, "Correct answer", tint = MaterialTheme.colors.correct)
        } else if (isChoice) {
            Icon(Icons.Default.Close, "Incorrect answer", tint = MaterialTheme.colors.error)
        }
    }
}

@Composable
private fun AnswerRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}

@Preview(widthDp = 400)
@Composable
private fun QuizResultDetailPreview() {
    val score = Random.nextFloat()
    QuizAppTheme {
        Surface {
            QuizResultDetailScreen(
                result = QuizResult(
                    username = "Username",
                    quizTitle = "The Quiz ".repeat(3),
                    createdBy = "Quizcreator",
                    score = score,
                    answers = listOf(
                        GradedAnswer.FillIn(isCorrect = true, "My 1st answer"),
                        GradedAnswer.FillIn(isCorrect = false, "My 2nd answer"),
                        GradedAnswer.MultipleChoice(
                            isCorrect = true,
                            choice = 1,
                            correctAnswer = 0
                        ),
                        GradedAnswer.MultipleChoice(
                            isCorrect = false,
                            choice = 0,
                            correctAnswer = 1
                        )
                    )
                ),
                form = QuizForm(
                    createdBy = "Quizcreator",
                    title = "The Quiz",
                    questions = listOf(
                        Question.FillIn("First question ".repeat(3)),
                        Question.FillIn("Second question"),
                        Question.MultipleChoice(
                            "Third question", answers = listOf(
                                Question.MultipleChoice.Answer("Answer 1"),
                                Question.MultipleChoice.Answer("Answer 2")
                            )
                        ),
                        Question.MultipleChoice(
                            "Fourth question ".repeat(3), answers = listOf(
                                Question.MultipleChoice.Answer("Answer 1 ".repeat(5)),
                                Question.MultipleChoice.Answer("Answer 2")
                            )
                        ),
                    )
                ),
                maxWidthDp = LocalConfiguration.current.screenWidthDp.dp
            )
        }
    }
}