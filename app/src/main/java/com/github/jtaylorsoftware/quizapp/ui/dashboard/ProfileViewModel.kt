package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.auth.AuthenticationEventProducer
import com.github.jtaylorsoftware.quizapp.data.domain.*
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.*
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.util.SimpleEmailValidator
import com.github.jtaylorsoftware.quizapp.util.SimplePasswordValidator
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * [UiState] that holds the representation for either the [ProfileScreen] or the [ProfileEditorScreen].
 *
 * It may instead hold neither representation if data is loading or there is a critical error.
 */
sealed interface ProfileUiState : UiState {
    /**
     * The data for the Profile is loaded.
     */
    data class Profile(
        override val loading: LoadingState,
        val data: User,
    ) : ProfileUiState

    /**
     * The data for the ProfileEditor is loaded.
     */
    data class Editor(
        override val loading: LoadingState,
        val data: User,
        val emailState: TextFieldState,
        val passwordState: TextFieldState,

        /**
         * Progress for email submission
         */
        val submitEmailStatus: LoadingState,

        /**
         * Progress for password submission.
         */
        val submitPasswordStatus: LoadingState,
    ) : ProfileUiState

    /**
     * The screen data is either empty, or could not be loaded for an error other than authentication.
     */
    data class NoProfile(
        override val loading: LoadingState,
    ) : ProfileUiState

    companion object {
        internal fun fromViewModelState(state: ProfileViewModelState) =
            when {
                state.data == null -> NoProfile(
                    state.loading,
                )
                state.emailState != null && state.passwordState != null -> Editor(
                    state.loading,
                    state.data,
                    state.emailState,
                    state.passwordState,
                    state.submitEmailStatus,
                    state.submitPasswordStatus
                )
                else -> Profile(
                    state.loading,
                    state.data,
                )
            }
    }
}

/**
 * Internal state representation for both the [ProfileScreen] and [ProfileEditorScreen].
 */
internal data class ProfileViewModelState(
    val loading: LoadingState = LoadingState.NotStarted,
    val data: User? = null,
    val emailState: TextFieldState? = null,
    val passwordState: TextFieldState? = null,
    val submitEmailStatus: LoadingState = LoadingState.NotStarted,
    val submitPasswordStatus: LoadingState = LoadingState.NotStarted,
) {
    val editing: Boolean = emailState != null && passwordState != null

    val screenIsBusy: Boolean =
        loading.isInProgress || submitEmailStatus.isInProgress || submitPasswordStatus.isInProgress
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userAuthService: UserAuthService,
    private val authEventProducer: AuthenticationEventProducer,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel(), UiStateSource {
    private var refreshJob: Job? = null

    // Used for waiting on validation to finish before submitting email or password
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    private var state by mutableStateOf(ProfileViewModelState())
    override val uiState by derivedStateOf { ProfileUiState.fromViewModelState(state) }

    /**
     * Logs out the current user and clears their cached profile data.
     */
    fun logOut() {
        if (state.screenIsBusy) {
            return
        }

        viewModelScope.launch {
            closeEditor()
            userAuthService.signOut()
            authEventProducer.onRequireLogIn()
        }
    }

    /**
     * Changes the [uiState] to represent the required data for the [ProfileEditorScreen].
     */
    fun openEditor() {
        if (state.editing || state.screenIsBusy) {
            return
        }

        state = state.copy(
            emailState = TextFieldState(text = state.data?.email ?: ""),
            passwordState = TextFieldState()
        )
    }

    /**
     * Changes the [uiState] to represent just the data for the [ProfileScreen].
     */
    fun closeEditor() {
        if (!state.editing || state.screenIsBusy) {
            return
        }

        state = state.copy(emailState = null, passwordState = null)
    }

    /**
     * Refreshes the stored profile data.
     *
     * Cannot be called when there is already something else happening.
     */
    fun refresh() {
        if (state.screenIsBusy) {
            return
        }
        loadProfile()
    }

    /**
     * Loads profile data.
     */
    private fun loadProfile() {
        state = state.copy(
            loading = LoadingState.InProgress,
        )

        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {
            userRepository.getProfile()
                .catch { emit(handleLoadException(it)) }
                .map { handleLoadResult(it) }
                .collect { nextState ->
                    delay(LOAD_DELAY_MILLI)
                    state = nextState
                    if (nextState.loading is LoadingState.Error) {
                        cancel()
                    }
                }

            ensureActive()
            state = state.copy(loading = LoadingState.Success())
        }
    }

    private fun handleLoadResult(result: ResultOrFailure<User>): ProfileViewModelState =
        when (result) {
            is Result.Success -> state.copy(data = result.value)
            is Result.Failure -> state.copy(loading = LoadingState.Error(result.reason))
        }

    private fun handleLoadException(throwable: Throwable): Result.Failure<Nothing> {
        // For now just always return a Result.Failure with UNKNOWN
        return Result.Failure(FailureReason.UNKNOWN)
    }

    /**
     * Changes the backing data for the email field on the profile editor.
     */
    fun setEmail(email: String) {
        if (state.screenIsBusy) {
            return
        }

        check(state.emailState != null)

        state = state.copy(emailState = state.emailState!!.copy(text = email, dirty = true))

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                validateEmail(email)
            }
        }
    }

    /**
     * Changes the backing data for the password field on the profile editor.
     */
    fun setPassword(password: String) {
        if (state.screenIsBusy) {
            return
        }

        check(state.passwordState != null)

        state =
            state.copy(passwordState = state.passwordState!!.copy(text = password, dirty = true))

        viewModelScope.launch(dispatcher) {
            waitGroup.add {
                validatePassword(password)
            }
        }
    }

    /**
     * Submits email changes when on the profile editor.
     */
    fun submitEmail() {
        if (state.screenIsBusy) {
            return
        }

        check(state.emailState != null)

        state = state.copy(submitEmailStatus = LoadingState.InProgress)

        viewModelScope.launch(dispatcher) {
            val email = state.emailState!!.text

            waitGroup.add {
                validateEmail(email)
            }

            // Try to wait for all validations to finish
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            if (state.emailState!!.error != null) {
                state =
                    state.copy(submitEmailStatus = LoadingState.Error(FailureReason.FORM_HAS_ERRORS))
                return@launch
            }

            handleSubmitEmailResult(userAuthService.changeEmail(email))
        }
    }

    private fun handleSubmitEmailResult(result: Result<Unit, ChangeEmailError>) {
        state = when (result) {
            is Result.Success -> {
                state.copy(submitEmailStatus = LoadingState.Success(SuccessStrings.EMAIL_CHANGED))
            }
            is Result.Failure -> {
                state.copy(
                    submitEmailStatus = LoadingState.Error(result.reason),
                    emailState = state.emailState!!.copy(error = result.errors?.email)
                )
            }
        }
    }

    /**
     * Submits password changes when on the profile editor.
     */
    fun submitPassword() {
        if (state.screenIsBusy) {
            return
        }

        check(state.passwordState != null)

        state = state.copy(submitPasswordStatus = LoadingState.InProgress)

        viewModelScope.launch(dispatcher) {
            val password = state.passwordState!!.text

            waitGroup.add {
                validatePassword(password)
            }

            // Try to wait for all validations to finish
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            if (state.passwordState!!.error != null) {
                state =
                    state.copy(submitPasswordStatus = LoadingState.Error(FailureReason.FORM_HAS_ERRORS))

                return@launch
            }

            handleSubmitPasswordResult(userAuthService.changePassword(password))
        }
    }

    private fun handleSubmitPasswordResult(result: Result<Unit, ChangePasswordError>) {
        state = when (result) {
            is Result.Success -> {
                state.copy(
                    submitPasswordStatus = LoadingState.Success(
                        SuccessStrings.PASSWORD_CHANGED
                    )
                )
            }
            is Result.Failure -> {
                state.copy(
                    submitPasswordStatus = LoadingState.Error(result.reason),
                    passwordState = state.passwordState!!.copy(error = result.errors?.password)
                )
            }
        }
    }

    private fun validateEmail(email: String) {
        state = state.copy(
            emailState = state.emailState!!.copy(
                error = if (!SimpleEmailValidator.validate(email)) {
                    "Please input a valid email."
                } else null
            ),
        )
    }

    private fun validatePassword(password: String) {
        state = state.copy(
            passwordState = state.passwordState!!.copy(
                error = if (!SimplePasswordValidator.validate(password)) {
                    "Password must be between 8 and 20 characters"
                } else null
            ),
        )
    }
}