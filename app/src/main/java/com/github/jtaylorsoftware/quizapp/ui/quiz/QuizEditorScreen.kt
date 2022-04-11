package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.ui.components.AppDatePicker
import com.github.jtaylorsoftware.quizapp.ui.components.AppTimePicker
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays a form for a user to create or edit a [Quiz]. The mode
 * (creating or editing) is controlled by the [isEditing] flag, and the
 * displayed content will vary accordingly. Notably, some fields will be
 * immutable in editing mode.
 *
 * @param quizState The basic data for a Quiz, such as the expiration and title.
 * @param onSubmit Called when submitting the Quiz.
 * @param isEditing Flag controlling mutability of the current Quiz.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuizEditorScreen(
    quizState: QuizState,
    onSubmit: () -> Unit,
    isEditing: Boolean = false,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
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
                FloatingActionButton(onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSubmit()
                }) {
                    Icon(painter = painterResource(R.drawable.ic_publish_24), "Upload quiz")
                }
                if (!isEditing) {
                    Spacer(Modifier.requiredHeight(8.dp))
                    FloatingActionButton(onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        coroutineScope.launch {
                            listState.scrollToItem(listState.layoutInfo.totalItemsCount)
                        }
                        quizState.addQuestion()
                    }) {
                        Icon(Icons.Default.Add, "Add question")
                    }
                }
            }
        }
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
                // Display the index and delete button separately for clarity
                Row {
                    Text("Question ${index + 1}:")
                    if (!isEditing) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            quizState.deleteQuestion(index)
                        }) {
                            Icon(Icons.Default.Delete, "Delete question")
                        }
                    }
                }
                Card {
                    QuestionEditor(
                        questionState,
                        onChangeQuestionType = {
                            quizState.changeQuestionType(index, it)
                        },
                        onEditQuestion = {
                            quizState.changeQuestion(index, it)
                        },
                        isEditing = isEditing
                    )
                }
            }
        }
    }
}

@Preview(group = "Quiz")
@Composable
fun QuizEditorScreenPreview() {
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
            is Question.MultipleChoice -> QuestionState.MultipleChoice(
                data = it,
                answerErrors = listOf(null, null)
            )
            is Question.FillIn -> QuestionState.FillIn(data = it)
            else -> QuestionState.Empty()
        }
    }

    QuizAppTheme {
        QuizEditorScreen(
            quizState = PreviewQuizState(title = TextFieldState("My Quiz"), isPublic = false),
            onSubmit = {}
        )
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
                    quizState.setTitle(it)
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
    onEditQuestion: (QuestionState) -> Unit,
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
                    question = questionState.data,
                    questionTextError = questionState.questionTextError,
                    answerErrors = questionState.answerErrors,
                    isEditing = isEditing,
                    onAddAnswer = { onEditQuestion(questionState.addAnswer()) },
                    onChangeAnswer = { i, answer ->
                        onEditQuestion(questionState.changeAnswer(i, answer))
                    },
                    onChangeCorrectAnswer = { onEditQuestion(questionState.changeCorrectAnswer(it)) },
                    onChangePrompt = { onEditQuestion(questionState.changeText(it)) },
                    onDeleteAnswer = { onEditQuestion(questionState.removeAnswer(it)) }
                )
            }
            is QuestionState.FillIn -> {
                FillInQuestion(
                    question = questionState.data,
                    onChangePrompt = { onEditQuestion(questionState.changeText(it)) },
                    onChangeCorrectAnswer = { onEditQuestion(questionState.changeCorrectAnswer(it)) },
                    questionTextError = questionState.questionTextError,
                    answerError = questionState.correctAnswerError,
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
                    questionState = QuestionState.Empty(),
                    onChangeQuestionType = {},
                    onEditQuestion = {},
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
                    questionState = QuestionState.FillIn(),
                    onChangeQuestionType = {},
                    onEditQuestion = {},
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
                    questionState = QuestionState.MultipleChoice(),
                    onChangeQuestionType = {},
                    onEditQuestion = {},
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
            QuestionTypeSelector(questionState = QuestionState.Empty(), onSelectType = {})
            Spacer(Modifier.fillMaxWidth())
            QuestionTypeSelector(questionState = QuestionState.FillIn(), onSelectType = {})
            Spacer(Modifier.fillMaxWidth())
            QuestionTypeSelector(questionState = QuestionState.MultipleChoice(), onSelectType = {})
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
    question: Question.MultipleChoice,
    questionTextError: String?,
    onChangePrompt: (String) -> Unit,
    answerErrors: List<String?>,
    onAddAnswer: () -> Unit,
    onChangeCorrectAnswer: (Int) -> Unit,
    onChangeAnswer: (Int, Question.MultipleChoice.Answer) -> Unit,
    onDeleteAnswer: (Int) -> Unit,
    isEditing: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = question.text,
        onValueChange = { onChangePrompt(it) },
        label = {
            Text("Question prompt")
        },
        isError = questionTextError != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    questionTextError?.let {
        Text(it, modifier = Modifier.semantics {
            contentDescription = "Question prompt hint"
        })
    }
    Text("Tap an answer to mark it as the correct choice")
    Column(Modifier.selectableGroup()) {
        question.answers.forEachIndexed { index, answer ->
            MultipleChoiceAnswer(
                index,
                answer.text,
                onChangeText = { onChangeAnswer(index, Question.MultipleChoice.Answer(it)) },
                selected = index == question.correctAnswer,
                onSelected = {
                    onChangeCorrectAnswer(index)
                },
                onDelete = {
                    onDeleteAnswer(index)
                },
                answerError = answerErrors[index],
                isEditing = isEditing,
            )
        }
    }
    if (!isEditing) {
        OutlinedButton(onClick = onAddAnswer) {
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
                question = Question.MultipleChoice(
                    text = "Question Prompt",
                    correctAnswer = 1,
                    answers = listOf(
                        Question.MultipleChoice.Answer("Answer text 1"),
                        Question.MultipleChoice.Answer("Answer text 2")
                    )
                ),
                questionTextError = "Question prompt error",
                onChangePrompt = {},
                answerErrors = listOf("Answer error 1", "Answer error 2"),
                onAddAnswer = {},
                onChangeCorrectAnswer = {},
                onChangeAnswer = { _, _ -> },
                onDeleteAnswer = {},
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
    text: String,
    onChangeText: (String) -> Unit,
    answerError: String?,
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
                value = text,
                onValueChange = { onChangeText(it) },
                label = {
                    Text("Answer text")
                },
                modifier = Modifier
                    .semantics {
                        contentDescription = "Edit answer ${index + 1} text"
                    },
                isError = answerError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            answerError?.let {
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
            text = "Answer text",
            onChangeText = {},
            answerError = "Answer error",
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
    question: Question.FillIn,
    questionTextError: String?,
    answerError: String?,
    onChangePrompt: (String) -> Unit,
    onChangeCorrectAnswer: (String) -> Unit,
    isEditing: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = question.text,
        onValueChange = { onChangePrompt(it) },
        label = {
            Text("Question prompt")
        },
        isError = questionTextError != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    questionTextError?.let {
        Text(it, modifier = Modifier.semantics {
            contentDescription = "Question prompt hint"
        })
    }

    TextField(
        value = question.correctAnswer ?: "",
        onValueChange = onChangeCorrectAnswer,
        label = {
            Text("Correct answer")
        },
        modifier = Modifier
            .semantics {
                contentDescription = "Change correct answer text"
            }, enabled = !isEditing,
        isError = answerError != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    answerError?.let {
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
                question = Question.FillIn("Fill In", "Correct Answer"),
                questionTextError = "Text error",
                answerError = "Answer error",
                onChangePrompt = {},
                onChangeCorrectAnswer = {},
                isEditing = false
            )
        }
    }
}