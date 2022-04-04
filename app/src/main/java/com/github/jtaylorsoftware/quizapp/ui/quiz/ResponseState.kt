package com.github.jtaylorsoftware.quizapp.ui.quiz

import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse


data class ResponseState(
    val response: QuestionResponse,
    val error: String? = null
)