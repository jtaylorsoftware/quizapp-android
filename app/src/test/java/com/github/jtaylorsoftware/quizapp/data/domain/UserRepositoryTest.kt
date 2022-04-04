package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.QuizResultListing
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeQuizResultListingDatabaseSource
import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import com.github.jtaylorsoftware.quizapp.data.network.FakeUserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.UserWithPassword
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizListingDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultListingDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserDto
import com.github.jtaylorsoftware.quizapp.matchers.SameUserAs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.core.IsInstanceOf
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {
    private lateinit var cache: FakeUserCache
    private lateinit var quizSource: FakeQuizListingDatabaseSource
    private lateinit var resultSource: FakeQuizResultListingDatabaseSource
    private lateinit var networkSource: FakeUserNetworkSource
    private lateinit var repository: UserRepository

    private fun randomId() = ObjectId(UUID.randomUUID().toString())

    private val userEntity = UserEntity(
        id = randomId().value,
        username = "user-entity",
    )
    private val userDto = UserDto(
        id = userEntity.id,
        date = userEntity.date,
        username = "user-dto",
        quizzes = listOf(randomId().value),
        results = listOf(randomId().value)
    )
    private val password = "password"
    private val userWithPassword = UserWithPassword(userDto, password)

    private val quizDto = listOf(
        QuizListingDto(id = randomId().value, title = "quiz-dto", user = userEntity.id),
        QuizListingDto(id = randomId().value, title = "quiz-dto", user = userEntity.id)
    )
    private val quizResultDto = quizDto.map { quiz ->
        QuizResultListingDto(
            id = randomId().value,
            quiz = quiz.id,
            quizTitle = quiz.title,
            user = userEntity.id,
            date = quiz.date
        )
    }
    private val quizEntity = quizDto.map { dto ->
        QuizListingEntity.fromDto(dto).copy(title="quiz-entity")
    }
    private val resultEntity = quizResultDto.map { dto ->
        QuizResultListingEntity.fromDto(dto).copy(quizTitle = "quiz-entity")
    }

    @Before
    fun beforeEach() {
        cache = FakeUserCache(userEntity)
        networkSource = FakeUserNetworkSource(listOf(userWithPassword), quizDto, quizResultDto)
        quizSource = FakeQuizListingDatabaseSource(quizEntity)
        resultSource = FakeQuizResultListingDatabaseSource(resultEntity)
        repository = UserRepositoryImpl(cache, networkSource, quizSource, resultSource)
    }

    @Test
    fun `getProfile should return local then network value`() = runTest {
        val results = mutableListOf<Result<User, Any?>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getProfile().take(2).collect {
                results.add(it)
            }
        }

        job.cancel()
        assertThat(
            (results[0] as Result.Success).value,
            `is`(SameUserAs(User.fromEntity(userEntity)))
        )
        assertThat(
            (results[1] as Result.Success).value,
            `is`(SameUserAs(User.fromDto(userDto)))
        )
    }

    @Test
    fun `getProfile should cache network response locally`() = runTest {
        // pre assert user entity is saved
        var cachedUser = cache.loadUser()
        assertThat(cachedUser, `is`(userEntity))

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getProfile().take(2).collect()
        }

        job.cancel()

        // should now be network value
        cachedUser = cache.loadUser()!!
        assertThat(User.fromEntity(cachedUser), `is`(SameUserAs(User.fromDto(userDto))))
    }

    @Test
    fun `getProfile should emit a failure when NetworkSource fails`() = runTest {
        val error = NetworkResult.HttpError(401)
        networkSource.failOnNextWith(error)

        val results = mutableListOf<Result<User, Any?>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getProfile().take(2).collect {
                results.add(it)
            }
        }

        job.cancel()
        assertThat(results[0], IsInstanceOf(Result.Success::class.java))
        assertThat(results[1], IsInstanceOf(Result.Unauthorized::class.java))
    }

    @Test
    fun `getQuizzes should return local then network value`() = runTest {
        val results = mutableListOf<Result<List<QuizListing>, Any?>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getQuizzes().take(2).collect {
                results.add(it)
            }
        }

        job.cancel()

        assertThat(
            (results[0] as Result.Success).value,
            containsInAnyOrder(*quizEntity.map {
                QuizListing.fromEntity(it)
            }.toTypedArray())
        )
        assertThat(
            (results[1] as Result.Success).value,
            containsInAnyOrder(*quizDto.map {
                QuizListing.fromDto(it)
            }.toTypedArray())
        )
    }

    @Test
    fun `getQuizzes should cache network response locally`() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getQuizzes().take(2).collect()
        }

        job.cancel()

        val cachedQuizzes = quizSource.getAllCreatedByUser(userDto.id)
        assertThat(
            cachedQuizzes.map { QuizListing.fromEntity(it) },
            containsInAnyOrder(*quizDto.map { QuizListing.fromDto(it) }.toTypedArray())
        )
    }

    @Test
    fun `getQuizzes should emit a failure when NetworkSource fails`() = runTest {
        val error = NetworkResult.HttpError(401)
        networkSource.failOnNextWith(error)

        val results = mutableListOf<Result<List<QuizListing>, Any?>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getQuizzes().take(2).collect {
                results.add(it)
            }
        }

        job.cancel()
        assertThat(results[0], IsInstanceOf(Result.Success::class.java))
        assertThat(results[1], IsInstanceOf(Result.Unauthorized::class.java))
    }

    @Test
    fun `getResults should return local then network value`() = runTest {
        val results = mutableListOf<Result<List<QuizResultListing>, Any?>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getResults().take(2).collect {
                results.add(it)
            }
        }

        job.cancel()

        assertThat(
            (results[0] as Result.Success).value,
            containsInAnyOrder(*resultEntity.map {
                QuizResultListing.fromEntity(it)
            }.toTypedArray())
        )
        assertThat(
            (results[1] as Result.Success).value,
            containsInAnyOrder(*quizResultDto.map {
                QuizResultListing.fromDto(it)
            }.toTypedArray())
        )
    }

    @Test
    fun `getResults should cache network response locally`() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getResults().take(2).collect()
        }

        job.cancel()

        val cachedResults = resultSource.getAllByUser(userDto.id)
        if(cachedResults.isNotEmpty()) print("$cachedResults") else print("")
        assertThat(
            cachedResults.map { QuizResultListing.fromEntity(it) },
            containsInAnyOrder(*quizResultDto.map { QuizResultListing.fromDto(it) }.toTypedArray())
        )
    }

    @Test
    fun `getResults should emit a failure when NetworkSource fails`() = runTest {
        val error = NetworkResult.HttpError(401)
        networkSource.failOnNextWith(error)

        val results = mutableListOf<Result<List<QuizListing>, Any?>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getQuizzes().take(2).collect {
                results.add(it)
            }
        }

        job.cancel()
        assertThat(results[0], IsInstanceOf(Result.Success::class.java))
        assertThat(results[1], IsInstanceOf(Result.Unauthorized::class.java))
    }
}