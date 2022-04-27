package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

/**
 * Renders a form for a user to respond to a quiz. The given responses should be
 * of the appropriate size, and each [FormResponseState] should be the appropriate type
 * for its matching question of the [QuizForm]. If either constraint is not met at any given moment,
 * [IllegalArgumentException] will be thrown.
 *
 * @param uiState The required state for the UI, including the [QuizForm].
 *
 * @param onSubmit Callback invoked when the user taps the 'submit' button.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuizFormScreen(
    uiState: QuizFormUiState.Form,
    onSubmit: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    val quiz = uiState.quiz
    val responses = uiState.responses

    require(quiz.questions.size == responses.size) {
        "There must be as many questions as responses (questions.size: ${quiz.questions.size}, responses.size: ${responses.size}"
    }

    AppScaffold(
        scaffoldState = scaffoldState,
        floatingActionButton = {
            QuizFormFab(isInProgress = uiState.uploadStatus is LoadingState.InProgress, onSubmit)
        }
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .padding(it)
                    .width(maxWidthDp),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            })
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item {
                        QuizHeader(quiz.title, quiz.createdBy, quiz.questions.size)
                    }

                    itemsIndexed(quiz.questions) { index, question ->
                        QuestionForm(
                            index,
                            question,
                            responses[index],
                        )
                    }

                    // Add some extra space so that user can scroll to adjust in case FAB covers content
                    // even with padding
                    item {
                        Spacer(
                            Modifier
                                .height(216.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QuizFormFab(
    isInProgress: Boolean,
    onSubmit: () -> Unit
) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    // Use dialog to prevent accidental upload
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogOpen = false
                    onSubmit()
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            },
            text = { Text("Submit responses? You will not be able to change them later.") },
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    FABWithProgress(
        onClick = {
            focusManager.clearFocus()
            keyboardController?.hide()
            dialogOpen = true
        },
        isInProgress = isInProgress,
        progressIndicator = {
            SmallCircularProgressIndicator(
                Modifier.semantics { contentDescription = "Uploading responses" },
                color = MaterialTheme.colors.onSecondary
            )
        }
    ) {
        Icon(painter = painterResource(R.drawable.ic_publish_24), "Submit responses")
    }
}

@Preview
@Composable
private fun QuizFormScreenPreview() {
    val form = QuizForm(
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
    )
    val responses = form.questions.map {
        when (it) {
            is Question.FillIn -> PreviewFillInFormState(
                QuestionResponse.FillIn("answer text"),
                error = "Some error",
                answer = TextFieldState()
            )
            is Question.MultipleChoice -> PreviewMultipleChoiceFormState(
                QuestionResponse.MultipleChoice(
                    1
                ), error = "Some error", choice = 0
            )
            else -> throw IllegalStateException()
        }
    }
    val uiState = QuizFormUiState.Form(
        loading = LoadingState.NotStarted,
        quiz = form,
        responses = responses,
        uploadStatus = LoadingState.NotStarted
    )
    QuizAppTheme {
        Surface {
            QuizFormScreen(
                uiState = uiState,
                onSubmit = {},
                maxWidthDp = LocalConfiguration.current.screenWidthDp.dp
            )
        }
    }
}

@Composable
private fun QuizHeader(title: String, createdBy: String, questionCount: Int) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.h5)
        Text(
            "by $createdBy",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text("$questionCount questions", style = MaterialTheme.typography.subtitle2)
    }
}

@Composable
private fun QuestionForm(
    index: Int,
    question: Question,
    responseState: FormResponseState,
) {
    QuestionCard {
        Text(
            "Question ${index + 1}.",
            style = MaterialTheme.typography.caption
        )
        Text(
            question.text,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Divider(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            thickness = Dp.Hairline
        )
        responseState.error?.let {
            Text(
                it,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        when (question) {
            is Question.Empty -> throw IllegalArgumentException("Cannot use Question.Empty in QuestionForm")
            is Question.FillIn -> {
                require(responseState is FormResponseState.FillIn) {
                    "Response type must be the same as Question type (FillIn)"
                }
                FillInQuestionForm(
                    index,
                    responseState
                )
            }
            is Question.MultipleChoice -> {
                require(responseState is FormResponseState.MultipleChoice) {
                    "Response type must be the same as Question type (MultipleChoice)"
                }
                MultipleChoiceQuestionForm(
                    index,
                    question.answers,
                    responseState
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MultipleChoiceQuestionForm(
    questionIndex: Int,
    answers: List<Question.MultipleChoice.Answer>,
    responseState: FormResponseState.MultipleChoice
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(Modifier.selectableGroup()) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                "Tap an answer to mark it as your choice",
                style = MaterialTheme.typography.caption
            )
        }
        answers.forEachIndexed { index, answer ->
            // Row that looks like: (Radio) 1. answer text
            Row(
                Modifier
                    .testTag("Select answer ${index + 1} for question ${questionIndex + 1}")
                    .selectable(
                        selected = responseState.choice == index,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            responseState.choice = index
                        },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 12.dp)
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
            ) {
                RadioButton(
                    selected = responseState.choice == index,
                    onClick = null,
                    modifier = Modifier
                        .alignBy(FirstBaseline)
                        .padding(horizontal = 12.dp)
                )
                Text(
                    "${index + 1}. ${answer.text}",
                    modifier = Modifier
                        .weight(1.0f)
                        .alignBy(FirstBaseline)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FillInQuestionForm(
    questionIndex: Int,
    responseState: FormResponseState.FillIn,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    AppTextField(
        state = responseState.answer,
        onTextChange = { responseState.changeAnswer(it) },
        label = "Answer",
        modifier = Modifier
            .testTag("Fill in answer for question ${questionIndex + 1}")
            .fillMaxWidth(),
        containerModifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
}


@Composable
private fun QuestionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier, elevation = 1.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}