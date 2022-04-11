package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.UserAuthService
import com.github.jtaylorsoftware.quizapp.data.domain.UserCredentialErrors
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserCredentials
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.*
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.util.SimplePasswordValidator
import com.github.jtaylorsoftware.quizapp.util.UsernameValidator
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

sealed interface LoginUiState : UiState {
    data class Form(
        override val loading: LoadingState,
        val usernameState: TextFieldState,
        val passwordState: TextFieldState,
    ) : LoginUiState

    object SignedIn : LoginUiState {
        override val loading: LoadingState = LoadingState.Default
    }

    companion object {
        internal fun fromViewModelState(state: LoginViewModelState) = if (!state.signedIn) {
            Form(
                loading = state.loadingState,
                usernameState = state.usernameState,
                passwordState = state.passwordState
            )
        } else {
            SignedIn
        }
    }
}

internal data class LoginViewModelState(
    /**
     * `true` if user is signed in.
     */
    val signedIn: Boolean = false,
    /**
     * `true` while waiting for response from login endpoint.
     */
    override val loading: Boolean = false,
    /**
     * Error message if login failed.
     */
    override val error: String? = null,
    val usernameState: TextFieldState = TextFieldState(),
    val passwordState: TextFieldState = TextFieldState(),
) : ViewModelState {

    val hasErrors: Boolean = usernameState.error != null ||
            passwordState.error != null
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userAuthService: UserAuthService,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(LoginViewModelState())

    // Used for waiting on validation to finish before submitting login info
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    val uiState = state
        .map { LoginUiState.fromViewModelState(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            LoginUiState.fromViewModelState(state.value)
        )

    init {
        val result = userAuthService.userIsSignedIn()
        if (result is Result.Success && result.value) {
            state.update {
                it.copy(signedIn = true)
            }
        }
    }

    fun setUsername(username: String) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(
                usernameState = it.usernameState.copy(
                    text = username,
                    dirty = true,
                )
            )
        }

        viewModelScope.launch(dispatcher) {
            // Do validation
            waitGroup.add {
                validateUsername(username)
            }
        }
    }

    fun setPassword(password: String) {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(
                passwordState = it.passwordState.copy(
                    text = password,
                    dirty = true,
                )
            )
        }

        viewModelScope.launch(dispatcher) {
            // Do validation
            waitGroup.add {
                validatePassword(password)
            }
        }
    }

    fun login() {
        if (state.value.loading) {
            return
        }

        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                validateUsername(state.value.usernameState.text)
                validatePassword(state.value.passwordState.text)
            }

            // Try to wait for all validations to finish
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            if (state.value.hasErrors) {
                state.update {
                    it.copy(loading = false)
                }
                return@launch
            }

            handleLoginResult(
                userAuthService.signInUser(
                    UserCredentials(
                        state.value.usernameState.text,
                        state.value.passwordState.text
                    )
                )
            )
        }
    }

    private fun handleLoginResult(result: Result<Unit, UserCredentialErrors>) {
        when (result) {
            is Result.Success -> {
                state.update {
                    it.copy(signedIn = true, loading = false)
                }
            }
            is Result.BadRequest -> {
                state.update {
                    it.copy(
                        loading = false,
                        error = "Please check your input.",
                        usernameState = it.usernameState.copy(error = result.error.username),
                        passwordState = it.passwordState.copy(error = result.error.password)
                    )
                }
            }
            else -> {
                state.update {
                    it.copy(loading = false, error = "Unable to connect to service.")
                }
            }
        }
    }

    private fun validateUsername(username: String) {
        state.update {
            it.copy(
                usernameState = it.usernameState.copy(
                    error = if (!UsernameValidator.validate(username)) {
                        "Username must be between 5 and 12 characters"
                    } else null
                ),
            )
        }
    }

    private fun validatePassword(password: String) {
        state.update {
            it.copy(
                passwordState = it.passwordState.copy(
                    error = if (!SimplePasswordValidator.validate(password)) {
                        "Password must be between 8 and 20 characters"
                    } else null
                ),
            )
        }
    }
}