package com.github.jtaylorsoftware.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizForm
import com.github.jtaylorsoftware.quizapp.ui.quiz.QuizFormScreen
import com.github.jtaylorsoftware.quizapp.ui.quiz.ResponseState
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme

class MainActivity : ComponentActivity() {
    //    private var quizState: QuizState by mutableStateOf(QuizState())
//    private var questions = mutableStateListOf<QuestionState>()
    private val questions = listOf(
        Question.MultipleChoice(
            text = "MC Question 1", answers = listOf(
                Question.MultipleChoice.Answer("MC Answer 1-1"),
                Question.MultipleChoice.Answer("MC Answer 1-2"),
                Question.MultipleChoice.Answer("MC Answer 1-3"),
            )
        ),
        Question.FillIn(text = "Fill Question 1"),
        Question.FillIn(text = "Fill Question 2"),
        Question.MultipleChoice(
            text = "MC Question 2", answers = listOf(
                Question.MultipleChoice.Answer("MC Answer 2-1"),
                Question.MultipleChoice.Answer("MC Answer 2-2"),
                Question.MultipleChoice.Answer("MC Answer 2-3"),
            )
        ),
        Question.FillIn(text = "Fill Question 3"),
    )
    private val quiz = QuizForm(
        createdBy = "Username123",
        title = "Quiztitle123",
        questions = questions,
    )

    private lateinit var responses: SnapshotStateList<ResponseState>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        responses = mutableStateListOf()
        questions.forEach {
            when (it) {
                is Question.MultipleChoice -> {
                    responses.add(ResponseState(QuestionResponse.MultipleChoice(choice = -1)))
                }
                is Question.FillIn -> {
                    responses.add(ResponseState(QuestionResponse.FillIn()))
                }
                else -> throw IllegalArgumentException()
            }
        }
        val changeResponse: (Int, ResponseState) -> Unit = { i, r ->
            responses[i] = r
        }
        setContent {
            QuizAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
//                    QuizEditorScreen(
//                        quizState = quizState,
//                        questions = questions,
//                        onChangeQuizState = { quizState = it },
//                        onAddQuestion = { questions.add(QuestionState.Empty()) },
//                        onChangeQuestionType = { i, type ->
//                            questions[i] = when (type) {
//                                QuestionType.Empty -> throw IllegalArgumentException("Cannot change to empty")
//                                QuestionType.FillIn -> QuestionState.FillIn()
//                                QuestionType.MultipleChoice -> QuestionState.MultipleChoice()
//                            }
//                        },
//                        onEditQuestion = { i, question ->
//                            Log.d("FOCUS", "onEditQuestion called with key: ${question.key}")
//                            questions[i] = question
//                        },
//                        onDeleteQuestion = { i -> questions.removeAt(i) },
//                        onSubmit = { /*TODO*/ })
                    QuizFormScreen(
                        quiz,
                        responses,
                        onChangeResponse = changeResponse,
                        onSubmit = {})
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    QuizAppTheme {
        Greeting("Android")
    }
}