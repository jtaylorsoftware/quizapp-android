package com.github.jtaylorsoftware.quizapp.auth

import io.mockk.confirmVerified
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

class JwtInterceptorTest {
    interface TestService {
        @GET("/")
        fun get(): Call<ResponseBody>
    }

    private var token: String? = null
    private lateinit var okHttpClient: OkHttpClient

    private lateinit var mockWebServer: MockWebServer
    private lateinit var testService: TestService

    private lateinit var onUnauthorized: () -> Unit

    @Before
    fun beforeEach() {
        token = null
        mockWebServer = MockWebServer()
        mockWebServer.start()

        onUnauthorized = mockk()

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(JwtInterceptor(this::token, onUnauthorized))
            .build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(mockWebServer.url("/"))
            .build()
        testService = retrofit.create(TestService::class.java)
    }

    @After
    fun afterEach() {
        mockWebServer.shutdown()
    }

    @Test
    fun `should add x-auth-token header when token is not null`() {
        token = "adf0ds9fsdksjfds071231adf"
        mockWebServer.enqueue(MockResponse().setBody("OK"))
        testService.get().execute()
        val request = mockWebServer.takeRequest()
        assertThat(request.getHeader("x-auth-token"), `is`(token))
    }

    @Test
    fun `should not add x-auth-token header when token is null`() {
        mockWebServer.enqueue(MockResponse().setBody("UNAUTHORIZED"))
        testService.get().execute()
        val request = mockWebServer.takeRequest()
        assertThat(request.getHeader("x-auth-token"), `is`(nullValue()))
    }

    @Test
    fun `should call onUnauthorized when a response returns 401 and token was in request`() {
        justRun { onUnauthorized() }

        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        testService.get().execute()

        verify(exactly = 1) {
            onUnauthorized()
        }
        confirmVerified(onUnauthorized)
    }

    @Test
    fun `should call onUnAuthorized when a response returns 401 and no token was in request`() {
        justRun { onUnauthorized() }
        token = null
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        testService.get().execute()

        verify(exactly = 1) {
            onUnauthorized()
        }
        confirmVerified(onUnauthorized)
    }
}