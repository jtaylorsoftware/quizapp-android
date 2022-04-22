package com.github.jtaylorsoftware.quizapp.ui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState

class TestFormResponseStateHolder(
    responses: List<QuestionResponse> = emptyList(),
    val error: String? = null,
) {
    private val _responses = mutableStateListOf<FormResponseState>().apply {
        responses.forEach {
            when(it) {
                is QuestionResponse.FillIn -> TestFillInHolder(it.answer)
                is QuestionResponse.MultipleChoice -> TestMultipleChoiceResponseStateHolder(it.choice)
            }
        }
    }
    val responses: List<FormResponseState> = _responses
}

class TestMultipleChoiceResponseStateHolder(
    choice: Int = -1,
    override val error: String? = null,
) : FormResponseState.MultipleChoice {
    override var choice: Int by mutableStateOf(choice)

    override val data: QuestionResponse = QuestionResponse.MultipleChoice(
        choice = choice
    )
}

class TestFillInResponseStateHolder(
    answer: String = "",
    override val error: String? = null,
) : FormResponseState.FillIn {
    override var answer by mutableStateOf(TextFieldState(text=answer))
        private set

    override val data: QuestionResponse = QuestionResponse.FillIn(
        answer = answer
    )

    override fun changeAnswer(text: String) {
        answer = answer.copy(text = text, dirty=true)
        if (text.isBlank()) {
            answer = answer.copy(error = "Error blank")
        }
    }
}