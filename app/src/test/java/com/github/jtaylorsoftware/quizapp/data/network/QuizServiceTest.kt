package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionDto
import com.github.jtaylorsoftware.quizapp.matchers.SameQuizAs
import com.github.jtaylorsoftware.quizapp.matchers.SameQuizFormAs
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
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
class QuizServiceTest {
    private val moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(QuestionDto::class.java, "type")
                .withSubtype(
                    QuestionDto.MultipleChoice::class.java,
                    QuestionType.MultipleChoice.name
                )
                .withSubtype(QuestionDto.FillIn::class.java, QuestionType.FillIn.name)
        )
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    private lateinit var quizService: QuizService
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

        quizService = retrofit.create(QuizService::class.java)
    }

    @After
    fun afterEach() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getById should correctly parse a QuizDto from response`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.quizJson)
        )

        when (val result = quizService.getById("Q1")) {
            is NetworkResult.Success -> assertThat(
                result.value,
                `is`(SameQuizAs(TestData.quizDto))
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `getListingById should correctly parse a QuizListingDto from response`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.quizListingJson)
        )

        when (val result =  quizService.getListingById("Q1")) {
            is NetworkResult.Success -> assertThat(result.value, `is`(TestData.quizListingDto))
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `getForm should correctly parse a QuizFormDto from response`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.quizFormJson)
        )

        when (val result =  quizService.getForm("Q1")) {
            is NetworkResult.Success -> assertThat(result.value, `is`(SameQuizFormAs(TestData.quizFormDto)))
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `createQuiz should be able to serialize QuizDto`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(201).setBody(TestData.objectIdResponseJson)
        )

        // Should not throw when serializing
        when(val result = quizService.createQuiz(TestData.quizDto)){
            is NetworkResult.Success -> assertThat(result.value, `is`(TestData.objectIdResponse))
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `updateQuiz should be able to serialize QuizDto`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(204)
        )

        // Should not throw when serializing
        when(val result = quizService.updateQuiz("Q1", TestData.quizDto)){
            is NetworkResult.Success -> { /** PASS **/ }
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }
}
