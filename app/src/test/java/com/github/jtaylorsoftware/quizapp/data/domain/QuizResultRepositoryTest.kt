package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuestionResponse
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResult
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.FakeQuizResultNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.asListing
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuizResultRepositoryTest {
    private lateinit var databaseSource: FakeQuizResultListingDatabaseSource
    private lateinit var networkSource: FakeQuizResultNetworkSource
    private lateinit var repository: QuizResultRepository

    private fun randomId() = ObjectId(UUID.randomUUID().toString())

    private val ids = (1..5).map { randomId() }
    private val userIds = (1..5).map { randomId() }
    private val quizIds = (1..5).map { randomId() }

    private val resultDto = ids.mapIndexed { index, id ->
        QuizResultDto(
            id = id.value,
            user = userIds[index].value,
            username = "username$index",
            quiz = quizIds[index].value,
            quizTitle = "quiz-dto"
        )
    }

    private val resultListingDto = resultDto.map { it.asListing() }
    private val resultListingEntities = resultDto.map {
        QuizResultListingEntity.fromDto(it.asListing()).copy(quizTitle = "quiz-entity")
    }

    @Before
    fun beforeEach() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        databaseSource = FakeQuizResultListingDatabaseSource(resultListingEntities)
        networkSource = FakeQuizResultNetworkSource(resultDto)
        repository = QuizResultRepositoryImpl(databaseSource, networkSource)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getForQuizByUser always returns network result`() = runTest {
        val quiz = quizIds[0]
        val user = userIds[0]
        val result = repository.getForQuizByUser(quiz, user)

        assertThat(
            (result as Result.Success).value,
            `is`(resultDto.first { it.quiz == quiz.value && it.user == user.value }.let {
                QuizResult.fromDto(it)
            })
        )
    }

    @Test
    fun `getForQuizByUser fails when NetworkSource fails`() = runTest {
        val error = NetworkResult.HttpError(403)
        networkSource.failOnNextWith(error)

        val result = repository.getForQuizByUser(randomId(), randomId())

        assertThat((result as Result.Failure).reason, `is`(FailureReason.FORBIDDEN))
    }

    @Test
    fun `getAllForQuiz always returns network result`() = runTest {
        val quiz = quizIds[0]
        val result = repository.getAllForQuiz(quiz)

        assertThat(
            (result as Result.Success).value,
            containsInAnyOrder(*resultDto.filter { it.quiz == quiz.value }
                .map { QuizResult.fromDto(it) }.toTypedArray())
        )
    }

    @Test
    fun `getAllForQuiz fails when NetworkSource fails`() = runTest {
        val error = NetworkResult.HttpError(403)
        networkSource.failOnNextWith(error)

        val result = repository.getAllForQuiz(randomId())

        assertThat((result as Result.Failure).reason, `is`(FailureReason.FORBIDDEN))
    }

    @Test
    fun `getListingForQuizByUser returns local then network result`() = runTest {
        val user = userIds[0]
        val quiz = quizIds[0]
        val results = mutableListOf<ResultOrFailure<QuizResultListing>>()
        repository.getListingForQuizByUser(quiz, user).collect {
            results.add(it)
        }

        assertThat(
            (results[0] as Result.Success).value,
            `is`(QuizResultListing.fromEntity(resultListingEntities.first { it.quiz == quiz.value && it.user == user.value }))
        )
        assertThat(
            (results[1] as Result.Success).value,
            `is`(QuizResultListing.fromDto(resultListingDto.first { it.quiz == quiz.value && it.user == user.value }))
        )
    }

    @Test
    fun `getListingForQuizByUser deletes local result when network returns Not Found`() = runTest {
        val user = userIds[0]
        val quiz = quizIds[0]
        val results = mutableListOf<ResultOrFailure<QuizResultListing>>()
        networkSource.failOnNextWith(NetworkResult.HttpError(404))
        repository.getListingForQuizByUser(quiz, user).collect {
            results.add(it)
        }

        // Should still retrieve the local data initially
        assertThat(
            (results[0] as Result.Success).value,
            `is`(QuizResultListing.fromEntity(resultListingEntities.first { it.quiz == quiz.value && it.user == user.value }))
        )

        // Network error on a single result type should cause the second value to be an error
        assertThat(results[1], IsInstanceOf(Result.Failure::class.java))

        // Should also no longer have local data
        assertThat(
            databaseSource.getByQuizAndUser(quiz.value, user.value),
            `is`(nullValue())
        )
    }

    @Test
    fun `getListingForQuizByUser should fail with NotFound when neither source has listings`() =
        runTest {
            val results = mutableListOf<ResultOrFailure<QuizResultListing>>()
            repository.getListingForQuizByUser(randomId(), randomId()).collect {
                results.add(it)
            }

            assertThat((results[0] as Result.Failure).reason, `is`(FailureReason.NOT_FOUND))
        }

    @Test
    fun `getAllListingsForQuiz returns local then network result`() = runTest {
        val quiz = quizIds[0]
        val results = mutableListOf<ResultOrFailure<List<QuizResultListing>>>()
        repository.getAllListingsForQuiz(quiz).collect {
            results.add(it)
        }

        assertThat(
            (results[0] as Result.Success).value,
            containsInAnyOrder(*resultListingEntities.filter { it.quiz == quiz.value }
                .map { QuizResultListing.fromEntity(it) }.toTypedArray())
        )
        assertThat(
            (results[1] as Result.Success).value,
            containsInAnyOrder(*resultListingDto.filter { it.quiz == quiz.value }
                .map { QuizResultListing.fromDto(it) }
                .toTypedArray())
        )
    }

    @Test
    fun `getAllListingsForQuiz deletes local data when network returns emptyList`() = runTest {
        val quiz = quizIds[0]
        val results = mutableListOf<ResultOrFailure<List<QuizResultListing>>>()

        // Create networkSource with no data
        networkSource = FakeQuizResultNetworkSource()
        repository = QuizResultRepositoryImpl(databaseSource, networkSource)

        repository.getAllListingsForQuiz(quiz).collect {
            results.add(it)
        }

        // Should still retrieve initial local data
        assertThat(
            (results[0] as Result.Success).value,
            containsInAnyOrder(*resultListingEntities.filter { it.quiz == quiz.value }
                .map { QuizResultListing.fromEntity(it) }.toTypedArray())
        )

        // Network error should cause Result.Success with emptyList (because the request
        // didn't actually fail, it just returned nothing)
        assertThat((results[1] as Result.Success).value, `is`(empty()))
        assertThat(
            databaseSource.getAllByQuiz(quiz.value),
            `is`(empty())
        )
    }

    @Test
    fun `getAllListingsForQuiz should cache network result`() = runTest {
        val quiz = quizIds[0]
        repository.getAllListingsForQuiz(quiz).collect()

        val results = databaseSource.getAllByQuiz(quiz.value)
        assertThat(
            results.map { QuizResultListing.fromEntity(it) },
            containsInAnyOrder(*resultListingDto.filter { it.quiz == quiz.value }
                .map { QuizResultListing.fromDto(it) }
                .toTypedArray())
        )
    }

    @Test
    fun `createResponseForQuiz uploads to NetworkSource and returns id`() = runTest {
        val result = repository.createResponseForQuiz(listOf(QuestionResponse.FillIn()), ObjectId())

        // Should succeed with ObjectId
        assertThat(result, IsInstanceOf(Result.Success::class.java))
    }

    @Test
    fun `createResponseForQuiz should fail with Forbidden when NetworkSource fails with 403 + expiration error`() =
        runTest {
            val error = NetworkResult.HttpError(
                403, listOf(
                    ApiError(field = "expiration", message = "expired")
                )
            )
            networkSource.failOnNextWith(error)

            val result =
                repository.createResponseForQuiz(listOf(QuestionResponse.FillIn()), ObjectId())

            // Should fail with expired=true
            assertThat((result as Result.Failure).reason, `is`(FailureReason.QUIZ_EXPIRED))
        }
}