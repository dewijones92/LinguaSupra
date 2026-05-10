package com.dewijones.linguasupra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LanguageProgressRing(
    fraction: Float,
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    overflowColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceAtLeast(0f),
        animationSpec = tween(durationMillis = 600),
        label = "progress",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val rectSize = Size(this.size.width, this.size.height)
            // background ring
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = rectSize,
                style = stroke,
            )
            // 0..1 progress
            val mainSweep = (animated.coerceAtMost(1f)) * 360f
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = mainSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = rectSize,
                style = stroke,
            )
            // overflow (>1) drawn in tertiary so over-quota looks like a victory lap
            if (animated > 1f) {
                val overflow = ((animated - 1f).coerceAtMost(1f)) * 360f
                drawArc(
                    color = overflowColor,
                    startAngle = -90f,
                    sweepAngle = overflow,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = rectSize,
                    style = stroke,
                )
            }
        }
        Text(text = emoji, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified, style = MaterialTheme.typography.headlineMedium)
    }
}
