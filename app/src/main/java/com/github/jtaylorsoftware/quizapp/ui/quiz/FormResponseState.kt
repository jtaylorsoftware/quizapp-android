package com.github.jtaylorsoftware.quizapp.ui.quiz

import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState

sealed interface FormResponseState {
    val data: QuestionResponse

    /**
     * The error for the response input by the user.
     */
    val error: String?

    interface MultipleChoice : FormResponseState {
        /**
         * Index of the selected answer.
         */
        var choice: Int
    }

    interface FillIn : FormResponseState {
        /**
         * The input text as the user's answer to the question.
         */
        val answer: TextFieldState

        fun changeAnswer(text: String)
    }
}