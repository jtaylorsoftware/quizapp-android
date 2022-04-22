package com.github.jtaylorsoftware.quizapp.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.whileSelect
import kotlinx.coroutines.supervisorScope

/**
 * Like [List.any], this returns `true` if any element of this [List] where [predicate] returns `true`.
 *
 * This differs from [List.any] in that [predicate] can run in a coroutine, and also that it maps each
 * element to its own [Deferred] returning the result of [predicate], and cancels all [Deferred] once any
 * of them completes with `true`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> List<T>.anyAsync(predicate: suspend (T) -> Boolean) = supervisorScope {
    map { value ->
        async {
            predicate(value)
        }
    }.let { list ->
        // Wait for the first predicate to complete with true
        var first: Int = -1
        var completed = 0
        whileSelect {
            list.forEachIndexed { i, deferred ->
                deferred.onAwait { hasError ->
                    if (hasError) {
                        first = i
                        return@onAwait false
                    }
                    completed++
                    completed != list.size
                }
            }
        }
        list.forEach {
            it.cancel()
        }
        first != -1
    }
}