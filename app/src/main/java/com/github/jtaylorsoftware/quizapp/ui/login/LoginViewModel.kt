package com.github.jtaylorsoftware.quizapp.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationEventProducer
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.UserAuthService
import com.github.jtaylorsoftware.quizapp.data.domain.UserCredentialErrors
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserCredentials
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.UiState
import com.github.jtaylorsoftware.quizapp.ui.UiStateSource
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.isInProgress
import com.github.jtaylorsoftware.quizapp.util.SimplePasswordValidator
import com.github.jtaylorsoftware.quizapp.util.UsernameValidator
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class LoginUiState(
    val usernameState: TextFieldState = TextFieldState(),
    val passwordState: TextFieldState = TextFieldState(),
    val loginStatus: LoadingState = LoadingState.NotStarted
) : UiState {
    override val loading: LoadingState = LoadingState.NotStarted

    val hasErrors: Boolean = usernameState.error != null || passwordState.error != null

    val screenIsBusy: Boolean = loginStatus.isInProgress
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userAuthService: UserAuthService,
    private val authEventProducer: AuthenticationEventProducer,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel(), UiStateSource {

    // Used for waiting on validation to finish before submitting login info
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    override var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        // Check if the user is already logged in locally
        val result = userAuthService.userIsSignedIn()
        if (result is Result.Success && result.value) {
            // User is already logged in
            uiState = uiState.copy(loginStatus = LoadingState.Success())

            // Produce "authenticated" event so that the login can be dismissed
            authEventProducer.onAuthenticated()
        }
    }

    fun setUsername(username: String) {
        if (uiState.screenIsBusy) {
            return
        }

        uiState = uiState.copy(
            usernameState = uiState.usernameState.copy(
                text = username,
                dirty = true,
            )
        )

        viewModelScope.launch(dispatcher) {
            // Do validation
            waitGroup.add {
                validateUsername(username)
            }
        }
    }

    fun setPassword(password: String) {
        if (uiState.screenIsBusy) {
            return
        }

        uiState = uiState.copy(
            passwordState = uiState.passwordState.copy(
                text = password,
                dirty = true,
            )
        )


        viewModelScope.launch(dispatcher) {
            // Do validation
            waitGroup.add {
                validatePassword(password)
            }
        }
    }

    fun login() {
        if (uiState.screenIsBusy) {
            return
        }

        uiState = uiState.copy(loginStatus = LoadingState.InProgress)

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                validateUsername(uiState.usernameState.text)
                validatePassword(uiState.passwordState.text)
            }

            // Try to wait for all validations to finish
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            if (uiState.hasErrors) {
                uiState =
                    uiState.copy(loginStatus = LoadingState.Error(FailureReason.FORM_HAS_ERRORS))

                return@launch
            }

            handleLoginResult(
                userAuthService.signInUser(
                    UserCredentials(
                        uiState.usernameState.text,
                        uiState.passwordState.text
                    )
                )
            )
        }
    }

    private fun handleLoginResult(result: Result<Unit, UserCredentialErrors>) {
        when (result) {
            is Result.Success -> {
                uiState = uiState.copy(loginStatus = LoadingState.Success())

                // Produce "authenticated" event so that the login can be dismissed
                authEventProducer.onAuthenticated()
            }
            is Result.Failure -> {
                uiState = uiState.copy(
                    loginStatus = LoadingState.Error(result.reason),
                    usernameState = uiState.usernameState.copy(error = result.errors?.username),
                    passwordState = uiState.passwordState.copy(error = result.errors?.password)
                )
            }
        }
    }

    private fun validateUsername(username: String) {
        uiState = uiState.copy(
            usernameState = uiState.usernameState.copy(
                error = if (!UsernameValidator.validate(username)) {
                    "Username must be between 5 and 12 characters"
                } else null
            ),
        )
    }

    private fun validatePassword(password: String) {
        uiState = uiState.copy(
            passwordState = uiState.passwordState.copy(
                error = if (!SimplePasswordValidator.validate(password)) {
                    "Password must be between 8 and 20 characters"
                } else null
            ),
        )
    }
}