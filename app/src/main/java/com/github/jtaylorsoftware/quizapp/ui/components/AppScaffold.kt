package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.github.jtaylorsoftware.quizapp.ui.LoadingState
import com.github.jtaylorsoftware.quizapp.ui.UiState

/**
 * Displays [content] in a [Scaffold] and handles updating the screen with Snackbars based on a [UiState].
 */
@Composable
fun AppScaffold(
    scaffoldState: ScaffoldState,
    uiState: UiState,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    if (uiState.loading is LoadingState.Error) {
        LaunchedEffect(scaffoldState.snackbarHostState) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = (uiState.loading as LoadingState.Error).message.value
            )
        }
    }

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
                    contentColor = MaterialTheme.colors.error
                )
            }
        }
    ) {
        content(it)
    }
}