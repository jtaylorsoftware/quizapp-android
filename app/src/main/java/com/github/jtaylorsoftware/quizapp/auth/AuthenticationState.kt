package com.github.jtaylorsoftware.quizapp.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Encapsulates logic for producing authentication events to bubble up to the top of the app.
 */
interface AuthenticationEventProducer {
    /**
     * Event indicating that the user is now authenticated.
     */
    fun onAuthenticated()

    /**
     * Event indicating that the user is now unauthenticated.
     */
    fun onRequireLogIn()
}

/**
 * Implementation of [AuthenticationEventProducer] that also produces state, acting as a hub
 * for the app's authentication state. Should be considered internal and only instantiatable
 * through Hilt injection. The [stateSource] used must only ever be used as a [MutableAuthenticationStateSource]
 * in the one [AuthenticationEventProducer] associated with it.
 */
class AuthenticationStateManager(
    private val stateSource: MutableAuthenticationStateSource = mutableAuthenticationStateSource()
) : AuthenticationEventProducer, AuthenticationStateSource {
    override val state: AuthenticationState
        get() = stateSource.state

    override fun onAuthenticated() {
        stateSource.state = AuthenticationState.Authenticated
    }

    override fun onRequireLogIn() {
        stateSource.state = AuthenticationState.RequireAuthentication
    }
}

/**
 * Models authentication state for cross-cutting authentication concerns.
 */
sealed interface AuthenticationState {
    object Authenticated : AuthenticationState
    object RequireAuthentication : AuthenticationState
}

/**
 * Provides the a source of [AuthenticationState] for the app.
 */
interface AuthenticationStateSource {
    val state: AuthenticationState
}

/**
 * A mutable [AuthenticationStateSource].
 */
interface MutableAuthenticationStateSource : AuthenticationStateSource {
    override var state: AuthenticationState
}

/**
 * Creates a [MutableAuthenticationStateSource].
 */
fun mutableAuthenticationStateSource(
    initialState: AuthenticationState = AuthenticationState.RequireAuthentication
): MutableAuthenticationStateSource = MutableAuthenticationStateSourceImpl(initialState)

internal class MutableAuthenticationStateSourceImpl(initialState: AuthenticationState) :
    MutableAuthenticationStateSource {
    override var state by mutableStateOf(initialState)
}