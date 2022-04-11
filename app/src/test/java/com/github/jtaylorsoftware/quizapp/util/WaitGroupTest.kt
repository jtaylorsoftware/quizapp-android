package com.github.jtaylorsoftware.quizapp.util

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalCoroutinesApi
class WaitGroupTest {
    private lateinit var waitGroup: WaitGroup

    @Before
    fun beforeEach() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        waitGroup = WaitGroup(TestScope(StandardTestDispatcher(scheduler)))
    }

    @Before
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test(expected = IllegalStateException::class)
    fun `add should throw when already waiting`() = runTest(dispatchTimeoutMs = 300) {
        waitGroup.add(Dispatchers.Default) {
            while(true) {
                yield()
            }
        }

        launch(Dispatchers.Default) {
            waitGroup.wait()
        }

        // Call add after WaitGroup has started waiting
        while (isActive) {
            if (waitGroup.waiting) {
                waitGroup.add {}
                break
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `wait should throw when already waiting`() = runTest(dispatchTimeoutMs = 300) {
        waitGroup.add(Dispatchers.Default) {
            while (true) {
                yield()
            }
        }

        launch(Dispatchers.Default) {
            waitGroup.wait()
        }

        // Call wait after WaitGroup has started waiting
        while (isActive) {
            if (waitGroup.waiting) {
                waitGroup.wait()
                break
            }
        }
    }

    @Test
    fun `waiting should return true after call to wait`() = runTest(dispatchTimeoutMs = 150) {
        waitGroup.add(Dispatchers.Default) {
            while (true) {
                yield()
            }
        }

        val job = launch(Dispatchers.Default) {
            waitGroup.wait()
        }

        // Call wait after WaitGroup has started waiting
        while (isActive) {
            if (waitGroup.waiting) {
                break
            }
        }

        job.cancelAndJoin()
    }

    @Test
    fun `wait should eventually finish waiting for all jobs`() = runTest(dispatchTimeoutMs = 150) {
        repeat(3) {
            waitGroup.add {
                delay(100)
            }
        }

        val job = launch(Dispatchers.Default) {
            waitGroup.wait()
        }

        // Ensure actually waiting before making jobs finish
        while (isActive) {
            if (waitGroup.waiting) {
                break
            }
        }

        // Make jobs finish
        advanceUntilIdle()

        job.join()
        assertThat(waitGroup.waiting, `is`(false))
    }

    @Test(expected = TimeoutCancellationException::class)
    fun `wait with timeout should stop waiting after timeout`() = runTest(dispatchTimeoutMs = 250) {
        waitGroup.add(Dispatchers.Default) {
            while(true) {
                yield()
            }
        }
        try {
            withContext(Dispatchers.Default) { // switch to another thread to stop from blocking test
                waitGroup.wait(timeout = 50.milliseconds)
            }
        } catch (e: TimeoutCancellationException) {
            throw e
        }
    }

    @Test
    fun `wait should be reusable after previous timeout`() = runTest(dispatchTimeoutMs = 500) {
        val finished = AtomicBoolean(false)
        waitGroup.add(Dispatchers.Default) {
            delay(500)
            finished.set(true)
        }

        try {
            withContext(Dispatchers.Default) {
                waitGroup.wait(timeout = 50.milliseconds)
            }
        } catch (e: TimeoutCancellationException) {
            // Make sure that the work added hasn't finished yet
            assertThat(finished.get(), `is`(false))
        }

        withContext(Dispatchers.Default)  {
            waitGroup.wait()
        }

        assertThat(finished.get(), `is`(true))
    }
}