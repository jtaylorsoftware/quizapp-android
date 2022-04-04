package com.github.jtaylorsoftware.quizapp.data.network

import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiError
import com.github.jtaylorsoftware.quizapp.data.network.dto.ApiErrorResponse
import okhttp3.Request
import okhttp3.ResponseBody
import okio.IOException
import okio.Timeout
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Encapsulates a network request that could potentially fail.
 * Failure states may be either a problem in the network itself, or a failure of
 * an API call.
 */
sealed interface NetworkResult<out T> {
    sealed interface Success<T> : NetworkResult<T> {
        val value: T
    }

    data class HttpError(val code: Int, val errors: List<ApiError> = emptyList()) :
        NetworkResult<Nothing>

    data class NetworkError(val throwable: Throwable) : NetworkResult<Nothing>
    data class Unknown(val throwable: Throwable) : NetworkResult<Nothing>

    companion object {
        fun <T> success(value: T): Success<T> = SuccessImpl(value)
        fun success(): Success<Unit> = EmptySuccess
    }
}

private data class SuccessImpl<T>(
    override val value: T
) : NetworkResult.Success<T>

private object EmptySuccess : NetworkResult.Success<Unit> {
    override val value = Unit
}

/**
 * Call transformer for NetworkResult<T> that assumes error bodies are always
 * ApiErrorResponse as per the API specification.
 */
private class NetworkResultCall<T>(
    private val delegate: Call<T>, // the original Call that would normally return one type
    private val apiErrorConverter: Converter<ResponseBody, ApiErrorResponse>,
    private val successType: Type
) : Call<NetworkResult<T>> {
    override fun enqueue(callback: Callback<NetworkResult<T>>) {
        return delegate.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    when {
                        successType == Unit::class.java -> {
                            // Response did not include a body, which may be allowed
                            // when T is Unit
                            @Suppress("UNCHECKED_CAST")
                            callback.onResponse(
                                this@NetworkResultCall,
                                Response.success(NetworkResult.success() as NetworkResult<T>)
                            )
                        }
                        body != null -> {
                            callback.onResponse(
                                this@NetworkResultCall,
                                Response.success(NetworkResult.success(body))
                            )
                        }
                        else -> {
                            // Expected a body but did not get one
                            callback.onResponse(
                                this@NetworkResultCall,
                                Response.success(
                                    NetworkResult.Unknown(IllegalStateException("Expected response body, but was null"))
                                )
                            )
                        }
                    }
                } else {
                    val errorResponse = response.errorBody()?.let {
                        try {
                            apiErrorConverter.convert(it)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val failure = NetworkResult.HttpError(
                        response.code(),
                        errorResponse?.errors ?: emptyList()
                    )

                    callback.onResponse(
                        this@NetworkResultCall,
                        Response.success(failure)
                    )
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                val failure = when (t) {
                    is IOException -> NetworkResult.NetworkError(t)
                    else -> NetworkResult.Unknown(t)
                }
                callback.onResponse(
                    this@NetworkResultCall,
                    Response.success(failure)
                )
            }
        })
    }

    override fun execute(): Response<NetworkResult<T>> {
        throw UnsupportedOperationException("NetworkResultCall doesn't support execute")
    }

    override fun clone(): Call<NetworkResult<T>> =
        NetworkResultCall(delegate.clone(), apiErrorConverter, successType)

    override fun isExecuted(): Boolean = delegate.isExecuted
    override fun cancel() = delegate.cancel()
    override fun isCanceled(): Boolean = delegate.isCanceled
    override fun request(): Request = delegate.request()
    override fun timeout(): Timeout = delegate.timeout()
}

/**
 * Result adapter for NetworkResult<T> that assumes error bodies are always
 * ApiErrorResponse as per the API specification.
 */
private class NetworkResultAdapter<T>(
    private val successType: Type,
    private val apiErrorConverter: Converter<ResponseBody, ApiErrorResponse>
) : CallAdapter<T, Call<NetworkResult<T>>> {
    override fun responseType(): Type = successType

    override fun adapt(call: Call<T>): Call<NetworkResult<T>> {
        return NetworkResultCall(call, apiErrorConverter, successType)
    }
}

/**
 * Factory for NetworkResultAdapter that checks if the raw return type of the
 * Call is NetworkResult.
 */
class NetworkResultAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (Call::class.java != getRawType(returnType)) {
            return null
        }

        // Ensure that outermost `returnType` is a generic Call type with type argument
        require(returnType is ParameterizedType) {
            "return must be Call<T>"
        }

        val responseType = getParameterUpperBound(0, returnType)
        if (getRawType(responseType) != NetworkResult::class.java) {
            return null
        }

        // Ensure that the nested NetworkResult has a type argument
        require(responseType is ParameterizedType) {
            "T of Call<T> must be NetworkResult<R>"
        }

        // Get the desired success type T from NetworkResult<T>
        val successType = getParameterUpperBound(0, responseType)

        val apiErrorConverter =
            retrofit.nextResponseBodyConverter<ApiErrorResponse>(
                null,
                ApiErrorResponse::class.java,
                annotations
            )

        return NetworkResultAdapter<Any>(successType, apiErrorConverter)
    }
}