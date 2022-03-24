package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.Composable
import com.github.jtaylorsoftware.quizapp.data.QuizForm
import com.github.jtaylorsoftware.quizapp.data.Response

@Composable
fun QuizFormScreen(
    quiz: QuizForm,
    responses: List<Response>,
    responseErrors: List<String?>,
    onChangeResponse: (Int, Response) -> Unit,
    onSubmit: () -> Unit,
) {

}