package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm

/**
 * Renders a form for a user to respond to a quiz. The given [responses] should be
 * of the appropriate size, and each [ResponseState] should be the appropriate type
 * for its matching question of [quiz]. If either constraint is not met, [IllegalArgumentException]
 * will be thrown, potentially in the middle of rendering questions if the size constraint is met
 * but not the type constraint.
 *
 * @param quiz The quiz to respond to. Provides the question text and possible answers.
 * @param responses The pre-allocated list of responses to each question. The chosen answers
 *                  should be updated by the user's actions.
 * @param onChangeResponse Callback invoked when the user changes their answer for a response to
 *                         a single question.
 * @param onSubmit Callback invoked when the user taps the 'submit' button.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuizFormScreen(
    quiz: QuizForm,
    responses: List<ResponseState>,
    onChangeResponse: (Int, ResponseState) -> Unit,
    onSubmit: () -> Unit,
) {
    require(quiz.questions.size == responses.size) {
        "There must be as many questions as responses (questions.size: ${quiz.questions.size}, responses.size: ${responses.size}"
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onSubmit, modifier = Modifier.semantics {
                contentDescription = "Submit responses"
            }) {
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                FloatingActionButton(onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSubmit()
                }) {
                    Icon(painter = painterResource(R.drawable.ic_publish_24), "Upload quiz")
                }
            }
        }
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        LazyColumn(modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .fillMaxWidth()
        ) {
            item {
                QuizHeader(quiz.title, quiz.createdBy, quiz.questions.size)
            }

            itemsIndexed(quiz.questions) { index, question ->
                Card {
                    QuestionForm(
                        index,
                        question,
                        responses[index],
                        onChangeResponse = onChangeResponse
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizHeader(title: String, createdBy: String, questionCount: Int) {
    Card {
        Column {
            Text("\"$title\"")
            Text("by $createdBy")
            Text("$questionCount questions")
        }
    }
}

@Composable
private fun QuestionForm(
    index: Int,
    question: Question,
    responseState: ResponseState,
    onChangeResponse: (Int, ResponseState) -> Unit
) {
    when (question) {
        is Question.Empty -> throw IllegalArgumentException("Cannot use Question.Empty in QuestionForm")
        is Question.FillIn -> {
            require(responseState.response is QuestionResponse.FillIn) {
                "Response type must be the same as Question type (FillIn)"
            }
            FillInQuestionForm(
                index,
                "${index + 1}. ${question.text}:",
                responseState.response.answer,
                responseState.error,
                onChangeAnswer = {
                    onChangeResponse(
                        index,
                        responseState.copy(response = responseState.response.copy(answer = it))
                    )
                }
            )
        }
        is Question.MultipleChoice -> {
            require(responseState.response is QuestionResponse.MultipleChoice) {
                "Response type must be the same as Question type (MultipleChoice)"
            }
            MultipleChoiceQuestionForm(
                index,
                "${index + 1}. ${question.text}:",
                question.answers,
                responseState.response.choice,
                responseState.error,
                onChangeChoice = {
                    onChangeResponse(
                        index,
                        responseState.copy(response = responseState.response.copy(choice = it))
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MultipleChoiceQuestionForm(
    questionIndex: Int,
    prompt: String,
    answers: List<Question.MultipleChoice.Answer>,
    choice: Int,
    error: String?,
    onChangeChoice: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(Modifier.selectableGroup()) {
        Text(prompt)
        Text("Tap an answer to mark it as your choice")
        error?.let {
            Text(error, color = MaterialTheme.colors.error)
        }
        answers.forEachIndexed { index, answer ->
            // Row that looks like: (Radio) 1. answer text
            Row(
                Modifier
                    .testTag("Select answer ${index + 1} for question ${questionIndex + 1}")
                    .selectable(
                        selected = choice == index,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onChangeChoice(index)
                        }
                    )
            ) {
                RadioButton(
                    selected = choice == index,
                    onClick = null
                )
                Text("${index + 1}. ${answer.text}")
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FillInQuestionForm(
    questionIndex: Int,
    prompt: String,
    answer: String,
    error: String?,
    onChangeAnswer: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column {
        Text(prompt)
        error?.let {
            Text(error, color = MaterialTheme.colors.error)
        }
        TextField(
            value = answer,
            onValueChange = onChangeAnswer,
            label = {
                Text("Answer")
            },
            modifier = Modifier.testTag("Fill in answer for question ${questionIndex + 1}"),
            isError = error != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )
    }
}