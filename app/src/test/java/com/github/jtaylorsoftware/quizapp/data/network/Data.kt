package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.*

object TestData {
    val quizJson = """
    {
        "_id": "Q1",
        "date": "2022-04-01T06:19:47.612Z",
        "user": "U1",
        "title": "Sample Quiz",
        "expiration": "2022-05-01T06:19:47.612Z",
        "isPublic": false,
        "questions": [
          {
            "type": "FillIn",
            "text": "Question 1",
            "correctAnswer": "Answer"
          },
          {
            "type": "MultipleChoice",
            "text": "Question 2",
            "correctAnswer": 0,
            "answers": [
              {
                "text": "Answer 1"
              },
              {
                "text": "Answer 2"
              }
            ]
          }
        ],
        "results": [
          "R1", "R2"
        ],
        "allowedUsers": [
          "U2", "U3"
        ]
    }
    """.trimIndent()

    val quizDto = QuizDto(
        id = "Q1",
        date = "2022-04-01T06:19:47.612Z",
        user = "U1",
        title = "Sample Quiz",
        expiration = "2022-05-01T06:19:47.612Z",
        isPublic = false,
        allowedUsers = listOf("U2", "U3"),
        questions = listOf(
            QuestionDto.FillIn(
                text = "Question 1",
                correctAnswer = "Answer"
            ),
            QuestionDto.MultipleChoice(
                text = "Question 2",
                correctAnswer = 0,
                answers = listOf(
                    QuestionDto.MultipleChoice.Answer("Answer 1"),
                    QuestionDto.MultipleChoice.Answer("Answer 2")
                )
            )
        ),
        results = listOf("R1", "R2"),
    )

    val quizListingJson = """
    {
        "_id": "Q1",
        "date": "2022-04-01T06:19:47.612Z",
        "user": "U5",
        "title": "Quiz Title",
        "expiration": "2022-04-01T06:19:47.612Z",
        "isPublic": true,
        "resultsCount": 5,
        "questionCount": 10
    }
    """.trimIndent()
    val quizListingDto = QuizListingDto(
        id = "Q1",
        date = "2022-04-01T06:19:47.612Z",
        user = "U5",
        title = "Quiz Title",
        expiration = "2022-04-01T06:19:47.612Z",
        isPublic = true,
        resultsCount = 5,
        questionCount = 10
    )

    val quizFormJson = """
    {
      "_id": "Q1",
      "date": "2022-04-01T06:19:47.612Z",
      "user": "U5",
      "title": "Quiz 1",
      "expiration": "2022-04-01T06:19:47.612Z",
      "questions": [
        {
          "type": "FillIn",
          "text": "Question 1",
          "correctAnswer": "Answer"
        },
        {
          "type": "MultipleChoice",
          "text": "Question 2",
          "correctAnswer": 1,
          "answers": [
            {
              "text": "Answer 1"
            },
            {
              "text": "Answer 2"
            }
          ]
        }
      ]
    }
    """.trimIndent()
    val quizFormDto = QuizFormDto(
        id = "Q1",
        date = "2022-04-01T06:19:47.612Z",
        username = "U5",
        title = "Quiz 1",
        expiration = "2022-04-01T06:19:47.612Z",
        questions = listOf(
            QuestionDto.FillIn(
                text = "Question 1",
                correctAnswer = "Answer"
            ),
            QuestionDto.MultipleChoice(
                text = "Question 2",
                correctAnswer = 1,
                answers = listOf(
                    QuestionDto.MultipleChoice.Answer("Answer 1"),
                    QuestionDto.MultipleChoice.Answer("Answer 2")
                )
            )
        )
    )

    val resultJson = """
    {
        "_id": "R1",
        "date": "2022-04-01T06:19:47.612Z",
        "user": "U1",
        "username": "user1",
        "quiz": "Q1",
        "score": 0.5,
        "quizTitle": "Quiz One",
        "ownerUsername": "quizowner1",
        "answers": [
          {
            "type": "FillIn",
            "isCorrect": false,
            "answer": "Wrong",
            "correctAnswer": "Answer"
          },
          {
            "type": "MultipleChoice",
            "isCorrect": true,
            "choice": 1,
            "correctAnswer": 1
          }
        ]
    }
    """.trimIndent()
    val resultDto = QuizResultDto(
        id = "R1",
        date = "2022-04-01T06:19:47.612Z",
        user = "U1",
        username = "user1",
        quiz = "Q1",
        score = 0.5f,
        quizTitle = "Quiz One",
        createdBy = "quizowner1",
        answers = listOf(
            GradedAnswerDto.FillIn(
                isCorrect = false,
                answer = "Wrong",
                correctAnswer = "Answer"
            ),
            GradedAnswerDto.MultipleChoice(
                isCorrect = true,
                choice = 1,
                correctAnswer = 1
            )
        )
    )

    val resultListingJson = """
    {
        "_id": "R1",
        "date": "2022-04-01T06:19:47.612Z",
        "user": "U1",
        "username": "user1",
        "quiz": "Q1",
        "score": 0.5,
        "quizTitle": "Quiz One",
        "ownerUsername": "quizowner1"
    }
    """.trimIndent()
    val resultListingDto = QuizResultListingDto(
        id = "R1",
        date = "2022-04-01T06:19:47.612Z",
        user = "U1",
        username = "user1",
        quiz = "Q1",
        score = 0.5f,
        quizTitle = "Quiz One",
        createdBy = "quizowner1"
    )

    val apiErrorsJson = """
    {
        "errors": [
          {
            "field": "fieldname1",
            "message": "message",
            "value": 0,
            "expected": 1,
            "index": 5
          },
          {
            "field": "fieldname2",
            "message": "message",
            "value": "Hello",
            "expected": "World" 
          }
        ]
    }
    """.trimIndent()
    val apiErrorsDto = ApiErrorResponse(
        errors = listOf(
            ApiError("fieldname1", "message", 0.0, 1.0, 5),
            ApiError("fieldname2", "message", "Hello", "World")
        )
    )

    val objectIdResponseJson = """
    {
      "id": "JDSFffju0Z92JKZDFJdkfsofd"
    }
    """.trimIndent()
    val objectIdResponse = ObjectIdResponse("JDSFffju0Z92JKZDFJdkfsofd")

    val tokenResponseJson = """
    {
        "token": "sfd09sdfsvxljfd8324783edlkfjxmdlksfj"
    }
    """.trimIndent()
    val tokenResponse = AuthToken("sfd09sdfsvxljfd8324783edlkfjxmdlksfj")

    val userJson = """
    {
      "_id": "U1",
      "date": "2022-04-01T06:19:47.612Z",
      "username": "user1",
      "email": "email@email.com",
      "quizzes": [
        "Q1", "Q2","Q3"
      ],
      "results": [
        "R1", "R2","R3"
      ]
    }
    """.trimIndent()
    val userDto = UserDto(
        id = "U1",
        date = "2022-04-01T06:19:47.612Z",
        username = "user1",
        email= "email@email.com",
        quizzes = listOf("Q1","Q2","Q3"),
        results = listOf("R1","R2","R3")
    )

    val quizListingListJson = """
    [
      {
        "_id": "Q1",
        "date": "2022-04-01T06:19:47.612Z",
        "user": "U5",
        "title": "Quiz Title",
        "expiration": "2022-04-01T06:19:47.612Z",
        "isPublic": true,
        "resultsCount": 5,
        "questionCount": 10
      }
    ]
    """.trimIndent()
    val quizListingList = listOf(
        quizListingDto
    )

    val resultListingListJson = """
    [
      {
        "_id": "R1",
        "date": "2022-04-01T06:19:47.612Z",
        "user": "U1",
        "username": "user1",
        "quiz": "Q1",
        "score": 0.5,
        "quizTitle": "Quiz One",
        "ownerUsername": "quizowner1"
      }
    ]
    """.trimIndent()
    val resultListingList = listOf(
        resultListingDto
    )
}