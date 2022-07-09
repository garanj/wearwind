package com.garan.wearwind.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

const val HIDDEN_ARC = -60f
const val LONG_HOLD_SECONDS = 2

@Composable
fun EndRing(
    onFinishTap: () -> Unit
) {
    val animateCircle = remember { Animatable(HIDDEN_ARC) }
    LaunchedEffect(animateCircle) {
        val result = animateCircle.animateTo(
            targetValue = 360f,
            animationSpec = tween(
                durationMillis = LONG_HOLD_SECONDS * 1000,
                easing = LinearEasing
            )
        )
        if (result.endReason == AnimationEndReason.Finished) {
            onFinishTap()
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (animateCircle.value >= 0) {
            val progressColor = MaterialTheme.colors.primary
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val arcSize = Size(size.width - 20f, size.height - 20f)
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = animateCircle.value,
                    useCenter = false,
                    style = Stroke(width = 100f),
                    topLeft = Offset(
                        (size.width - arcSize.width) / 2,
                        (size.height - arcSize.height) / 2
                    ),
                    size = arcSize
                )
            }
            Text(
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.display2,
                text = (animateCircle.value / (360 / LONG_HOLD_SECONDS) + 1).toInt().toString()
            )
        }
    }
}