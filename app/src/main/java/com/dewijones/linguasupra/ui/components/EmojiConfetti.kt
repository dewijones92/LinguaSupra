package com.dewijones.linguasupra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val emoji: String,
    val angleDeg: Float,
    val distancePx: Float,
    val rotateStart: Float,
    val rotateEnd: Float,
    val scale: Float,
)

/**
 * A small celebratory emoji burst at the location of [originX], [originY] (offset within the
 * Box that hosts this composable). Animates 8–12 particles outward then fades them.
 *
 * Re-trigger by changing [trigger] (any value change restarts).
 */
@Composable
fun EmojiConfetti(
    trigger: Int,
    emoji: String,
    originX: androidx.compose.ui.unit.Dp = 0.dp,
    originY: androidx.compose.ui.unit.Dp = 0.dp,
    particleCount: Int = 10,
) {
    if (trigger == 0) return
    key(trigger) {
        val particles = remember(trigger) {
            List(particleCount) {
                Particle(
                    emoji = emoji,
                    angleDeg = Random.nextFloat() * 360f,
                    distancePx = 80f + Random.nextFloat() * 100f,
                    rotateStart = Random.nextFloat() * 30f - 15f,
                    rotateEnd = Random.nextFloat() * 360f - 180f,
                    scale = 0.7f + Random.nextFloat() * 0.6f,
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                ConfettiParticle(
                    particle = p,
                    originX = originX,
                    originY = originY,
                )
            }
        }
    }
}

@Composable
private fun ConfettiParticle(
    particle: Particle,
    originX: androidx.compose.ui.unit.Dp,
    originY: androidx.compose.ui.unit.Dp,
) {
    val travel = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val rotate = remember { Animatable(particle.rotateStart) }
    LaunchedEffect(Unit) {
        travel.animateTo(1f, animationSpec = tween(durationMillis = 700))
        alpha.animateTo(0f, animationSpec = tween(durationMillis = 350))
    }
    LaunchedEffect(Unit) {
        rotate.animateTo(particle.rotateEnd, animationSpec = tween(durationMillis = 700))
    }
    val rad = Math.toRadians(particle.angleDeg.toDouble())
    val dx = (cos(rad) * particle.distancePx * travel.value).toInt()
    val dy = (sin(rad) * particle.distancePx * travel.value).toInt()
    Text(
        text = particle.emoji,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .offset(x = originX, y = originY)
            .graphicsLayer { translationX = dx.toFloat(); translationY = dy.toFloat() }
            .alpha(alpha.value)
            .scale(particle.scale)
            .rotate(rotate.value),
    )
}
