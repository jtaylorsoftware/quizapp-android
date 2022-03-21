package com.github.jtaylorsoftware.quizapp.ui.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.jtaylorsoftware.quizapp.ui.components.TextFieldState

class RegisterViewModel: ViewModel() {
    var usernameState: TextFieldState by mutableStateOf(TextFieldState())
        private set

    var passwordState: TextFieldState by mutableStateOf(TextFieldState())
        private set

    fun setUsername(username: String) {
        usernameState = usernameState.copy(text = username)
    }

    fun setPassword(password: String) {
        passwordState = passwordState.copy(text = password)
    }

    fun register(navigateToDashboard: () -> Unit) {
        navigateToDashboard()
    }
}