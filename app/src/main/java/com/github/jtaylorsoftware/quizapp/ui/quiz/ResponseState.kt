package com.github.jtaylorsoftware.quizapp.ui.quiz

import com.github.jtaylorsoftware.quizapp.data.Response

data class ResponseState(
    val response: Response,
    val error: String? = null
)