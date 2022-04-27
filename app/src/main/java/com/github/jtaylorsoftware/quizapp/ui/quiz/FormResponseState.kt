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

/**
 * [FormResponseState.FillIn] state suitable for previews.
 */
data class PreviewFillInFormState(
    override val data: QuestionResponse,
    override val error: String?,
    override val answer: TextFieldState
) : FormResponseState.FillIn {
    override fun changeAnswer(text: String) {
        TODO("Not yet implemented")
    }
}

/**
 * [FormResponseState.MultipleChoice] state suitable for previews.
 */
data class PreviewMultipleChoiceFormState(
    override val data: QuestionResponse,
    override val error: String?,
    override var choice: Int
) : FormResponseState.MultipleChoice