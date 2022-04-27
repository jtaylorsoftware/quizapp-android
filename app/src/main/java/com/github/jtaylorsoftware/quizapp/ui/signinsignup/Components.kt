package com.github.jtaylorsoftware.quizapp.ui.signinsignup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.components.ButtonWithProgress
import com.github.jtaylorsoftware.quizapp.ui.components.SmallCircularProgressIndicator

/**
 * Displays the primary action button for the SignIn or SignUp screens.
 */
@Composable
internal fun SignInSignUpButton(
    text: String,
    onClick: () -> Unit,
    isInProgress: Boolean,
    contentDescription: String,
) {
    val buttonTextAlpha by derivedStateOf { if (isInProgress) 0.0f else 1.0f }

    ButtonWithProgress(
        onClick = onClick,
        isInProgress = isInProgress,
        progressIndicator = {
            SmallCircularProgressIndicator(
                Modifier.semantics { this.contentDescription = contentDescription }
            )
        },
        shape = RoundedCornerShape(25),
        modifier = Modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth(0.75f),
    ) {
        Text(text, modifier = Modifier.alpha(buttonTextAlpha))
    }
}