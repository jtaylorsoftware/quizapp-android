package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Provides the [Scaffold] design to use across the app, with a custom
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState,
    floatingActionButton: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        floatingActionButton = floatingActionButton,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = { snackbarHostState ->
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    backgroundColor = MaterialTheme.colors.surface,

                    // Show all snackbars with "error" text color, as that is the primary use case
                    // TODO - Dynamically change based on type of message?
                    contentColor = MaterialTheme.colors.error
                )
            }
        }
    ) {
        content(it)
    }
}