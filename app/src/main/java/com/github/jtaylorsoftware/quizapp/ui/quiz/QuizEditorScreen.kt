package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.Question
import com.github.jtaylorsoftware.quizapp.data.Quiz
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import java.time.Instant

/**
 * Displays a form for a user to create or edit a [Quiz]. The mode
 * (creating or editing) is controlled by the [isEditing] flag, and the
 * displayed content will vary accordingly. Notably, some fields will be
 * immutable in editing mode.
 *
 * @param quizState The basic data for a Quiz, such as the expiration and title.
 * @param questions The questions in the Quiz.
 * @param onChangeQuizState Called when modifying one or more properties of [quizState].
 * @param onAddQuestion Callback invoked to add a [new question][QuestionState.Empty] to the Quiz.
 * @param onChangeQuestionType Called to change the type of a question to something other than [Empty][QuestionState.Empty].
 * @param onEditQuestion Function invoked to modify Question prompt or its answers.
 * @param onDeleteQuestion Called when deleting a specific Question.
 * @param onSubmit Called when submitting the Quiz.
 * @param isEditing Flag controlling mutability of the current Quiz.
 */
@Composable
fun QuizEditorScreen(
    quizState: QuizState,
    questions: List<QuestionState>,
    onChangeQuizState: (QuizState) -> Unit,
    onAddQuestion: () -> Unit,
    onChangeQuestionType: (Int, Question.Type) -> Unit,
    onEditQuestion: (Int, QuestionState) -> Unit,
    onDeleteQuestion: (Int) -> Unit,
    onSubmit: () -> Unit,
    isEditing: Boolean = false,
) {
    Scaffold(
        floatingActionButton = {
            Column {
                FloatingActionButton(onClick = onSubmit) {
                    Icon(painter = painterResource(R.drawable.ic_publish_24), "Upload quiz")
                }
                if (!isEditing) {
                    Spacer(Modifier.requiredHeight(8.dp))
                    FloatingActionButton(onClick = onAddQuestion) {
                        Icon(Icons.Default.Add, "Add question")
                    }
                }
            }
        }
    ) {
        LazyColumn(Modifier.fillMaxWidth()) {
            // Basic Quiz data
            item {
                QuizHeader(quizState = quizState, onChangeQuizState = onChangeQuizState)
            }

            item {
                Text("Questions:")
            }

            // List of editable questions
            itemsIndexed(questions, key = { _, q -> q.key }) { index, questionState ->
                // Display the index and delete button separately for clarity
                Row {
                    Text("Question ${index + 1}:")
                    if (!isEditing) {
                        IconButton(onClick = { onDeleteQuestion(index) }) {
                            Icon(Icons.Default.Delete, "Delete question")
                        }
                    }
                }
                Card {
                    QuestionEditor(
                        questionState,
                        onChangeQuestionType = {
                            onChangeQuestionType(index, it)
                        },
                        onEditQuestion = {
                            onEditQuestion(index, it)
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
                question = it,
                answerErrors = listOf(null, null)
            )
            is Question.FillIn -> QuestionState.FillIn(question = it)
            else -> QuestionState.Empty
        }
    }

    QuizAppTheme {
        QuizEditorScreen(
            quizState = QuizState(title = TextFieldState("My Quiz"), isPublic = false),
            questions = questionState,
            onChangeQuizState = {},
            onAddQuestion = { /*TODO*/ },
            onChangeQuestionType = { _, _ -> },
            onEditQuestion = { _, _ -> },
            onDeleteQuestion = {},
            onSubmit = {})
    }
}

/**
 * Displays editable fields for basic Quiz data, including title, allowedUsers, and expiration.
 */
@Composable
private fun QuizHeader(quizState: QuizState, onChangeQuizState: (QuizState) -> Unit) {
    Card {
        Column {
            QuizTitle(
                title = quizState.title,
                onTitleChange = {
                    onChangeQuizState(
                        quizState.copy(title = quizState.title.copy(text = it))
                    )
                }
            )
            AllowedUsers(
                isPublic = quizState.isPublic,
                onChangeIsPublic = { onChangeQuizState(quizState.copy(isPublic = it)) },
                allowedUsers = quizState.allowedUsers,
                onChangeAllowedUsers = { onChangeQuizState(quizState.copy(allowedUsers = it)) },
                allowedUsersError = quizState.allowedUsersError
            )
            Expiration(
                expiration = quizState.expiration,
                changeExpiration = { onChangeQuizState(quizState.copy(expiration = it)) },
                expirationError = quizState.expirationError
            )
        }
    }
}

/**
 * Renders editable Quiz title field.
 */
@Composable
private fun QuizTitle(title: TextFieldState, onTitleChange: (String) -> Unit) {
    TextField(
        value = title.text,
        onValueChange = onTitleChange,
        isError = title.error != null,
        label = {
            Text("Quiz Title")
        },
        modifier = Modifier.semantics {
            contentDescription = "Edit quiz title"
        }
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
@Composable
private fun AllowedUsers(
    isPublic: Boolean,
    onChangeIsPublic: (Boolean) -> Unit,
    allowedUsers: String,
    onChangeAllowedUsers: (String) -> Unit,
    allowedUsersError: String?,
) {
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
        TextField(value = allowedUsers, onValueChange = onChangeAllowedUsers, label = {
            Text("Allowed Users")
        }, modifier = Modifier.semantics {
            contentDescription = "Edit allowed users"
        }, isError = allowedUsersError != null)
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
    Text("Expiration:")
    // TODO
}

/**
 * Provides components to edit all parts of a Question, including its type.
 */
@Composable
private fun QuestionEditor(
    questionState: QuestionState,
    onChangeQuestionType: (Question.Type) -> Unit,
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
                    question = questionState.question,
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
                    question = questionState.question,
                    onEditQuestion = { onEditQuestion(questionState.copy(question = it)) },
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
                    questionState = QuestionState.Empty,
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
    onSelectType: (Question.Type) -> Unit,
) {
    val selectedType = questionState.question.type
    Row {
        Text("Question type:")

        // Row of icons representing selected type while also giving ability to change it
        // by pressing the desired type (resets current Question)
        Row(Modifier.selectableGroup()) {
            Box(
                Modifier
                    .selectable(
                        selected = selectedType == Question.Type.FillIn,
                        onClick = { onSelectType(Question.Type.FillIn) })
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_border_color_24),
                    contentDescription = "Fill in the blank question",
                    tint = if (selectedType == Question.Type.FillIn) MaterialTheme.colors.primary else Color.DarkGray,
                )
            }

            Box(
                Modifier
                    .selectable(
                        selected = selectedType == Question.Type.MultipleChoice,
                        onClick = { onSelectType(Question.Type.MultipleChoice) })
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_format_list_numbered_24),
                    contentDescription = "Multiple choice question",
                    tint = if (selectedType == Question.Type.MultipleChoice) MaterialTheme.colors.primary else Color.DarkGray,
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
            QuestionTypeSelector(questionState = QuestionState.Empty, onSelectType = {})
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
    TextField(value = question.text, onValueChange = onChangePrompt, label = {
        Text("Question prompt")
    }, isError = questionTextError != null)
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
    Row {
        // Row that looks like: (Radio) 1. answer text [Delete]
        RadioButton(selected = selected, onClick = onSelected, enabled = !isEditing,
            modifier = Modifier.semantics {
                contentDescription = "Pick answer ${index + 1}"
            })
        Column {
            TextField(value = text, onValueChange = onChangeText, label = {
                Text("Answer text")
            }, modifier = Modifier.semantics {
                contentDescription = "Edit answer ${index + 1} text"
            }, isError = answerError != null)
            answerError?.let {
                Text(it, modifier = Modifier.semantics {
                    contentDescription = "Answer ${index + 1} hint"
                })
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.semantics {
            contentDescription = "Delete answer ${index + 1}"
        }) {
            Icon(Icons.Default.Delete, null)
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
@Composable
private fun FillInQuestion(
    question: Question.FillIn,
    questionTextError: String?,
    answerError: String?,
    onEditQuestion: (Question.FillIn) -> Unit,
    isEditing: Boolean,
) {
    TextField(value = question.text, onValueChange = {
        onEditQuestion(question.copy(text = it))
    }, label = {
        Text("Question prompt")
    }, isError = questionTextError != null)
    questionTextError?.let {
        Text(it, modifier = Modifier.semantics {
            contentDescription = "Question prompt hint"
        })
    }
    TextField(value = question.correctAnswer!!, onValueChange = {
        onEditQuestion(question.copy(correctAnswer = it))
    }, label = {
        Text("Correct answer")
    }, modifier = Modifier.semantics {
        contentDescription = "Change correct answer text"
    }, enabled = !isEditing,
        isError = answerError != null
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
                onEditQuestion = {},
                isEditing = false
            )
        }
    }
}