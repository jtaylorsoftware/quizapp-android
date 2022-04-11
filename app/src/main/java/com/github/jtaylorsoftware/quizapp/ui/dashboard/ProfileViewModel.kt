package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jtaylorsoftware.quizapp.data.domain.Result
import com.github.jtaylorsoftware.quizapp.data.domain.UserAuthService
import com.github.jtaylorsoftware.quizapp.data.domain.UserRepository
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.di.DefaultDispatcher
import com.github.jtaylorsoftware.quizapp.ui.*
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState
import com.github.jtaylorsoftware.quizapp.util.SimpleEmailValidator
import com.github.jtaylorsoftware.quizapp.util.SimplePasswordValidator
import com.github.jtaylorsoftware.quizapp.util.WaitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    ) : ProfileUiState

    /**
     * The screen data is either empty, or could not be loaded for an error other than authentication.
     */
    data class NoProfile(
        override val loading: LoadingState
    ) : ProfileUiState

    /**
     * The user must sign in again to view this resource.
     */
    object RequireSignIn : ProfileUiState {
        override val loading: LoadingState = LoadingState.Error(ErrorStrings.UNAUTHORIZED.message)
    }

    companion object {
        internal fun fromViewModelState(state: ProfileViewModelState) =
            when {
                state.unauthorized -> RequireSignIn
                state.data == null -> NoProfile(state.loadingState)
                state.emailState != null && state.passwordState != null -> Editor(
                    state.loadingState,
                    state.data,
                    state.emailState,
                    state.passwordState
                )
                else -> Profile(
                    state.loadingState,
                    state.data,
                )
            }
    }
}

/**
 * Internal state representation for both the [ProfileScreen] and [ProfileEditorScreen].
 */
internal data class ProfileViewModelState(
    override val loading: Boolean = false,
    override val error: String? = null,
    val unauthorized: Boolean = false,
    val data: User? = null,
    val emailState: TextFieldState? = null,
    val passwordState: TextFieldState? = null,
) : ViewModelState {
    val editing: Boolean
        get() = emailState != null && passwordState != null
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userAuthService: UserAuthService,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var refreshJob: Job? = null
    private val state = MutableStateFlow(ProfileViewModelState())

    // Used for waiting on validation to finish before submitting email or password
    private val waitGroup = WaitGroup(viewModelScope + dispatcher)

    val uiState = state
        .map { ProfileUiState.fromViewModelState(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ProfileUiState.fromViewModelState(state.value)
        )

    init {
        refresh()
    }

    /**
     * Changes the [uiState] to represent the required data for the [ProfileEditorScreen].
     */
    fun openEditor() {
        if (state.value.editing || state.value.loading) {
            return
        }

        state.update {
            it.copy(emailState = TextFieldState(), passwordState = TextFieldState())
        }
    }

    /**
     * Changes the [uiState] to represent just the data for the [ProfileScreen].
     */
    fun closeEditor() {
        // Do not allow refresh while loading a form or refreshing data.
        if (!state.value.editing || state.value.loading) {
            return
        }

        state.update {
            it.copy(emailState = null, passwordState = null)
        }
    }

    /**
     * Refreshes the stored profile data.
     * 
     * Cannot be called when there is already loading happening.
     */
    fun refresh() {
        // Do not allow refresh while loading a form or refreshing data.
        if (state.value.loading) {
            return
        }

        refreshJob?.cancel()

        state.update {
            it.copy(loading = true)
        }

        refreshJob = viewModelScope.launch {
            userRepository.getProfile()
                .onEach {
                    handleRefreshResult(it)
                }
                .catch {
                    handleRefreshException()
                }
                .collect()
        }
    }

    private fun handleRefreshResult(result: Result<User, Any?>) {
        when (result) {
            is Result.Success -> state.update {
                it.copy(
                    data = result.value,
                    error = null,
                    loading = false,
                )
            }
            is Result.Unauthorized -> state.update {
                ProfileViewModelState(
                    loading = false,
                    unauthorized = true
                )
            }
            else -> state.update {
                it.copy(
                    error = ErrorStrings.NETWORK.message,
                    loading = false,
                )
            }
        }
    }

    private fun handleRefreshException() {
        state.update {
            it.copy(
                error = ErrorStrings.UNKNOWN.message,
                loading = false,
            )
        }
    }

    /**
     * Changes the backing data for the email field on the profile editor.
     */
    fun setEmail(email: String) {
        if (state.value.loading) {
            return
        }

        check(state.value.emailState != null)

        state.update {
            it.copy(emailState = it.emailState!!.copy(text = email, dirty = true))
        }

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
        if (state.value.loading) {
            return
        }

        check(state.value.passwordState != null)

        state.update {
            it.copy(passwordState = it.passwordState!!.copy(text = password, dirty = true))
        }

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
        if (state.value.loading) {
            return
        }

        check(state.value.emailState != null)
        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch(dispatcher) {
            val email = state.value.emailState!!.text

            waitGroup.add {
                validateEmail(email)
            }

            // Try to wait for all validations to finish
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            if (state.value.emailState!!.error != null) {
                state.update {
                    it.copy(loading = false)
                }
                return@launch
            }

            handleSubmitEmailResult(userAuthService.changeEmail(email))
        }
    }

    private fun handleSubmitEmailResult(result: Result<Unit, String?>) {
        when (result) {
            is Result.Success -> {
                state.update {
                    it.copy(loading = false, error = null)
                }
            }
            is Result.BadRequest -> {
                state.update {
                    it.copy(
                        loading = false,
                        emailState = it.emailState!!.copy(error = result.error)
                    )
                }
            }
            is Result.Unauthorized -> {
                state.update {
                    ProfileViewModelState(
                        loading = false,
                        unauthorized = true
                    )
                }
            }
            else -> {
                state.update {
                    it.copy(
                        loading = false,
                        error = ErrorStrings.NETWORK.message
                    )
                }
            }
        }
    }

    /**
     * Submits password changes when on the profile editor.
     */
    fun submitPassword() {
        if (state.value.loading) {
            return
        }

        check(state.value.passwordState != null)
        state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch(dispatcher) {
            val password = state.value.passwordState!!.text

            waitGroup.add {
                validatePassword(password)
            }

            // Try to wait for all validations to finish
            try {
                waitGroup.wait(1000.milliseconds)
            } catch (e: TimeoutCancellationException) {
                // Validations didn't finish, to ensure prompt submission just let server validate it
            }

            if (state.value.passwordState!!.error != null) {
                state.update {
                    it.copy(loading = false)
                }
                return@launch
            }

            handleSubmitPasswordResult(userAuthService.changePassword(password))
        }
    }

    private fun handleSubmitPasswordResult(result: Result<Unit, String?>) {
        when (result) {
            is Result.Success -> {
                state.update {
                    it.copy(loading = false, error = null)
                }
            }
            is Result.BadRequest -> {
                state.update {
                    it.copy(
                        loading = false,
                        passwordState = it.passwordState!!.copy(error = result.error)
                    )
                }
            }
            is Result.Unauthorized -> {
                state.update {
                    ProfileViewModelState(
                        loading = false,
                        unauthorized = true
                    )
                }
            }
            else -> {
                state.update {
                    it.copy(
                        loading = false,
                        error = ErrorStrings.NETWORK.message
                    )
                }
            }
        }
    }

    private fun validateEmail(email: String) {
        state.update {
            it.copy(
                emailState = it.emailState!!.copy(
                    error = if (!SimpleEmailValidator.validate(email)) {
                        "Please input a valid email."
                    } else null
                ),
            )
        }
    }

    private fun validatePassword(password: String) {
        state.update {
            it.copy(
                passwordState = it.passwordState!!.copy(
                    error = if (!SimplePasswordValidator.validate(password)) {
                        "Password must be between 8 and 20 characters"
                    } else null
                ),
            )
        }
    }
}