package com.github.jtaylorsoftware.quizapp.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)

@MediumTest
class QuizListingDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: QuizListingDao

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val quizzes = listOf(
        QuizListingEntity(
            id = "QUIZ123",
            date = "",
            user = "USER123",
            title = "",
            expiration = "",
            isPublic = true,
            resultsCount = 0,
            questionCount = 2,
        ),
        QuizListingEntity(
            id = "QUIZ456",
            date = "",
            user = "USER123",
            title = "",
            expiration = "",
            isPublic = true,
            resultsCount = 0,
            questionCount = 2,
        ),
        QuizListingEntity(
            id = "QUIZ789",
            date = "",
            user = "USER456",
            title = "",
            expiration = "",
            isPublic = true,
            resultsCount = 0,
            questionCount = 2,
        )
    )

    @Before
    fun beforeEach() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.quizListingDao()

        runTest {
            dao.insertAll(quizzes)
        }
    }

    @After
    @Throws(IOException::class)
    fun afterEach() {
        db.close()
    }

    @Test
    fun getById_returnsRow() = runTest {
        val id = "QUIZ123"
        val expected = quizzes.first { it.id == id }
        val actual = dao.getById(id)
        assertThat(actual, `is`(expected))
    }

    @Test
    fun getAllCreatedByUser_returnsRows() = runTest {
        // Test with a user id that has actually been used
        val user = "USER123"
        val expected = quizzes.filter { it.user == user }
        val actual = dao.getAllCreatedByUser(user)
        assertThat(actual, `is`(expected))
    }

    @Test
    fun deleteAll_removesAllRows() = runTest {
        dao.deleteAll()
        val cursor = db.query("SELECT COUNT(*) FROM quiz_listing", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        assertThat(count, `is`(0))
    }
}