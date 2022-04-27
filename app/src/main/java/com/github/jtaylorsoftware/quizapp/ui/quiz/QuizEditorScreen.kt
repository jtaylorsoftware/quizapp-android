package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import java.time.LocalTime
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
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    val listState = rememberLazyListState()

    AppScaffold(
        modifier = Modifier.testTag("QuizEditorScreen"),
        scaffoldState = scaffoldState,
        floatingActionButton = {
            QuizEditorFABs(
                uiState = uiState,
                onSubmit = onSubmit,
                listState = listState
            )
        }
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .padding(it)
                    .width(maxWidthDp),
                contentAlignment = Alignment.TopCenter
            ) {
                QuizEditor(
                    quizState = uiState.quizState,
                    isEditing = uiState.isEditing,
                    listState = listState
                )
            }
        }
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

    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    // Show dialog when trying to upload quiz to prevent accidental upload
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogOpen = false
                    onSubmit()
                }) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            },
            text = { Text("Upload quiz? You won't be able to change some of the questions later.") },
        )
    }


    Column(
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            },
        horizontalAlignment = Alignment.End
    ) {
        FABWithProgress(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                dialogOpen = true
            },
            isInProgress = isUploading,
            backgroundColor = if (uiState.isEditing) MaterialTheme.colors.secondary else MaterialTheme.colors.secondaryVariant,
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
            Spacer(Modifier.height(16.dp))
            ExtendedFloatingActionButton(
                text = { Text("Add question") },
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    coroutineScope.launch {
                        listState.scrollToItem(listState.layoutInfo.totalItemsCount)
                    }
                    uiState.quizState.addQuestion()
                }, icon = {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = MaterialTheme.colors.onSecondary
                    )
                })
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

    val clearFocusOnTap: (Offset) -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .pointerInput(Unit) { detectTapGestures(onTap = clearFocusOnTap) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        quizHeader(quizState)

        questionItems(
            quizState = quizState,
            isEditing = isEditing,
            listState = listState,
        )

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

private fun LazyListScope.quizHeader(quizState: QuizState) {
    item {
        QuestionCard {
            QuizHeader(quizState = quizState)
        }
    }
}

private fun LazyListScope.questionItems(
    quizState: QuizState,
    isEditing: Boolean,
    listState: LazyListState,
) {
    // List of editable questions
    itemsIndexed(quizState.questions, key = { _, q -> q.key }) { index, questionState ->
        val scope = rememberCoroutineScope()
        val offset = with(LocalDensity.current) { 216.dp.roundToPx() }

        QuestionCard {
            QuestionItem(
                index = index,
                questionState = questionState,
                isEditing = isEditing,
                onChangeQuestionType = { quizState.changeQuestionType(index, it) },
                onDeleteQuestion = { quizState.deleteQuestion(index) },
                scrollToQuestionEnd = {
                    scope.launch {
                        // Allow questions to scroll to a little above their own size so that
                        // they can prevent themselves from partially going offscreen on size changes (such as
                        // multiple choice questions adding answers)
                        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == questionState.key }
                            ?.let { item ->
                                listState.scrollToItem(item.index, item.size - offset)
                            }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QuestionItem(
    index: Int,
    questionState: QuestionState,
    isEditing: Boolean,
    onChangeQuestionType: (QuestionType) -> Unit,
    onDeleteQuestion: () -> Unit,
    scrollToQuestionEnd: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Prevent accidentally deleting a question by using a dialog
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogOpen = false
                    onDeleteQuestion()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            },
            text = { Text("Delete question ${index + 1}?") },
        )
    }

    // Display the index and a delete button above the question content
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
    ) {
        Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            Text("Question ${index + 1}:")
        }
        Spacer(Modifier.weight(1.0f))
        if (!isEditing) {
            IconButton(onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                dialogOpen = true
            }) {
                Icon(
                    Icons.Default.Delete,
                    "Delete question",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
    questionState.error?.let {
        Text(
            it,
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }


    // Display question content
    QuestionEditor(
        questionState,
        onChangeQuestionType = onChangeQuestionType,
        isEditing = isEditing,
        scrollToQuestionEnd = scrollToQuestionEnd
    )
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
                isEditing = false
            )
        }
    }
}

/**
 * Displays editable fields for basic Quiz data, including title, allowedUsers, and expiration.
 */
@Composable
private fun QuizHeader(quizState: QuizState) {
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
    Box(
        Modifier
            .requiredHeight(32.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        quizState.questionsError?.let {
            Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
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

    AppTextField(
        state = title,
        onTextChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        containerModifier = Modifier.fillMaxWidth(),
        label = "Quiz Title",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
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
    val textFieldState =
        remember(allowedUsers) { TextFieldState(text = allowedUsers, error = allowedUsersError) }
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
    ) {
        val onClick = { onChangeIsPublic(!isPublic) }
        Box(
            Modifier
                .clickable(onClick = onClick)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Public Quiz")
        }
        Spacer(Modifier.weight(1.0f))
        Switch(
            checked = isPublic,
            onCheckedChange = { onClick() }
        )
    }
    if (!isPublic) {
        AppTextField(
            state = textFieldState,
            onTextChange = onChangeAllowedUsers,
            label = "Allowed Users",
            modifier = Modifier.fillMaxWidth(),
            containerModifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )
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


    var datePickerOpen: Boolean by rememberSaveable(key = "EXPIRATION_DATE_PICKER_OPEN") {
        mutableStateOf(
            false
        )
    }

    // Note: For some reason, this code started causing cast exceptions (Bool -> LocalDateTime)
    // on restoring state after rotations without explicit keys. Added explicit keys to
    // each call for safety, even though only `dateValue` had problems.
    var dateValue by rememberSaveable(expirationDateTime, key = "EXPIRATION_DATE_VALUE") {
        mutableStateOf(
            expirationDateTime.toLocalDate()
        )
    }

    var timePickerOpen: Boolean by rememberSaveable(key = "EXPIRATION_TIME_PICKER_OPEN") {
        mutableStateOf(
            false
        )
    }

    var timeValue: LocalTime by rememberSaveable(
        expirationDateTime,
        key = "EXPIRATION_TIME_VALUE"
    ) {
        mutableStateOf(
            expirationDateTime.toLocalTime()
        )
    }

    Text("Edit Expiration:")
    Box(
        Modifier
            .requiredHeight(32.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        expirationError?.let {
            Text(
                "Error: $it",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error
            )
        }
    }
    Row(
        Modifier
            .height(IntrinsicSize.Min),
    ) {
        val onClick = { datePickerOpen = true }
        Box(
            Modifier
                .clickable(onClick = onClick)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Date:", textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1.0f))
        OutlinedButton(onClick = onClick) {
            Text(text = expirationDateStr)
        }
    }
    Row(
        Modifier
            .height(IntrinsicSize.Min)
    ) {
        val onClick = { timePickerOpen = true }
        Box(
            Modifier
                .clickable(onClick = onClick)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Time:", textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1.0f))
        OutlinedButton(onClick = onClick) {
            Text(text = expirationTimeStr)
        }
    }

    AppDatePicker(
        value = dateValue,
        onValueChange = {
            dateValue = it
        },
        open = datePickerOpen,
        onDismiss = {
            datePickerOpen = false
            changeExpiration(
                LocalDateTime.of(dateValue, expirationDateTime.toLocalTime())
                    .atZone(ZoneId.systemDefault()).toInstant()
            )
        })

    AppTimePicker(
        value = timeValue,
        onValueChange = {
            timeValue = it
        },
        open = timePickerOpen,
        onDismiss = {
            timePickerOpen = false
            changeExpiration(
                LocalDateTime.of(expirationDateTime.toLocalDate(), timeValue)
                    .atZone(ZoneId.systemDefault())
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
    scrollToQuestionEnd: () -> Unit
) {
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
                scrollToQuestionEnd = scrollToQuestionEnd
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


@Preview(showBackground = true, group = "Question")
@Composable
private fun QuestionEditorPreview() {
    QuizAppTheme {
        Column {
            QuestionEditor(
                questionState = PreviewEmptyState(),
                onChangeQuestionType = {},
                isEditing = false,
                scrollToQuestionEnd = {}
            )
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            QuestionEditor(
                questionState = PreviewFillInState(),
                onChangeQuestionType = {},
                isEditing = false,
                scrollToQuestionEnd = {}
            )
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            QuestionEditor(
                questionState = PreviewMultipleChoiceState(),
                onChangeQuestionType = {},
                isEditing = false,
                scrollToQuestionEnd = {}
            )
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
    val currentType = questionState.data.type

    // Prevent accidentally changing the type and resetting data by showing a dialog
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var nextType by rememberSaveable { mutableStateOf(QuestionType.Empty) }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogOpen = false
                    onSelectType(nextType)
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            },
            text = { Text("Change question type?") },
        )
    }

    val onClickType: (QuestionType) -> Unit = {
        nextType = it
        if (currentType == QuestionType.Empty) {
            onSelectType(nextType)
        } else if (currentType != nextType) {
            // The user has already picked the type once, use dialog to confirm
            dialogOpen = true
        }
    }

    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            Text("Type:")
        }

        Spacer(Modifier.weight(1.0f))

        // Row of icons representing selected type while also giving ability to change it
        // by pressing the desired type (resets current Question)
        Row(Modifier.selectableGroup()) {
            Icon(
                painter = painterResource(R.drawable.ic_border_color_24),
                contentDescription = "Fill in the blank",
                tint = if (currentType == QuestionType.FillIn) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .selectable(
                        selected = currentType == QuestionType.FillIn,
                        onClick = {
                            onClickType(QuestionType.FillIn)
                        })
                    .padding(12.dp)
                    .requiredSize(24.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_format_list_numbered_24),
                contentDescription = "Multiple choice",
                tint = if (currentType == QuestionType.MultipleChoice) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .selectable(
                        selected = currentType == QuestionType.MultipleChoice,
                        onClick = {
                            onClickType(QuestionType.MultipleChoice)
                        })
                    .padding(12.dp)
                    .requiredSize(24.dp),
            )
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
    scrollToQuestionEnd: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    AppTextField(
        state = question.prompt,
        onTextChange = { question.changePrompt(it) },
        label = "Question prompt",
        modifier = Modifier.fillMaxWidth(),
        containerModifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    if (!isEditing) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                "Tap an answer to mark it as the correct choice",
                style = MaterialTheme.typography.caption
            )
        }
    }
    Spacer(
        Modifier
            .height(8.dp)
            .fillMaxWidth()
    )
    Column(
        Modifier
            .selectableGroup()
            .fillMaxWidth()
    ) {
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
        val focusManager = LocalFocusManager.current
        val addAnswerAndScroll: () -> Unit = {
            focusManager.clearFocus()
            keyboardController?.hide()
            question.addAnswer()
            scrollToQuestionEnd()
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            OutlinedButton(onClick = addAnswerAndScroll) {
                Text("Add answer")
            }
        }
    }
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
                            Question.MultipleChoice.Answer("Answer text 1 ".repeat(5)),
                            Question.MultipleChoice.Answer("Answer text 2")
                        )
                    )
                ),
                isEditing = false,
                scrollToQuestionEnd = {}
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
    Row(Modifier.height(IntrinsicSize.Min)) {
        // Row that looks like: (Radio) 1. answer text [Delete]
        RadioButton(
            selected = selected,
            onClick = onSelected,
            enabled = !isEditing,
            modifier = Modifier
                .semantics {
                    contentDescription = "Pick answer ${index + 1}"
                }
                .padding(top = 12.dp) // Computed size of Radio is 48dp with 24dp icon, push down by 12 to center
                .alignBy(FirstBaseline) // Align entire Radio and its internal padding
        )
        AppTextField(
            state = answer.text,
            onTextChange = { answer.changeText(it) },
            label = "Answer text",
            modifier = Modifier.fillMaxWidth(),
            containerModifier = Modifier
                .alignBy(FirstBaseline)
                .weight(1.0f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )
        if (!isEditing) {
            // Prevent accidental deletion by showing a dialog
            var dialogOpen by rememberSaveable { mutableStateOf(false) }
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
                    text = { Text("Delete answer ${index + 1}?") },
                )
            }
            IconButton(
                onClick = { dialogOpen = true }, modifier = Modifier
                    .padding(top = 12.dp)
                    .alignBy(FirstBaseline)
                    .requiredSize(48.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    "Delete answer ${index + 1}",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

@Preview(showBackground = true, group = "MultipleChoice", widthDp = 400)
@Composable
private fun MultipleChoiceAnswerPreview() {
    QuizAppTheme {
        MultipleChoiceAnswer(
            index = 1,
            answer = PreviewMultipleChoiceState.PreviewAnswerHolder(
                TextFieldState(
                    text = "Answer text ".repeat(10),
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
    AppTextField(
        state = question.prompt,
        onTextChange = { question.changePrompt(it) },
        label = "Question prompt",
        modifier = Modifier.fillMaxWidth(),
        containerModifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    )
    AppTextField(
        state = question.correctAnswer,
        onTextChange = { question.changeCorrectAnswer(it) },
        label = "Correct answer",
        modifier = Modifier.fillMaxWidth(),
        containerModifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        enabled = !isEditing,
    )
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