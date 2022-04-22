package com.github.jtaylorsoftware.quizapp.di

import com.github.jtaylorsoftware.quizapp.auth.AuthenticationEventProducer
import com.github.jtaylorsoftware.quizapp.auth.JwtInterceptor
import com.github.jtaylorsoftware.quizapp.data.QuestionType
import com.github.jtaylorsoftware.quizapp.data.local.UserCache
import com.github.jtaylorsoftware.quizapp.data.network.NetworkResultAdapterFactory
import com.github.jtaylorsoftware.quizapp.data.network.QuizResultService
import com.github.jtaylorsoftware.quizapp.data.network.QuizService
import com.github.jtaylorsoftware.quizapp.data.network.UserService
import com.github.jtaylorsoftware.quizapp.data.network.dto.GradedAnswerDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuestionResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    @Provides
    @Singleton
    fun provideRetrofit(
        userCache: UserCache,
        authenticationEventProducer: AuthenticationEventProducer,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .client(createHttpClient(userCache, authenticationEventProducer))
        .baseUrl("http://www.makequizzes.online/api/v2/")
        .addCallAdapterFactory(NetworkResultAdapterFactory())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideQuizService(retrofit: Retrofit): QuizService =
        retrofit.create(QuizService::class.java)

    @Provides
    @Singleton
    fun provideResultService(retrofit: Retrofit): QuizResultService =
        retrofit.create(QuizResultService::class.java)

    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService =
        retrofit.create(UserService::class.java)

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(QuestionDto::class.java, "type")
                .withSubtype(
                    QuestionDto.MultipleChoice::class.java,
                    QuestionType.MultipleChoice.name
                )
                .withSubtype(QuestionDto.FillIn::class.java, QuestionType.FillIn.name)
        )
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

    private fun createHttpClient(
        userCache: UserCache,
        authenticationEventProducer: AuthenticationEventProducer
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            JwtInterceptor(
                getToken = userCache::loadToken,
                onUnauthorized = {
                    userCache.clearToken()
                    authenticationEventProducer.onRequireLogIn()
                }
            )
        )
        .build()
}