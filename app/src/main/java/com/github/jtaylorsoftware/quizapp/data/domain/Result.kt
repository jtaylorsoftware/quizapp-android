package com.github.jtaylorsoftware.quizapp.data.domain

/**
 * Encapsulates a method result that may succeed with
 * a value of type `T`, or fail with or without errors.
 *
 * Some types will resemble HTTP status codes where
 * nothing more specific is appropriate.
 */
sealed interface Result<out T, out E> {
    sealed interface Success<T> : Result<T, Nothing> {
        val value: T
    }

    data class BadRequest<E>(val error: E) : Result<Nothing, E>

    data class Conflict<E>(val error: E) : Result<Nothing, E>

    object NotFound : Result<Nothing, Nothing>

    object Unauthorized : Result<Nothing, Nothing>

    object Forbidden : Result<Nothing, Nothing>

    object Expired : Result<Nothing, Nothing>

    object NetworkError : Result<Nothing, Nothing>

    object UnknownError : Result<Nothing, Nothing>

    companion object {
        fun success(): Success<Unit> = EmptySuccess

        // Provided for symmetry with `success()`
        fun <T> success(value: T): Success<T> = SuccessImpl(value)
    }
}

private data class SuccessImpl<T>(
    override val value: T
) : Result.Success<T>

private object EmptySuccess : Result.Success<Unit> {
    override val value = Unit
}
