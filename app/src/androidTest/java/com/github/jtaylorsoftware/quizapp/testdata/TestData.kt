package com.github.jtaylorsoftware.quizapp.testdata

import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import com.github.jtaylorsoftware.quizapp.data.network.UserWithPassword
import com.github.jtaylorsoftware.quizapp.data.network.asListing
import com.github.jtaylorsoftware.quizapp.data.network.dto.*
import java.util.*


val loggedInUserId = randomId()
const val loggedInUserUsername = "LoggedInUserTest"
const val loggedInUserPassword = "password"
const val loggedInUserQuizTitle = "QuizByLoggedInUserTest"
val loggedInUserQuizId = randomId()
val loggedInUserResultId = randomId()

val otherUserId = randomId()
const val otherUserUsername = "OtherUserTest"
const val otherUserPassword = "password"
val otherUserResultId = randomId()

val otherUserDto = UserDto(
    id = otherUserId,
    username = otherUserUsername,
    email = "LoggedInUserTestEmail@example.com",
    quizzes = listOf(),
    results = listOf(otherUserResultId),
)
//
//val otherUserWithPassword = UserWithPassword(
//    user = otherUserDto, password = otherUserPassword
//)

val otherUserQuizResultDtos = listOf(
    QuizResultDto(
        id = otherUserResultId,
        user = otherUserId,
        username = otherUserUsername,
        quiz = loggedInUserQuizId,
        score = 0.5f,
        quizTitle = loggedInUserQuizTitle,
        createdBy = loggedInUserUsername,
        answers = listOf(
            GradedAnswerDto.FillIn(isCorrect = false, answer = "IncorrectAnswer"),
            GradedAnswerDto.MultipleChoice(isCorrect = true, choice = 0, correctAnswer = 0)
        )
    )
)
//
//val otherUserQuizResultListingDto: List<QuizResultListingDto> =
//    otherUserQuizResultDtos.map { it.asListing() }

val otherUserQuizResultListingEntities: List<QuizResultListingEntity> =
    otherUserQuizResultDtos.map { QuizResultListingEntity.fromDto(it.asListing()) }

val loggedInUserQuizzesDtos = listOf(
    QuizDto(
        id = loggedInUserQuizId,
        user = loggedInUserId,
        title = loggedInUserQuizTitle,
        isPublic = true,
        questions = listOf(
            QuestionDto.FillIn(
                text = "TestFillInPrompt",
                correctAnswer = "TestFillInCorrectAnswer"
            ),
            QuestionDto.MultipleChoice(
                text = "TestMultipleChoicePrompt", correctAnswer = 0, answers = listOf(
                    QuestionDto.MultipleChoice.Answer("TestMultipleChoiceAnswer0"),
                    QuestionDto.MultipleChoice.Answer("TestMultipleChoiceAnswer1")
                )
            )
        ),
        results = listOf(otherUserResultId)
    )
)

val loggedInUserQuizListingDtos: List<QuizListingDto> =
    loggedInUserQuizzesDtos.map { it.asListing() }

val loggedInUserQuizListingEntities: List<QuizListingEntity> =
    loggedInUserQuizzesDtos.map { QuizListingEntity.fromDto(it.asListing()) }


val loggedInUserQuizResultDtos = listOf(
    QuizResultDto(
        id = loggedInUserResultId,
        user = loggedInUserId,
        username = loggedInUserUsername,
        quiz = loggedInUserQuizId,
        score = 0.5f,
        quizTitle = loggedInUserQuizTitle,
        createdBy = loggedInUserUsername,
        answers = listOf(
            GradedAnswerDto.FillIn(isCorrect = false, answer = "IncorrectAnswer"),
            GradedAnswerDto.MultipleChoice(isCorrect = true, choice = 0, correctAnswer = 0)
        )
    )
)

val loggedInUserQuizResultListingEntities: List<QuizResultListingEntity> =
    loggedInUserQuizResultDtos.map { QuizResultListingEntity.fromDto(it.asListing()) }


val loggedInUserQuizResultListingDtos: List<QuizResultListingDto> =
    loggedInUserQuizResultDtos.map { it.asListing() }

val loggedInUserDto = UserDto(
    id = loggedInUserId,
    username = loggedInUserUsername,
    email = "LoggedInUserTestEmail@example.com",
    quizzes = listOf(loggedInUserQuizId),
    results = listOf(),
)
val loggedInUserEntity = UserEntity.fromDto(loggedInUserDto)

val loggedInUserJwt = randomId()

val loggedInUserWithPassword = UserWithPassword(
    user = loggedInUserDto, password = loggedInUserPassword
)

val testQuizDtos = loggedInUserQuizzesDtos
val testResultDtos = otherUserQuizResultDtos + loggedInUserQuizResultDtos
val testResultEntities = otherUserQuizResultListingEntities + loggedInUserQuizResultListingEntities
//val testUserWithPasswords = listOf(otherUserWithPassword, loggedInUserWithPassword)
//val testUserDtos = listOf(otherUserDto, loggedInUserDto)

private fun randomId(): String = UUID.randomUUID().toString()