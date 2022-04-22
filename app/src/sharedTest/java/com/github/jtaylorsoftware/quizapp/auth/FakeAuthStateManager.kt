package com.github.jtaylorsoftware.quizapp.auth

class FakeAuthStateManager(
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