package com.github.jtaylorsoftware.quizapp.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * An [Interceptor] that adds a JWT to each request using the
 * "x-auth-token" header.
 *
 * @param getToken A function that can be called to get the current JWT. If it returns null,
 *                 the header won't be included.
 */
class JwtInterceptor(val getToken: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        when (val token = getToken()) {
            null -> proceed(request())
            else -> proceed(request().newBuilder().addHeader("x-auth-token", token).build())
        }
    }
}