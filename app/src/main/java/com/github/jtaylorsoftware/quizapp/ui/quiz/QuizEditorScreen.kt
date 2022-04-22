package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays a form for a user to create a new Quiz or edit an existing Quiz. The user will be
 * unable to change certain fields, including the number of questions, if they are editing.
 *
 * @param uiState The data for the Quiz including upload status.
 *
 * @param onSubmit Called when submitting the Quiz.
 */
@Composable
fun QuizEditorScreen(
    uiState: QuizEditorUiState.Editor,
    onSubmit: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
) {
    val listState = rememberLazyListState()

    AppScaffold(
        modifier = Modifier.testTag("QuizEditorScreen"),
        uiState = uiState,
        scaffoldState = scaffoldState,
        floatingActionButton = {
            QuizEditorFABs(
                uiState = uiState,
                onSubmit = onSubmit,
                listState = listState
            )
        }
    ) {
        QuizEditor(
            quizState = uiState.quizState,
            isEditing = uiState.isEditing,
            listState = listState
        )
    }
}

/**
 * Displays the FAB(s) necessary for the editor. The upload button is always displayed, but
 * the "add question" button is only shown when creating a new quiz.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QuizEditorFABs(
    uiState: QuizEditorUiState.Editor,
    onSubmit: () -> Unit,
    listState: LazyListState,
) {
    val coroutineScope = rememberCoroutineScope()

    val isUploading = remember(uiState) { uiState.uploadStatus is LoadingState.InProgress }
    val uploadIconAlpha by derivedStateOf { if (isUploading) 0.0f else 1.0f }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(Modifier
        .pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        }
    ) {
        FABWithProgress(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onSubmit()
            },
            isInProgress = isUploading,
            progressIndicator = {
                SmallCircularProgressIndicator(
                    Modifier.semantics { contentDescription = "Uploading quiz" },
                    color = MaterialTheme.colors.onSecondary
                )
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_publish_24),
                "Upload quiz",
                modifier = Modifier.alpha(uploadIconAlpha),
                tint = MaterialTheme.colors.onSecondary
            )
        }
        if (!uiState.isEditing) {
            Spacer(Modifier.requiredHeight(8.dp))
            FloatingActionButton(onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                coroutineScope.launch {
                    listState.scrollToItem(listState.layoutInfo.totalItemsCount)
                }
                uiState.quizState.addQuestion()
            }) {
                Icon(
                    Icons.Default.Add,
                    "Add question",
                    tint = MaterialTheme.colors.onSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QuizEditor(
    quizState: QuizState,
    isEditing: Boolean,
    listState: LazyListState = rememberLazyListState(),
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(state = listState, modifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        }
        .fillMaxWidth()
    ) {
        // Basic Quiz data
        item {
            QuizHeader(quizState = quizState)
        }

        item {
            Text("Questions:")
        }

        // List of editable questions
        itemsIndexed(quizState.questions, key = { _, q -> q.key }) { index, questionState ->
            // Display the index and delete button above the question content
            Row {
                Text("Question ${index + 1}:")
                if (!isEditing) {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        quizState.deleteQuestion(index)
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete question",
                            tint = MaterialTheme.colors.error
                        )
                    }
                }
            }
            // Display question content
            Card {
                QuestionEditor(
                    questionState,
                    onChangeQuestionType = {
                        quizState.changeQuestionType(index, it)
                    },
                    isEditing = isEditing
                )
            }
        }
    }
}

@Preview(group = "Quiz")
@Composable
private fun QuizEditorPreview() {
    val questions = listOf(
        Question.MultipleChoice(
            text = "Question Prompt",
            correctAnswer = 1,
            answers = listOf(
                Question.MultipleChoice.Answer("Answer text 1"),
                Question.MultipleChoice.Answer("Answer text 2")
            ),
        ),
        Question.FillIn(text = "Fill In Question", "Correct answer"),
        Question.Empty,
    )
    val questionState = questions.map {
        when (it) {
            is Question.MultipleChoice -> PreviewMultipleChoiceState(data = it)
            is Question.FillIn -> PreviewFillInState(data = it)
            else -> PreviewEmptyState()
        }
    }

    QuizAppTheme {
        Surface {
            QuizEditor(
                quizState = PreviewQuizState(
                    title = TextFieldState("My Quiz"),
                    isPublic = false,
                    questions = questionState,
                ),
                isEditing = false,
            )
        }
    }
}

/**
 * Displays editable fields for basic Quiz data, including title, allowedUsers, and expiration.
 */
@Composable
private fun QuizHeader(quizState: QuizState) {
    Card {
        Column {
            QuizTitle(
                title = quizState.title,
                onTitleChange = {
                    quizState.changeTitleText(it)
                }
            )
            AllowedUsers(
                isPublic = quizState.isPublic,
                onChangeIsPublic = { quizState.isPublic = it },
                allowedUsers = quizState.allowedUsers,
                onChangeAllowedUsers = { quizState.allowedUsers = it },
                allowedUsersError = quizState.allowedUsersError
            )
            Expiration(
                expiration = quizState.expiration,
                changeExpiration = { quizState.expiration = it },
                expirationError = quizState.expirationError
            )
        }
    }
}

/**
 * Renders editable Quiz title field.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QuizTitle(title: TextFieldState, onTitleChange: (String) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = title.text,
        onValueChange = onTitleChange,
        isError = title.error != null,
        label = {
            Text("Quiz Title")
        },
        modifier = Modifier.semantics {
            contentDescription = "Edit quiz title"
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    title.error?.let {
        Text(it, modifier = Modifier.semantics {
            contentDescription = "Quiz title hint"
        })
    }
}

/**
 * Shows editable allowedUsers text field.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AllowedUsers(
    isPublic: Boolean,
    onChangeIsPublic: (Boolean) -> Unit,
    allowedUsers: String,
    onChangeAllowedUsers: (String) -> Unit,
    allowedUsersError: String?,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row {
        Text("Public Quiz?")
        Switch(
            checked = isPublic,
            onCheckedChange = onChangeIsPublic,
            modifier = Modifier.semantics {
                contentDescription = "Toggle public quiz"
            })
    }
    if (!isPublic) {
        TextField(
            value = allowedUsers,
            onValueChange = onChangeAllowedUsers,
            label = {
                Text("Allowed Users")
            },
            modifier = Modifier.semantics {
                contentDescription = "Edit allowed users"
            },
            isError = allowedUsersError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )
        allowedUsersError?.let { error ->
            Text(error, modifier = Modifier.semantics {
                contentDescription = "Allowed users hint"
            })
        }
    }
}

/**
 * Provides onClick actions to set the expiration date and time.
 */
@Composable
private fun Expiration(
    expiration: Instant,
    changeExpiration: (Instant) -> Unit,
    expirationError: String?
) {
    val expirationDateTime: LocalDateTime =
        remember(expiration) { expiration.atZone(ZoneId.systemDefault()).toLocalDateTime() }
    val expirationDateStr: String by derivedStateOf {
        expirationDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    }
    val expirationTimeStr: String by derivedStateOf {
        expirationDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

    var datePickerOpen: Boolean by remember { mutableStateOf(false) }
    var timePickerOpen: Boolean by remember { mutableStateOf(false) }

    Text("Expiration:")
    expirationError?.let {
        Text("Error: $it", color = MaterialTheme.colors.error)
    }
    Row {
        Text("Date:")
        OutlinedButton(onClick = { datePickerOpen = true }, modifier = Modifier.semantics {
            contentDescription = "Change expiration date"
        }) {
            Text(text = expirationDateStr)
        }
    }
    Row {
        Text("Time:")
        OutlinedButton(onClick = { timePickerOpen = true }, modifier = Modifier.semantics {
            contentDescription = "Change expiration time"
        }) {
            Text(text = expirationTimeStr)
        }
    }

    AppDatePicker(
        defaultValue = expirationDateTime.toLocalDate(),
        open = datePickerOpen,
        onDismiss = {
            datePickerOpen = false
            changeExpiration(
                LocalDateTime.of(it, expirationDateTime.toLocalTime())
                    .atZone(ZoneId.systemDefault()).toInstant()
            )
        })

    AppTimePicker(value = expirationDateTime.toLocalTime(), open = timePickerOpen, onDismiss = {
        timePickerOpen = false
        changeExpiration(
            LocalDateTime.of(expirationDateTime.toLocalDate(), it).atZone(ZoneId.systemDefault())
                .toInstant()
        )
    })
}

/**
 * Provides components to edit all parts of a Question, including its type.
 */
@Composable
private fun QuestionEditor(
    questionState: QuestionState,
    onChangeQuestionType: (QuestionType) -> Unit,
    isEditing: Boolean,
) {
    Column {
        // Cannot change the type at all when editing
        if (!isEditing) {
            QuestionTypeSelector(questionState = questionState, onSelectType = onChangeQuestionType)
        }

        // Show a different body or answer editor depending on the type of question.
        when (questionState) {
            is QuestionState.Empty -> {}
            is QuestionState.MultipleChoice -> {
                MultipleChoiceQuestion(
                    question = questionState,
                    isEditing = isEditing,
                )
            }
            is QuestionState.FillIn -> {
                FillInQuestion(
                    question = questionState,
                    isEditing = isEditing
                )
            }
        }
    }
}


@Preview(showBackground = true, group = "Question")
@Composable
private fun QuestionEditorPreview() {
    QuizAppTheme {
        Column {
            Card {
                QuestionEditor(
                    questionState = PreviewEmptyState(),
                    onChangeQuestionType = {},
                    isEditing = false
                )
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Card {
                QuestionEditor(
                    questionState = PreviewFillInState(),
                    onChangeQuestionType = {},
                    isEditing = false
                )
            }
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Card {
                QuestionEditor(
                    questionState = PreviewMultipleChoiceState(),
                    onChangeQuestionType = {},
                    isEditing = false
                )
            }
        }
    }
}

/**
 * Shows IconButtons for selecting the type of the Question.
 */
@Composable
private fun QuestionTypeSelector(
    questionState: QuestionState,
    onSelectType: (QuestionType) -> Unit,
) {
    val selectedType = questionState.data.type
    Row {
        Text("Question type:")

        // Row of icons representing selected type while also giving ability to change it
        // by pressing the desired type (resets current Question)
        Row(Modifier.selectableGroup()) {
            Box(
                Modifier
                    .selectable(
                        selected = selectedType == QuestionType.FillIn,
                        onClick = { onSelectType(QuestionType.FillIn) })
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_border_color_24),
                    contentDescription = "Fill in the blank question",
                    tint = if (selectedType == QuestionType.FillIn) MaterialTheme.colors.primary else Color.DarkGray,
                )
            }

            Box(
                Modifier
                    .selectable(
                        selected = selectedType == QuestionType.MultipleChoice,
                        onClick = { onSelectType(QuestionType.MultipleChoice) })
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_format_list_numbered_24),
                    contentDescription = "Multiple choice question",
                    tint = if (selectedType == QuestionType.MultipleChoice) MaterialTheme.colors.primary else Color.DarkGray,
                )
            }

        }
    }
}

@Preview(showBackground = true, group = "Question")
@Composable
private fun QuestionTypeSelectorPreview() {
    QuizAppTheme {
        Column {
            QuestionTypeSelector(questionState = PreviewEmptyState(), onSelectType = {})
            Spacer(Modifier.fillMaxWidth())
            QuestionTypeSelector(questionState = PreviewFillInState(), onSelectType = {})
            Spacer(Modifier.fillMaxWidth())
            QuestionTypeSelector(questionState = PreviewMultipleChoiceState(), onSelectType = {})
        }
    }
}

/**
 * Editor for a MultipleChoice Question. Allows adding, editing, and removing
 * individual answers.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MultipleChoiceQuestion(
    question: QuestionState.MultipleChoice,
    isEditing: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = question.prompt.text,
        onValueChange = { question.changePrompt(it) },
        label = {
            Text("Question prompt")
        },
        isError = question.prompt.error != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    question.prompt.error?.let {
        Text(it, modifier = Modifier.semantics {
            contentDescription = "Question prompt hint"
        })
    }
    Text("Tap an answer to mark it as the correct choice")
    Column(Modifier.selectableGroup()) {
        question.answers.forEachIndexed { index, answer ->
            MultipleChoiceAnswer(
                index,
                answer,
                selected = index == question.correctAnswer,
                onSelected = {
                    question.changeCorrectAnswer(index)
                },
                onDelete = {
                    question.removeAnswer(index)
                },
                isEditing = isEditing,
            )
        }
    }
    if (!isEditing) {
        OutlinedButton(onClick = question::addAnswer) {
            Text("Add answer")
        }
    }
}

@Preview(showBackground = true, group = "MultipleChoice")
@Composable
private fun MultipleChoiceQuestionPreview() {
    QuizAppTheme {
        Column {
            MultipleChoiceQuestion(
                question = PreviewMultipleChoiceState(
                    error = "Text error",
                    correctAnswerError = "Select a correct answer",
                    data = Question.MultipleChoice(
                        text = "Question Prompt",
                        correctAnswer = 1,
                        answers = listOf(
                            Question.MultipleChoice.Answer("Answer text 1"),
                            Question.MultipleChoice.Answer("Answer text 2")
                        )
                    )
                ),
                isEditing = false
            )
        }
    }
}

/**
 * A single answer/choice for a MultipleChoice Question.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MultipleChoiceAnswer(
    index: Int,
    answer: QuestionState.MultipleChoice.Answer,
    selected: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit,
    isEditing: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row {
        // Row that looks like: (Radio) 1. answer text [Delete]
        RadioButton(selected = selected, onClick = onSelected, enabled = !isEditing,
            modifier = Modifier.semantics {
                contentDescription = "Pick answer ${index + 1}"
            })
        Column {
            TextField(
                value = answer.text.text,
                onValueChange = { answer.changeText(it) },
                label = {
                    Text("Answer text")
                },
                modifier = Modifier
                    .semantics {
                        contentDescription = "Edit answer ${index + 1} text"
                    },
                isError = answer.text.error != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            answer.text.error?.let {
                Text(it, modifier = Modifier.semantics {
                    contentDescription = "Answer ${index + 1} hint"
                })
            }
        }
        if (!isEditing) {
            IconButton(onClick = onDelete, modifier = Modifier.semantics {
                contentDescription = "Delete answer ${index + 1}"
            }) {
                Icon(Icons.Default.Delete, null)
            }
        }
    }
}

@Preview(showBackground = true, group = "MultipleChoice")
@Composable
private fun MultipleChoiceAnswerPreview() {
    QuizAppTheme {
        MultipleChoiceAnswer(
            index = 1,
            answer = PreviewMultipleChoiceState.PreviewAnswerHolder(
                TextFieldState(
                    text = "Answer text",
                    error = "Answer error"
                )
            ),
            selected = true,
            onSelected = {},
            onDelete = {},
            isEditing = false
        )
    }
}

/**
 * Provides an editable prompt and correctAnswer for a Fill in the blank Question.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FillInQuestion(
    question: QuestionState.FillIn,
    isEditing: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = question.prompt.text,
        onValueChange = { question.changePrompt(it) },
        label = {
            Text("Question prompt")
        },
        isError = question.prompt.error != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    question.prompt.error?.let {
        Text(it, modifier = Modifier.semantics {
            contentDescription = "Question prompt hint"
        })
    }

    TextField(
        value = question.correctAnswer.text,
        onValueChange = { question.changeCorrectAnswer(it) },
        label = {
            Text("Correct answer")
        },
        modifier = Modifier
            .semantics {
                contentDescription = "Change correct answer text"
            }, enabled = !isEditing,
        isError = question.correctAnswer.error != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    question.correctAnswer.error?.let {
        Text(it, modifier = Modifier.semantics {
            contentDescription = "Fill-in answer hint"
        })
    }
}

@Preview(showBackground = true, group = "FillIn")
@Composable
private fun FillInQuestionPreview() {
    QuizAppTheme {
        Column {
            FillInQuestion(
                question = PreviewFillInState(
                    error = "Text error",
                    correctAnswerError = "Correct answer error",
                    data = Question.FillIn("Fill In", "Correct Answer")
                ),
                isEditing = false
            )
        }
    }
}