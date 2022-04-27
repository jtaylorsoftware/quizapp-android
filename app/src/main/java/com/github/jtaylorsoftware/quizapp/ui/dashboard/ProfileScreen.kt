package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.jtaylorsoftware.quizapp.R
import com.github.jtaylorsoftware.quizapp.data.domain.models.ObjectId
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.OnSuccess
import com.github.jtaylorsoftware.quizapp.ui.WindowSizeClass
import com.github.jtaylorsoftware.quizapp.ui.components.*
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import com.github.jtaylorsoftware.quizapp.ui.theme.onTopAppBar
import com.github.jtaylorsoftware.quizapp.ui.theme.topAppBar
import com.github.jtaylorsoftware.quizapp.util.toLocalizedString
import com.google.accompanist.flowlayout.FlowRow

/**
 * Displays a [User]'s non-sensitive data, such as username,
 * the number of quizzes, and the number of results.
 *
 * @param navigateToQuizCreator Function called when user taps the "Create Quiz" button. Should
 * be the same action as tapping the FloatingActionButton on the QuizList screen.
 *
 * @param navigateToQuizResults Called when the user taps the "View results" button. Should
 * perform the same navigation as tapping the BottomNavigation icon.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState.Profile,
    navigateToQuizCreator: () -> Unit,
    navigateToQuizResults: () -> Unit,
    maxWidthDp: Dp = LocalConfiguration.current.screenWidthDp.dp,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .width(maxWidthDp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserCard(uiState.data)
        QuizzesCard(uiState.data.quizzes, navigateToQuizCreator)
        QuizResultsCard(uiState.data.results, navigateToQuizResults)
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    QuizAppTheme {
        Surface {
            ProfileScreen(
                uiState = ProfileUiState.Profile(
                    loading = LoadingState.NotStarted,
                    data = User(
                        username = "Username",
                        email = "useremail@example.com",
                        quizzes = listOf(ObjectId("123")),
                        results = listOf(ObjectId("123"), ObjectId("123"))
                    ),
                    false
                ),
                navigateToQuizCreator = {},
                navigateToQuizResults = {},
                maxWidthDp = LocalConfiguration.current.screenWidthDp.dp
            )
        }
    }
}

/**
 * A card displaying a brief summary of a [User]'s non-sensitive data.
 *
 * @param user User to read data from.
 */
@Composable
private fun UserCard(user: User) {
    ProfileListCard(Modifier.fillMaxWidth()) {
        Text(
            "Hello, ${user.username}",
            modifier = Modifier.padding(bottom = 12.dp),
            style = MaterialTheme.typography.h4
        )
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                "Joined: ${user.date.toLocalizedString()}",
                modifier = Modifier.padding(bottom = 4.dp),
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@Composable
private fun QuizzesCard(quizzes: List<ObjectId>, navigateToQuizCreator: () -> Unit) {
    ProfileListCard(Modifier.fillMaxWidth()) {
        Text(
            "You've created ${quizzes.size} quizzes.",
            style = MaterialTheme.typography.h6
        )
        // TODO: Most recent quiz
        ProfileNavigationButton(text = "Create quiz", onClick = navigateToQuizCreator)
    }
}

@Composable
private fun QuizResultsCard(results: List<ObjectId>, navigateToQuizResults: () -> Unit) {
    ProfileListCard(Modifier.fillMaxWidth()) {
        Text(
            "You've taken ${results.size} quizzes.",
            style = MaterialTheme.typography.h6
        )
        // TODO: Most recent result
        ProfileNavigationButton(text = "View results", onClick = navigateToQuizResults)
    }
}

@Composable
private fun ProfileNavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints {
        Button(
            onClick = onClick,
            modifier = modifier.widthIn(min = maxWidth * 0.4f, max = maxWidth * 0.6f)
        ) {
            Text(text)
        }
    }
}

/**
 * Displays either a max size screen or a Dialog where the user can edit their profile data or sign out.
 *
 * @param onClose Callback invoked when the user taps to close the editor.
 *
 * @param onChangeEmail Callback invoked when user inputs text in the form.
 *
 * @param onChangePassword Callback invoked when the first password input changes.
 *
 * @param onSubmitPassword Callback invoked when the user presses "Submit" in the Password form.
 *
 * @param onSubmitEmail Callback invoked when the user presses "Submit" in the Email form.
 *
 * @param windowSizeClass The current [WindowSizeClass].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProfileSettingsDialog(
    uiState: ProfileUiState.Profile,
    onClose: () -> Unit,
    onChangeEmail: (String) -> Unit,
    onChangePassword: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onSubmitPassword: () -> Unit,
    maxWidthDp: Dp,
    windowSizeClass: WindowSizeClass,
) {
    // Ensure Settings are actually open
    check(
        uiState.submitEmailStatus != null &&
                uiState.submitPasswordStatus != null &&
                uiState.emailState != null &&
                uiState.passwordState != null
    )

    val useDialog = remember(windowSizeClass) {
        when (windowSizeClass) {
            WindowSizeClass.Compact -> false // allow maximum size possible
            else -> true // disallow max size, show as standard dialog
        }
    }

    val settings = @Composable { modifier: Modifier ->
        ProfileSettings(
            modifier = modifier,
            onClose = onClose,
            email = uiState.data.email,
            emailState = uiState.emailState,
            passwordState = uiState.passwordState,
            submitEmailStatus = uiState.submitEmailStatus,
            submitPasswordStatus = uiState.submitPasswordStatus,
            onChangeEmail = onChangeEmail,
            onChangePassword = onChangePassword,
            onSubmitEmail = onSubmitEmail,
            onSubmitPassword = onSubmitPassword
        )
    }

    if (useDialog) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false // Handling at Route level
            )
        ) {
            settings(
                Modifier
                    .padding(16.dp)
                    .width(maxWidthDp)
            )
        }
    } else {
        settings(
            Modifier
                .padding(vertical = 16.dp, horizontal = 32.dp)
                .fillMaxSize()
        )
    }
}


/**
 * Displays a TopAppBar styled like it's inline with the form.
 */
@Composable
internal fun ProfileTopBar(
    openSettings: () -> Unit,
    onLogOut: () -> Unit,
) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    // Show dialog to prevent accidental logout
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogOpen = false
                    onLogOut()
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            },
            text = { Text("Do you really want to sign out?") },
        )
    }

    TopAppBar(
        title = { },
        navigationIcon = { },
        backgroundColor = MaterialTheme.colors.topAppBar,
        contentColor = MaterialTheme.colors.onTopAppBar,
        elevation = 0.dp,
        actions = {
            IconButton(onClick = openSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                )
            }
            IconButton(onClick = { dialogOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_logout_24),
                    contentDescription = "Log out",
                )
            }
        }
    )
}

@Preview
@Composable
private fun ProfileTopBarPreview() {
    QuizAppTheme {
        ProfileTopBar(openSettings = {}, onLogOut = {})
    }
}

/**
 * Displays a TopAppBar styled like it's inline with the form.
 */
@Composable
internal fun SettingsTopBar(
    onClose: () -> Unit,
) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    // Show dialog to prevent accidental logout
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogOpen = false
                    // onLogOut()
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Cancel")
                }
            },
            text = { Text("Do you really want to sign out?") },
        )
    }

    TopAppBar(
        title = { Text("Profile Settings") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close profile settings")
            }
        },
        backgroundColor = MaterialTheme.colors.topAppBar,
        contentColor = MaterialTheme.colors.onTopAppBar,
        elevation = 0.dp,
        actions = {
//            IconButton(onClick = { dialogOpen = true }) {
//                Icon(
//                    painter = painterResource(R.drawable.ic_logout_24),
//                    contentDescription = "Log out",
//                    modifier = Modifier.testTag("LogOut")
//                )
//            }
        }
    )
}

@Preview
@Composable
private fun EditorTopBarPreview() {
    QuizAppTheme {
        SettingsTopBar(onClose = {})
    }
}

@Composable
internal fun ProfileSettings(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    email: String,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    submitEmailStatus: LoadingState,
    submitPasswordStatus: LoadingState,
    onChangeEmail: (String) -> Unit,
    onChangePassword: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onSubmitPassword: () -> Unit,
) {

    // The email as it was when the Editor was opened, or after the email was changed
    // successfully
    var initialEmail by rememberSaveable { mutableStateOf(email) }

    var emailFormIsOpen: Boolean by rememberSaveable { mutableStateOf(false) }
    var passwordFormIsOpen: Boolean by rememberSaveable { mutableStateOf(false) }

    OnSuccess(submitEmailStatus) {
        // Close the email form after successfully changing it
        emailFormIsOpen = false

        // Also change the initial (now updated) email to reflect changes
        initialEmail = emailState.text
    }

    OnSuccess(submitPasswordStatus) {
        // Close the password form after successfully changing it
        passwordFormIsOpen = false
    }

    ProfileSettings(
        modifier = modifier,
        onClose = onClose,
        emailState = emailState,
        passwordState = passwordState,
        submitEmailStatus = submitEmailStatus,
        submitPasswordStatus = submitPasswordStatus,
        onChangeEmail = onChangeEmail,
        onChangePassword = onChangePassword,
        onSubmitEmail = onSubmitEmail,
        onSubmitPassword = onSubmitPassword,
        initialEmail = initialEmail,
        emailFormIsOpen = emailFormIsOpen,
        setEmailFormIsOpen = { emailFormIsOpen = it },
        passwordFormIsOpen = passwordFormIsOpen,
        setPasswordFormIsOpen = { passwordFormIsOpen = it }
    )
}

@Composable
private fun ProfileSettings(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    submitEmailStatus: LoadingState,
    submitPasswordStatus: LoadingState,
    onChangeEmail: (String) -> Unit,
    onChangePassword: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onSubmitPassword: () -> Unit,
    initialEmail: String,
    emailFormIsOpen: Boolean,
    setEmailFormIsOpen: (Boolean) -> Unit,
    passwordFormIsOpen: Boolean,
    setPasswordFormIsOpen: (Boolean) -> Unit,
) {
    Column(horizontalAlignment = Alignment.Start) {
        SettingsTopBar(onClose = onClose)
        Surface {
            Column(
                modifier
                    .testTag("ProfileSettingsDialog")
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Email: $initialEmail", style = MaterialTheme.typography.h6)

                Column {
                    // Display a column of options for changing various aspects of the user's profile
                    FlowRow(Modifier.padding(vertical = 32.dp)) {
                        Button(
                            onClick = {
                                setPasswordFormIsOpen(false)
                                setEmailFormIsOpen(true)
                            },
                            enabled = !emailFormIsOpen,
                        ) {
                            Text("Change Email")
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(
                            onClick = {
                                setEmailFormIsOpen(false)
                                setPasswordFormIsOpen(true)
                            },
                            enabled = !passwordFormIsOpen,
                        ) {
                            Text("Change Password")
                        }
                    }
                }

                if (emailFormIsOpen) {
                    EmailForm(
                        emailState,
                        { setEmailFormIsOpen(false) },
                        submitEmailStatus,
                        onChangeEmail,
                        onSubmitEmail
                    )
                }
                if (passwordFormIsOpen) {
                    PasswordForm(
                        passwordState,
                        { setPasswordFormIsOpen(false) },
                        submitPasswordStatus,
                        onChangePassword,
                        onSubmitPassword
                    )
                }

                Text(
                    "Account deletion available when signed into the web app.",
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Preview(widthDp = 600)
@Composable
private fun ProfileSettingsPreview() {
    val uiState = ProfileUiState.Profile(
        LoadingState.NotStarted,
        User(username = "Username", email = "email@example.com"),
        settingsOpen = true,
        TextFieldState(),
        TextFieldState(),
        LoadingState.NotStarted,
        LoadingState.NotStarted,
    )
    QuizAppTheme {
        Surface {
            ProfileSettings(
                email = uiState.data.email,
                emailState = uiState.emailState!!,
                passwordState = uiState.passwordState!!,
                submitEmailStatus = uiState.submitEmailStatus!!,
                submitPasswordStatus = uiState.submitPasswordStatus!!,
                onChangeEmail = {},
                onChangePassword = {},
                onSubmitEmail = {},
                onSubmitPassword = {},
                onClose = {}
            )
        }
    }
}

/**
 * Displays the user's current email, with a button to open a form to change the email.
 *
 * @param emailState State containing any changes to the email.
 * @param closeForm Function to close state of form.
 * @param submitEmailStatus Status of email form submission.
 * @param onChangeEmail Callback invoked when user inputs text in the form.
 * @param onSubmitEmail Callback invoked when user wants to submit their changes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EmailForm(
    emailState: TextFieldState,
    closeForm: () -> Unit,
    submitEmailStatus: LoadingState,
    onChangeEmail: (String) -> Unit,
    onSubmitEmail: () -> Unit
) {
    val isInProgress = remember(submitEmailStatus) { submitEmailStatus is LoadingState.InProgress }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(Modifier.width(IntrinsicSize.Min)) {
        EmailField(
            state = emailState,
            onTextChange = onChangeEmail,
            modifier = Modifier.requiredWidth(300.dp),
            imeAction = ImeAction.Done,
            onImeAction = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )
        CancelAndSubmitButtons(
            propertyName = "Email",
            onCancelClick = {
                focusManager.clearFocus()
                closeForm()
            },
            onSubmitClick = {
                focusManager.clearFocus()
                onSubmitEmail()
            },
            submitIsInProgress = isInProgress
        )
    }
}

@Preview
@Composable
private fun EmailFormPreview() {
    QuizAppTheme {
        Surface {
            Column {
                EmailForm(
                    emailState = TextFieldState(text = "email@example.com"),
                    closeForm = {},
                    submitEmailStatus = LoadingState.InProgress,
                    onChangeEmail = {},
                    onSubmitEmail = {}
                )
            }
        }
    }
}

/**
 * Displays a form for the user to input a new password. The first input password
 * is error checked externally, whereas the confirmation password only checks itself
 * against the first password.
 *
 * @param passwordState The state for the to-be-submitted password that can be error checked
 * according to business logic.
 *
 * @param closeForm Function to close state of form.
 * @param submitPasswordStatus Status of email form submission.
 * @param onChangePassword Callback invoked when the first password input changes.
 * @param onSubmitPassword Callback invoked when the user presses "Submit."
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasswordForm(
    passwordState: TextFieldState,
    closeForm: () -> Unit,
    submitPasswordStatus: LoadingState,
    onChangePassword: (String) -> Unit,
    onSubmitPassword: () -> Unit
) {
    val isInProgress =
        remember(submitPasswordStatus) { submitPasswordStatus is LoadingState.InProgress }

    var confirmPasswordState by rememberSaveable(stateSaver = TextFieldState.Saver) {
        mutableStateOf(
            TextFieldState()
        )
    }
    val onConfirmPasswordChanged = { confirmPassword: String ->
        val error = if (confirmPassword != passwordState.text) {
            "Passwords do not match."
        } else null
        confirmPasswordState = TextFieldState(text = confirmPassword, error = error, dirty = true)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(Modifier.width(IntrinsicSize.Min)) {
        PasswordField(
            state = passwordState,
            modifier = Modifier.requiredWidth(300.dp),
            onTextChange = onChangePassword,
            imeAction = ImeAction.Next
        )
        ConfirmPasswordField(
            state = confirmPasswordState,
            onTextChange = onConfirmPasswordChanged,
            modifier = Modifier.requiredWidth(300.dp),
            imeAction = ImeAction.Done,
            onImeAction = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )

        CancelAndSubmitButtons(
            propertyName = "Password",
            onCancelClick = {
                confirmPasswordState = confirmPasswordState.copy(text = "")
                focusManager.clearFocus()
                closeForm()
            },
            onSubmitClick = {
                confirmPasswordState = confirmPasswordState.copy(text = "")
                focusManager.clearFocus()
                onSubmitPassword()
            },
            submitIsInProgress = isInProgress
        )
    }
}

@Preview
@Composable
private fun PasswordFormPreview() {
    QuizAppTheme {
        Surface {
            Column {
                PasswordForm(
                    passwordState = TextFieldState(text = "password"),
                    closeForm = {},
                    submitPasswordStatus = LoadingState.InProgress,
                    onChangePassword = {},
                    onSubmitPassword = {}
                )
            }
        }
    }
}

/**
 * Displays a [Row] with a [OutlinedButton] for cancel and [Button] for submit.
 *
 * @param propertyName The name of the property this composable is for.
 */
@Composable
private fun CancelAndSubmitButtons(
    propertyName: String,
    onCancelClick: () -> Unit,
    onSubmitClick: () -> Unit,
    submitIsInProgress: Boolean,
) {
    val submitButtonAlpha by derivedStateOf { if (submitIsInProgress) 0.0f else 1.0f }

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(
            onClick = onCancelClick,
            enabled = !submitIsInProgress
        ) {
            Text("Cancel")
        }
        Spacer(Modifier.width(16.dp))
        ButtonWithProgress(
            onClick = onSubmitClick,
            isInProgress = submitIsInProgress,
            progressIndicator = {
                SmallCircularProgressIndicator(
                    Modifier.semantics {
                        contentDescription = "Change $propertyName is in progress"
                    }
                )
            }
        ) {
            Text("Submit", modifier = Modifier.alpha(submitButtonAlpha))
        }
    }
}