package com.github.jtaylorsoftware.quizapp.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jtaylorsoftware.quizapp.ui.theme.correct

/**
 * Reusable [LazyColumn] component with added padding and a defined width.
 * Has a slot for the [Composable] shown before any dynamically updated content.
 */
@Composable
inline fun ProfileList(
    modifier: Modifier = Modifier,
    noinline header: @Composable (LazyItemScope.() -> Unit)? = null,
    crossinline content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        if (header != null) {
            item {
                header()
            }
        }

        content()
    }
}

@Composable
internal fun ProfileListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier, elevation = 1.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}

/**
 * Renders a ring pie chart based on [score]. Also shows [score] as a percent value in the middle
 * of the chart.
 *
 * @param size Size of the chart. Must be at least 32dp (to allow for the percent in the middle).
 *
 * @param score A percent (in the range `[0.0,1.0]`) to display.
 */
@Composable
internal fun ScoreRingChart(score: Float, size: Dp = 64.dp) {
    val scorePercentString = remember { "${"%.2f".format(score * 100)}%" }

    Box(
        Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Score $scorePercentString"
        },
        contentAlignment = Alignment.Center
    ) {
        Text(scorePercentString, style = MaterialTheme.typography.caption)

        val correct = MaterialTheme.colors.correct
        val incorrect = MaterialTheme.colors.error
        Canvas(Modifier.requiredSize(size)) {
            val end1 = (1.0f - score) * 360.0f + 0.01f
            val end2 = score * 360.0f + 0.01f
            drawArc(
                incorrect,
                startAngle = 270.0f,
                sweepAngle = end1,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx())
            )
            drawArc(
                correct,
                startAngle = 270.0f + end1,
                sweepAngle = end2,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}
