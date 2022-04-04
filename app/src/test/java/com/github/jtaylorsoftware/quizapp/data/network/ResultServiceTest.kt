package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.network.dto.GradedAnswerDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionResponseDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizFormResponsesDto
import com.github.jtaylorsoftware.quizapp.matchers.SameResultAs
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
class ResultServiceTest {
    private val moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(GradedAnswerDto::class.java, "type")
                .withSubtype(
                    GradedAnswerDto.MultipleChoice::class.java,
                    QuestionType.MultipleChoice.name
                )
                .withSubtype(GradedAnswerDto.FillIn::class.java, QuestionType.FillIn.name)
        )
        .add(
            PolymorphicJsonAdapterFactory.of(QuestionResponseDto::class.java, "type")
                .withSubtype(
                    QuestionResponseDto.MultipleChoice::class.java,
                    QuestionType.MultipleChoice.name
                )
                .withSubtype(QuestionResponseDto.FillIn::class.java, QuestionType.FillIn.name)
        )
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    private lateinit var resultService: QuizResultService
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

        resultService = retrofit.create(QuizResultService::class.java)
    }

    @After
    fun afterEach() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getForQuizByUser should correctly parse a ResultDto from response`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(TestData.resultJson)
        )

        when (val result = resultService.getForQuizByUser("Q1", "U1")) {
            is NetworkResult.Success -> assertThat(
                result.value,
                `is`(SameResultAs(TestData.resultDto))
            )
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }

    @Test
    fun `getListingForQuizByUser should correctly parse a ResultListingDto from response`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(TestData.resultListingJson)
            )

            when (val result = resultService.getListingForQuizByUser("Q1", "U1")) {
                is NetworkResult.Success -> assertThat(
                    result.value,
                    `is`(TestData.resultListingDto)
                )
                else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
            }
        }

    @Test
    fun `createResultForQuiz should serialize a QuizFormResponsesDto`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(201).setBody(TestData.objectIdResponseJson)
        )

        val responses = QuizFormResponsesDto(
            answers = listOf(
                QuestionResponseDto.FillIn(answer = "Answer"),
                QuestionResponseDto.MultipleChoice(choice = 1)
            )
        )

        // Should not throw when serializing, should also be success
        when (val result = resultService.createResultForQuiz(responses, "Q1")) {
            is NetworkResult.Success -> assertThat(result.value, `is`(TestData.objectIdResponse))
            else -> throw IllegalStateException("Expected NetworkResult.Success, got $result")
        }
    }
}