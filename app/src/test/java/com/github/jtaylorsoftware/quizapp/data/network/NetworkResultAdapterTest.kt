package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.ObjectIdResponse
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizListingDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkResultAdapterTest {
    private val moshi = Moshi.Builder().build()
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    interface TestService {
        @GET("/unit")
        suspend fun unit(): NetworkResult<Unit> // no body is expected

        @GET("/small")
        suspend fun small(): NetworkResult<ObjectIdResponse> // return small json data

        @GET("/large")
        suspend fun large(): NetworkResult<QuizListingDto> // return large json data
    }

    private lateinit var mockWebServer: MockWebServer
    private lateinit var testService: TestService

    @Before
    fun beforeEach() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)

        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Test by passing a response through retrofit while using NetworkAdapterFactory
        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(mockWebServer.url("/"))
            .addCallAdapterFactory(NetworkResultAdapterFactory())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        testService = retrofit.create(TestService::class.java)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        mockWebServer.shutdown()
    }

    @Test
    fun `result should be Success with value during normal execution`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(201).setBody(TestData.objectIdResponseJson)
        )

        when (val result = testService.small()) {
            is NetworkResult.Success -> assertThat(
                result.value,
                `is`(TestData.objectIdResponse)
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `result should be Success with Unit value when response type is NetworkResult of Unit`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200)
            )

            when (val result = testService.unit()) {
                is NetworkResult.Success -> assertThat(
                    result.value,
                    `is`(Unit)
                )
                else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
            }
        }

    @Test
    fun `result should be Success with Unit, even when body exists, if response type is NetworkResult of Unit`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(TestData.apiErrorsJson)
            )

            when (val result = testService.unit()) {
                is NetworkResult.Success -> assertThat(
                    result.value,
                    `is`(Unit)
                )
                else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
            }
        }

    @Test
    fun `result should be Failure with reason NetworkError when expected body and got nothing`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(201)
            )

            val result = testService.small()
            if (result !is NetworkResult.NetworkError) {
                throw IllegalStateException("Expected NetworkResult.NetworkError, got $result")
            }
        }

    @Test
    fun `result should be Failure with reason Unknown when expected body and got the wrong body type`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(201).setBody(TestData.apiErrorsJson)
            )

            // response should be ObjectIdResponse, but will be sent the JSON for an ApiErrorResponse
            val result = testService.small()
            if (result !is NetworkResult.Unknown) {
                throw IllegalStateException("Expected NetworkResult.Unknown, got $result")
            }
        }

    @Test
    fun `result should be Failure with reason NetworkError when expected body and a response of empty string`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(201).setBody("")
            )
            val result = testService.small()
            if (result !is NetworkResult.NetworkError) {
                throw IllegalStateException("Expected NetworkResult.NetworkError, got $result")
            }
        }

    @Test
    fun `result should be Failure with reason NetworkError when unexpected network error occurs`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(TestData.quizListingJson)
                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
            )
            val result = testService.large()
            if (result !is NetworkResult.NetworkError) {
                throw IllegalStateException("Expected NetworkResult.NetworkError, got $result")
            }
        }

    @Test
    fun `result should be Failure with API errors when HTTP response has error code`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(400).setBody(TestData.apiErrorsJson)
            )

            when (val result = testService.large()) {
                is NetworkResult.HttpError -> {
                    assertThat(
                        result.errors,
                        Matchers.containsInAnyOrder(*TestData.apiErrorsDto.errors.toTypedArray())
                    )
                }
                else -> throw IllegalStateException("Expected NetworkResult.HttpError, got $result")
            }
        }
}