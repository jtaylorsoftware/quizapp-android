package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.UserCredentialsDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserRegistrationDto
import com.github.jtaylorsoftware.quizapp.matchers.SameUserDtoAs
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class UserServiceTest {
    private val moshi = Moshi.Builder().build()
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    private lateinit var service: UserService
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun beforeEach() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(mockWebServer.url("/"))
            .addCallAdapterFactory(NetworkResultAdapterFactory())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        service = retrofit.create(UserService::class.java)
    }

    @After
    fun afterEach() {
        mockWebServer.shutdown()
    }

    @Test
    fun `registerUser should serialize UserRegistrationDto and deserialize AuthToken`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.tokenResponseJson)
        )

        when (
            val result = service.registerUser(UserRegistrationDto("username", "email", "password"))
        ) {
            is NetworkResult.Success -> assertThat(
                result.value,
                `is`(TestData.tokenResponse)
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `signInUser should serialize UserCredentialsDto and deserialize AuthToken`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.tokenResponseJson)
        )

        when (
            val result = service.signInUser(UserCredentialsDto("username", "password"))
        ) {
            is NetworkResult.Success -> assertThat(
                result.value,
                `is`(TestData.tokenResponse)
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `getProfile should deserialize UserDto`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.userJson)
        )

        when (val result = service.getProfile()) {
            is NetworkResult.Success -> assertThat(
                result.value,
                `is`(SameUserDtoAs(TestData.userDto))
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `getQuizzes should deserialize list of QuizListingDto`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.quizListingListJson)
        )

        when (val result = service.getQuizzes()) {
            is NetworkResult.Success -> assertThat(
                result.value[0],
                `is`(TestData.quizListingList[0])
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `getResults should deserialize list of ResultListingDto`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.resultListingListJson)
        )

        when (val result = service.getResults()) {
            is NetworkResult.Success -> assertThat(
                result.value[0],
                `is`(TestData.resultListingList[0])
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }
}