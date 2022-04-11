package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.Question
import com.github.jtaylorsoftware.quizapp.data.domain.models.Quiz
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.matchers.SameQuizValidationErrorAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuizRepositoryTest {
    private lateinit var databaseSource: FakeQuizListingDatabaseSource
    private lateinit var networkSource: FakeQuizNetworkSource
    private lateinit var repository: QuizRepository

    private fun randomId() = ObjectId(UUID.randomUUID().toString())

    private val ids = (1..10).map { randomId() }

    private val quizEntities = (1..5).map {
        QuizListingEntity(
            id = ids[it - 1].value,
            title = "quiz-entity-$it"
        )
    }
    private val quizDto = (1..5).map {
        QuizDto(
            id = ids[it - 1].value,
            title = "quiz-dto-$it"
        )
    }

    @Before
    fun beforeEach() {
        Dispatchers.setMain(StandardTestDispatcher())
        databaseSource = FakeQuizListingDatabaseSource(quizEntities)
        networkSource = FakeQuizNetworkSource(quizDto)
        repository = QuizRepositoryImpl(databaseSource, networkSource)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getQuiz should always return network Quiz`() = runTest {
        // Index of test data to retrieve
        val ind = 0

        // Should be the QuizDto from the network source, converted to the domain object
        val quiz = repository.getQuiz(ids[ind])
        assertThat((quiz as Result.Success).value.title, `is`(quizDto[ind].title))
    }

    @Test
    fun `getQuiz should fail when NetworkSource fails`() = runTest {
        // Force the NetworkSource to fail
        val error = NetworkResult.NetworkError(IOException())
        networkSource.failOnNextWith(error)

        // Should fail because NetworkSource did
        val quizResult = repository.getQuiz(ids[0])
        assertThat(quizResult, IsInstanceOf(Result.NetworkError::class.java))
    }

    @Test
    fun `getQuiz should fail with NotFound when NetworkSource returns 404`() = runTest {
        // Should fail because NetworkSource did
        val quiz = repository.getQuiz(randomId())

        assertThat(quiz, IsInstanceOf(Result.NotFound::class.java))
    }

    @Test
    fun `getAsListing should return local then network value`() = runTest {
        val quizzes = mutableListOf<Result<QuizListing, Any?>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getAsListing(ids[0]).take(2).collect {
                quizzes.add(it)
            }
        }

        job.cancel()

        assertThat(quizzes, hasSize(2))
        assertThat((quizzes[0] as Result.Success).value.title, `is`(quizEntities[0].title))
        assertThat((quizzes[1] as Result.Success).value.title, `is`(quizDto[0].title))
    }

    @Test
    fun `getAsListing should cache network result`() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getAsListing(ids[0]).take(2).collect()
        }

        job.cancel()

        val cached = databaseSource.getById(ids[0].value)!!
        assertThat(
            cached.title,
            `is`(quizDto[0].title)
        )
    }

    @Test
    fun `getAsListing should fail with NotFound when neither source has the requested quiz`() =
        runTest {
            val results = mutableListOf<Result<QuizListing, Any?>>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.getAsListing(randomId()).collect {
                    results.add(it)
                }
            }

            job.cancel()

            assertThat(results[0], IsInstanceOf(Result.NotFound::class.java))
        }

    @Test
    fun `createQuiz uploads to NetworkSource and returns id`() = runTest {
        val result = repository.createQuiz(Quiz())

        // Should succeed with id
        val id = (result as Result.Success).value

        // Should be able to get same back now from repository
        val quiz = repository.getQuiz(id)
        assertThat(quiz, IsInstanceOf(Result.Success::class.java))
    }

    @Test
    fun `createQuiz saves a listing locally for the successfully created quiz`() = runTest {
        val quiz = Quiz()
        val result = repository.createQuiz(quiz)

        // Should succeed with id
        val id = (result as Result.Success).value

        // Should be able to get listing back from database using returned id from network
        val quizListing = databaseSource.getById(id.value)
        assertThat(quizListing!!.date, `is`(quiz.date.toString()))
    }

    @Test
    fun `createQuiz should fail with QuizValidationError when NetworkSource returns HTTP 400`() =
        runTest {
            // Force the NetworkSource to fail
            val errorMessage = "Invalid"
            val error = NetworkResult.HttpError(
                400,
                listOf(
                    ApiError(field = "title", message = errorMessage),
                    ApiError(field = "expiration", message = errorMessage),
                    ApiError(field = "allowedUsers", message = errorMessage),
                    ApiError(field = "questions", message = errorMessage),
                    ApiError(field = "questions", message = errorMessage, index = 0),
                    ApiError(field = "questions", message = errorMessage, index = 1)
                )
            )
            networkSource.failOnNextWith(error)

            val result =
                repository.createQuiz(Quiz(questions = List(2) { Question.MultipleChoice() }))
            assertThat(result, IsInstanceOf(Result.BadRequest::class.java))

            val validationError = QuizValidationErrors(
                title = errorMessage,
                expiration = errorMessage,
                allowedUsers = errorMessage,
                questions = errorMessage,
                questionErrors = listOf(errorMessage, errorMessage)
            )

            assertThat(
                (result as Result.BadRequest).error,
                `is`(SameQuizValidationErrorAs(validationError))
            )
        }

    @Test
    fun `editQuiz uploads to NetworkSource`() = runTest {
        val result = repository.editQuiz(ids[0], Quiz())

        // Should succeed with Unit
        assertThat(result, IsInstanceOf(Result.Success::class.java))
    }

    @Test
    fun `editQuiz should cache updated listing when successfully editing quiz`() = runTest {
        // Use a new Quiz as the "edits" - realistically the id should never be modified, the
        // id comes from the response from createQuiz (which is attached to a Quiz/QuizListing saved locally)
        val edits = Quiz(id = ObjectId("123"))
        repository.editQuiz(ids[0], edits)

        // Should be able to get updated listing back from database
        val quizListing = databaseSource.getById(edits.id.value)
        assertThat(quizListing!!.date, `is`(edits.date.toString()))
    }

    @Test
    fun `editQuiz should fail with QuizValidationError when NetworkSource returns HTTP 400`() =
        runTest {
            // Force the NetworkSource to fail
            val errorMessage = "Invalid"
            val error = NetworkResult.HttpError(
                400,
                listOf(
                    ApiError(field = "title", message = errorMessage),
                    ApiError(field = "expiration", message = errorMessage),
                    ApiError(field = "allowedUsers", message = errorMessage),
                    ApiError(field = "questions", message = errorMessage),
                    ApiError(field = "questions", message = errorMessage, index = 0),
                    ApiError(field = "questions", message = errorMessage, index = 1)
                )
            )
            networkSource.failOnNextWith(error)

            val result = repository.editQuiz(
                ids[0],
                Quiz(questions = List(2) { Question.MultipleChoice() })
            )
            assertThat(result, IsInstanceOf(Result.BadRequest::class.java))

            val validationError = QuizValidationErrors(
                title = errorMessage,
                expiration = errorMessage,
                allowedUsers = errorMessage,
                questions = errorMessage,
                questionErrors = listOf(errorMessage, errorMessage)
            )

            assertThat(
                (result as Result.BadRequest).error,
                `is`(SameQuizValidationErrorAs(validationError))
            )
        }

    @Test
    fun `deleteQuiz should delete from NetworkSource`() = runTest {
        val id = ids[0]
        val result = repository.deleteQuiz(id)

        // Should succeed with Unit
        assertThat(result, IsInstanceOf(Result.Success::class.java))

        // Should be deleted
        val getResult = repository.getQuiz(id)
        assertThat(getResult, IsInstanceOf(Result.NotFound::class.java))
    }

    @Test
    fun `deleteQuiz should fail when NetworkSource fails`() = runTest {
        // Force the NetworkSource to fail
        val error = NetworkResult.HttpError(403)
        networkSource.failOnNextWith(error)

        val result = repository.deleteQuiz(ObjectId())
        assertThat(result, IsInstanceOf(Result.Forbidden::class.java))
    }
}