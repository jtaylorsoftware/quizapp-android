package com.github.jtaylorsoftware.quizapp.data.domain

import com.github.jtaylorsoftware.quizapp.data.domain.models.UserCredentials
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserRegistration
import com.github.jtaylorsoftware.quizapp.data.local.FakeUserCache
import com.github.jtaylorsoftware.quizapp.data.network.FakeUserNetworkSource
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResult
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.core.IsInstanceOf
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserAuthServiceTest {
    private lateinit var cache: FakeUserCache
    private lateinit var networkSource: FakeUserNetworkSource
    private lateinit var service: UserAuthService

    @Before
    fun beforeEach() {
        cache = FakeUserCache()
        networkSource = FakeUserNetworkSource()
        service = UserAuthServiceImpl(cache, networkSource)
    }

    @Test
    fun `userIsSignedIn returns true when JWT is valid`() = runTest {
        cache.saveToken("TOKEN")
        assertThat((service.userIsSignedIn() as Result.Success).value, `is`(true))
    }

    @Test
    fun `registerUser should store the returned auth token from network`() = runTest {
        val reg = UserRegistration("username", "email@email.com", "password")
        val result = service.registerUser(reg)

        assertThat(result, IsInstanceOf(Result.Success::class.java))
        assertThat(cache.loadToken(), `is`(notNullValue()))
    }

    @Test
    fun `registerUser should fail with errors when NetworkSource fails`() = runTest {
        val message = "Invalid"
        val error = NetworkResult.HttpError(
            400, listOf(
                ApiError(field = "username", value = "username", message = message),
                ApiError(field = "email", value = "email@email.com", message = message),
                ApiError(field = "password", message = message)
            )
        )
        networkSource.failOnNextWith(error)

        val reg = UserRegistration("username", "email@email.com", "password")
        val result = service.registerUser(reg)

        assertThat(result, IsInstanceOf(Result.BadRequest::class.java))
        assertThat(
            (result as Result.BadRequest).error,
            `is`(UserRegistrationErrors(username = message, email = message, password = message))
        )
    }

    @Test
    fun `signInUser should store the returned auth token from network`() = runTest {
        val creds = UserCredentials("username", "password")
        val result = service.signInUser(creds)

        assertThat(result, IsInstanceOf(Result.Success::class.java))
        assertThat(cache.loadToken(), `is`(notNullValue()))
    }

    @Test
    fun `signInUser should fail with errors when NetworkSource fails`() = runTest {
        val message = "Invalid"
        val error = NetworkResult.HttpError(
            400, listOf(
                ApiError(field = "username", value = "username", message = message),
                ApiError(field = "password", message = message)
            )
        )
        networkSource.failOnNextWith(error)

        val creds = UserCredentials("username", "password")
        val result = service.signInUser(creds)

        assertThat(result, IsInstanceOf(Result.BadRequest::class.java))
        assertThat(
            (result as Result.BadRequest).error,
            `is`(UserCredentialErrors(username = message, password = message))
        )
    }

    @Test
    fun `changeEmail should fail when NetworkSource fails`() = runTest {
        val message = "Invalid"
        val error = NetworkResult.HttpError(
            400, listOf(
                ApiError(field = "email", value = "email@email.com", message = message),
            )
        )
        networkSource.failOnNextWith(error)

        val result = service.changeEmail("email@email.com")
        assertThat(result, IsInstanceOf(Result.BadRequest::class.java))
        assertThat(
            (result as Result.BadRequest).error,
            `is`(message)
        )
    }

    @Test
    fun `changePassword should fail when NetworkSource fails`() = runTest {
        val message = "Invalid"
        val error = NetworkResult.HttpError(
            400, listOf(
                ApiError(field = "password", message = message),
            )
        )
        networkSource.failOnNextWith(error)

        val result = service.changePassword("password")
        assertThat(result, IsInstanceOf(Result.BadRequest::class.java))
        assertThat(
            (result as Result.BadRequest).error,
            `is`(message)
        )
    }
}