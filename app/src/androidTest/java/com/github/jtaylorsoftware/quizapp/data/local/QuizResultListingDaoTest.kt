package com.github.jtaylorsoftware.quizapp.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)

@MediumTest
class QuizResultListingDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: QuizResultListingDao

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val results = listOf(
        QuizResultListingEntity(
            id = "RESULT1",
            date = "",
            user = "USER1",
            quiz = "QUIZ1",
            score = 1f,
            quizTitle = "",
            createdBy = ""
        ),
        QuizResultListingEntity(
            id = "RESULT2",
            date = "",
            user = "USER2",
            quiz = "QUIZ1",
            score = 1f,
            quizTitle = "",
            createdBy = ""
        ),
        QuizResultListingEntity(
            id = "RESULT3",
            date = "",
            user = "USER1",
            quiz = "QUIZ2",
            score = 1f,
            quizTitle = "",
            createdBy = ""
        ),
        QuizResultListingEntity(
            id = "RESULT4",
            date = "",
            user = "USER2",
            quiz = "QUIZ2",
            score = 1f,
            quizTitle = "",
            createdBy = ""
        ),
        QuizResultListingEntity(
            id = "RESULT5",
            date = "",
            user = "USER3",
            quiz = "QUIZ1",
            score = 1f,
            quizTitle = "",
            createdBy = ""
        )
    )

    init {
        val responses: MutableSet<Pair<String, String>> = mutableSetOf()

        /**
         * Sanity check result data to make sure it resembles realistic data
         * (user can't respond to own quiz, and can't respond to quiz more than once)
         */
        results.forEach {
            check(it.user != it.quiz)
            check(!responses.contains(it.user to it.quiz))
            responses.add(it.user to it.quiz)
        }
    }

    @Before
    fun beforeEach() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.resultListingDao()

        runTest {
            dao.insertAll(results)
        }
    }

    @After
    @Throws(IOException::class)
    fun afterEach() {
        db.close()
    }

    @Test
    fun getById_returnsRow() = runTest {
        val id = "RESULT1"
        val expected = results.first { it.id == id }
        val actual = dao.getById(id)
        MatcherAssert.assertThat(actual, `is`(expected))
    }

    @Test
    fun getAllByUser_returnsRows() = runTest {
        val userId = "USER1"
        val expected = results.filter { it.user == userId }
        val actual = dao.getAllByUser(userId)
        MatcherAssert.assertThat(actual, `is`(expected))
    }

    @Test
    fun getAllByQuiz_returnsRows() = runTest {
        val quizId = "QUIZ1"
        val expected = results.filter { it.quiz == quizId }
        val actual = dao.getAllByQuiz(quizId)
        MatcherAssert.assertThat(actual, `is`(expected))
    }

    @Test
    fun getByQuizAndUser_returnsRow() = runTest {
        val quizId = "QUIZ1"
        val userId = "USER1"
        val expected = results.first { it.quiz == quizId && it.user == userId }
        val actual = dao.getByQuizAndUser(quizId, userId)
        MatcherAssert.assertThat(actual, `is`(expected))
    }

    @Test
    fun deleteAll_removesAllRows() = runTest {
        dao.deleteAll()
        val cursor = db.query("SELECT COUNT(*) FROM result_listing", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        MatcherAssert.assertThat(count, `is`(0))
    }
}