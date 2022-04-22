package com.github.jtaylorsoftware.quizapp.data.domain

/**
 * Encapsulates a method result that may succeed with
 * a value of type `T`, or fail with or without errors.
 */
sealed interface Result<out T, out E> {
    sealed interface Success<T> : Result<T, Nothing> {
        val value: T
    }

    data class Failure<E>(val reason: FailureReason, val errors: E? = null): Result<Nothing, E>

    companion object {
        fun success(): Success<Unit> = EmptySuccess

        // Provided for symmetry with `success()`
        fun <T> success(value: T): Success<T> = SuccessImpl(value)
        fun <E> failure(reason: FailureReason, errors: E): Result<Nothing, E> = Failure(reason, errors)
        fun failure(reason: FailureReason): ResultOrFailure<Nothing> = Failure(reason, null)
    }
}

/**
 * A [Result] where failures happen without any known `errors` structure.
 */
typealias ResultOrFailure<T> = Result<T, Nothing>

private data class SuccessImpl<T>(
    override val value: T
) : Result.Success<T>

private object EmptySuccess : Result.Success<Unit> {
    override val value = Unit
}