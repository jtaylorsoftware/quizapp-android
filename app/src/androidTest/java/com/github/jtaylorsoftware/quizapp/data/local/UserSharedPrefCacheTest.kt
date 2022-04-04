package com.github.jtaylorsoftware.quizapp.data.local

import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity
import org.hamcrest.Description
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Test


@MediumTest
class UserSharedPrefCacheTest {
    private lateinit var userCache: UserCache

    @Before
    fun beforeEach() {
        userCache = UserSharedPrefCache(
            InstrumentationRegistry.getInstrumentation().context
        )
    }

    @Test
    fun saveAndLoadUser() {
        val user = UserEntity(
            id = "user123",
            date = "date",
            username = "username",
            email = "useremail@email.com",
            quizzes = listOf("quiz123", "quiz456"),
            results = listOf("results123", "results456")
        )

        // Should be able to save and get same user out
        userCache.saveUser(user)
        val savedUser = userCache.loadUser()

        assertThat(savedUser, `is`(SameUserAs(user)))
    }

    @Test
    fun saveAndLoadToken() {
        val token = "10sdflkjz013lkjdsflkjksfdmcxv0213"

        // Should be able to save and get same token out
        userCache.saveToken(token)
        val savedToken = userCache.loadToken()

        assertThat(savedToken, `is`(token))
    }

    @Test
    fun clearUser() {
        val user = UserEntity(
            id = "user123",
            date = "date",
            username = "username",
            email = "useremail@email.com",
            quizzes = listOf("quiz123", "quiz456"),
            results = listOf("results123", "results456")
        )

        // Should be able to save, clear user, and next load is null
        userCache.saveUser(user)
        assertThat(userCache.loadUser(), `is`(SameUserAs(user)))

        userCache.clearUser()
        assertThat(userCache.loadUser(), `is`(nullValue()))
    }
}

private class SameUserAs(private val expected: UserEntity?) : TypeSafeMatcher<UserEntity>() {
    override fun describeTo(description: Description?) {
        description?.appendText("equal to in any order: $expected")
    }

    override fun matchesSafely(actual: UserEntity?): Boolean {
        return actual === expected ||
                actual != null &&
                expected != null &&
                actual.id == expected.id &&
                actual.date == expected.date &&
                actual.username == expected.username &&
                actual.email == expected.email &&
                listsMatchInAnyOrder(actual, expected)
    }

    private fun listsMatchInAnyOrder(actual: UserEntity, expected: UserEntity): Boolean =
        containsInAnyOrder(*expected.quizzes.toTypedArray()).matches(actual.quizzes) &&
                containsInAnyOrder(*expected.results.toTypedArray()).matches(actual.results)

}