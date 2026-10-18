package com.smartpreorder.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Fades + slides an element up the first time it appears. Use [delayMs] to stagger. */
@Composable
fun Modifier.appear(delayMs: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36.dp.toPx()
    }
}

/** Orange gradient avatar with a soft pulsing ring. */
@Composable
fun PulseAvatar(initial: String, size: Dp = 96.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val ring = t.animateFloat(
        initialValue = 1f, targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring"
    )
    val ringAlpha = t.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringAlpha"
    )
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    Box(Modifier.size(size * 1.5f).scale(pop.value), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).graphicsLayer {
                scaleX = ring.value; scaleY = ring.value; alpha = ringAlpha.value
            }.border(3.dp, BrandOrange, CircleShape)
        )
        Box(
            Modifier.size(size)
                .background(Brush.linearGradient(listOf(BrandOrange, BrandAmber)), CircleShape)
                .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initial, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp, style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
