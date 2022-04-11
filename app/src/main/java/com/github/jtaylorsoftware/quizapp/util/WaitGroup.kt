package com.github.jtaylorsoftware.quizapp.util

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Allows waiting for a set of coroutines to finish executing. It uses a provided [CoroutineScope] to launch new jobs.
 * The [WaitGroup] is valid as long as [scope] is valid.
 *
 * Adding of jobs can only happen when [wait] has not yet been called on the [WaitGroup].
 *
 * @param scope The parent scope to launch new [Job]s in.
 */
class WaitGroup(private val scope: CoroutineScope) {

    private val _waiting = AtomicBoolean(false)
    val waiting: Boolean
        get() = _waiting.get()

    private val mutex = Mutex()
    private val jobs = mutableSetOf<Job>()

    /**
     * Launches a new [Job] in the context given by [scope] + [context].
     *
     * Calling code is responsible for handling any exceptions thrown within [block].
     *
     * Attempting to call [add] after a call to [wait] will result in an
     * [IllegalStateException]. Callers should check the value [waiting] before
     * calling [add] if it's possible for multiple coroutines to add while another
     * has called wait.
     *
     * @param context Optional CoroutineContext to override the one provided by [scope].
     */
    suspend fun add(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend () -> Unit
    ) {
        check(!_waiting.get()) { "Call to add during wait" }

        val job =
            scope.launch(context) {
                block()
            }
        job.invokeOnCompletion {
            scope.launch(context) {
                try {
                    onDone(job)
                } catch (e: Exception) {
                }
            }
        }

        add(job)
    }

    private suspend fun add(job: Job) = mutex.withLock {
        jobs += job
    }

    private suspend fun onDone(job: Job) = mutex.withLock {
        jobs -= job
    }

    /**
     * Suspends in the calling context and waits for all current [Job]s to complete.
     * See [add] for limitations on ordering between calls to [add] and [wait].
     *
     * If a timeout is specified and waiting times out before all Jobs have completed, the [WaitGroup]
     * will still hold references to these uncompleted Jobs. Successive calls to [wait] wait for both
     * these uncompleted jobs and any new jobs added since the last wait with [add].
     *
     * [wait] throws [TimeoutCancellationException] when it times out.
     *
     * @param timeout Optional timeout, after which this [WaitGroup] will stop waiting for any current jobs
     * to finish.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun wait(
        timeout: Duration = Duration.INFINITE,
    ) {
        if (!scope.isActive) {
            throw CancellationException("WaitGroup.scope has been cancelled")
        }

        check(_waiting.compareAndSet(false, true)) {
            "Call to wait before previous wait finished"
        }

        try {
            withTimeout(timeout) {
                while (isActive) {
                    yield()
                    val isDone = mutex.withLock { jobs.isEmpty() }
                    if (isDone) {
                        return@withTimeout
                    }
                }
            }
        } finally {
            check(_waiting.compareAndSet(true, false)) {
                "waiting was unexpectedly false at end of wait"
            }
        }
    }
}