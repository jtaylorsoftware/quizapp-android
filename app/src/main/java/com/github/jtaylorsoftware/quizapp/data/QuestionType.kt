package com.github.jtaylorsoftware.quizapp.data

import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz

/**
 * The type of Question, which indicates what its body will contain.
 */
enum class QuestionType {
    /**
     * A multiple-choice question with multiple answers that the user will
     * select one of.
     */
    MultipleChoice,

    /**
     * A fill-in-the-blank question with one exact answer.
     */
    FillIn,

    /**
     * No body type. This Type is used as a placeholder when creating a [Quiz].
     */
    Empty
}
