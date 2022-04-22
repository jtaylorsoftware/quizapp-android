package com.github.jtaylorsoftware.quizapp.ui.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationEventProducer
import com.github.jtaylorsoftware.quizapp.data.domain.FailureReason
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.UserAuthService
import com.github.jtaylorsoftware.quizapp.data.domain.UserRegistrationErrors
import com.github.jtaylorsoftware.quizapp.data.domain.models.UserRegistration
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.UiState
import com.github.jtaylorsoftware.quizapp.ui.UiStateSource
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.ui.isInProgress
import com.github.jtaylorsoftware.quizapp.util.SimpleEmailValidator
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

data class SignupUiState(
    /**
     * `true` if user is signed in.
     */
    val registerStatus: LoadingState = LoadingState.NotStarted,
    val usernameState: TextFieldState = TextFieldState(),
    val passwordState: TextFieldState = TextFieldState(),
    val emailState: TextFieldState = TextFieldState(),
): UiState {
    override val loading: LoadingState = LoadingState.Success()

    val screenIsBusy: Boolean = registerStatus.isInProgress
    val hasErrors: Boolean = usernameState.error != null ||
            emailState.error != null ||
            passwordState.error != null
}

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val userAuthService: UserAuthService,
    private val authEventProducer: AuthenticationEventProducer,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel(), UiStateSource {

    // Used for waiting on validation to finish before doing signup
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    override var uiState by mutableStateOf(SignupUiState())
        private set

    init {
        // Check if user is already signed in and can skip this screen
        val result = userAuthService.userIsSignedIn()
        if (result is Result.Success && result.value) {
            uiState = uiState.copy(registerStatus = LoadingState.Success())

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
        validateUsername(username)
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

    fun setEmail(email: String) {
        if (uiState.screenIsBusy) {
            return
        }

        uiState = uiState.copy(
            emailState = uiState.emailState.copy(
                text = email,
                dirty = true,
            )
        )

        viewModelScope.launch(dispatcher) {
            // Do validation
            waitGroup.add {
                validateEmail(email)
            }
        }
    }

    fun register() {
        if (uiState.screenIsBusy) {
            return
        }

        uiState = uiState.copy(registerStatus = LoadingState.InProgress)

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                validateUsername(uiState.usernameState.text)
                validateEmail(uiState.emailState.text)
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
                    uiState.copy(registerStatus = LoadingState.Error(FailureReason.FORM_HAS_ERRORS))
                return@launch
            }

            handleSignUpResult(
                userAuthService.registerUser(
                    UserRegistration(
                        username = uiState.usernameState.text,
                        email = uiState.emailState.text,
                        password = uiState.passwordState.text
                    )
                )
            )
        }
    }

    private fun handleSignUpResult(result: Result<Unit, UserRegistrationErrors>) {
        when (result) {
            is Result.Success -> {
                uiState = uiState.copy(registerStatus = LoadingState.Success())

                // Produce "authenticated" event so that the registration screen can be dismissed
                authEventProducer.onAuthenticated()
            }
            is Result.Failure -> {
                uiState = uiState.copy(
                    registerStatus = LoadingState.Error(result.reason),
                    usernameState = uiState.usernameState.copy(error = result.errors?.username),
                    emailState = uiState.emailState.copy(error = result.errors?.email),
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

    private fun validateEmail(email: String) {
        uiState = uiState.copy(
            emailState = uiState.emailState.copy(
                error = if (!SimpleEmailValidator.validate(email)) {
                    "Please input a valid email."
                } else null
            ),
        )
    }
}