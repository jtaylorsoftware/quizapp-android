package com.github.jtaylorsoftware.quizapp.auth

import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * An [Interceptor] that adds a JWT to each request using the "x-auth-token" header.
 * On responses that include a token and that return HTTP 401, it
 * will remove the token by calling [onUnauthorized].
 *
 * @param getToken A function that can be called to get the current JWT. If it returns null,
 * the header won't be included.
 *
 * @param onUnauthorized Function called when a response returns 401 Unauthorized.
 */
class JwtInterceptor(
    val getToken: () -> String?,
    val onUnauthorized: () -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        val response = when (val token = getToken()) {
            null -> proceed(request())
            else -> proceed(request().newBuilder().addHeader("x-auth-token", token).build())
        }
        if (response.code() == HTTP_UNAUTHORIZED) {
            onUnauthorized()
        }
        response
    }
}