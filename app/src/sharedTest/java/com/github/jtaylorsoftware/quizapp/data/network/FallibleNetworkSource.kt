package com.github.jtaylorsoftware.quizapp.data.network

/**
 * Provides a way to control how fake NetworkSources will fail requests.
 */
open class FallibleNetworkSource {
    private var _nextFailure: NetworkResult<Nothing>? = null
    protected val failOnNext: NetworkResult<Nothing>?
        get() = if (_nextFailure != null) {
            val error = _nextFailure
            _nextFailure = null
            error
        } else null

    /**
     * Makes the next method call guaranteed to fail with an error.
     * Any calls after the next one will not fail unless this method is called again.
     */
    fun failOnNextWith(error: NetworkResult<Nothing>) {
        _nextFailure = error
    }
}