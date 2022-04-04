package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiErrorResponse
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

@OptIn(ExperimentalCoroutinesApi::class)
class ApiErrorAdapterTest {
    private val moshi = Moshi.Builder().build()
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    interface TestService {
        @GET("/")
        suspend fun get(): ApiErrorResponse
    }

    private lateinit var mockWebServer: MockWebServer
    private lateinit var testService: TestService

    @Before
    fun beforeEach() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)

        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Test by passing a response through retrofit instance using a moshi
        // instance that uses codegen
        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(mockWebServer.url("/"))
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
    fun `moshi can parse ApiErrorResponse correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody(TestData.apiErrorsJson)
        )

        val errors = testService.get().errors
        assertThat(
            errors,
            containsInAnyOrder(*TestData.apiErrorsDto.errors.toTypedArray())
        )
    }
}