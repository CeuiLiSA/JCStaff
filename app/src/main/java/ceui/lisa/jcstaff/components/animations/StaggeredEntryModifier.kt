package ceui.lisa.jcstaff.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Staggered fade-in + slide-up entry animation for list items.
 * Each item fades in and slides up from 24dp below, with a per-item delay.
 * Only animates the first appearance (won't re-animate on scroll back).
 */
fun Modifier.staggeredFadeIn(index: Int): Modifier = composed {
    val density = LocalDensity.current
    val offsetPx = with(density) { 24.dp.toPx() }
    var hasAppeared by remember { mutableStateOf(false) }
    val alpha = remember { Animatable(if (hasAppeared) 1f else 0f) }
    val translationY = remember { Animatable(if (hasAppeared) 0f else offsetPx) }

    LaunchedEffect(Unit) {
        if (!hasAppeared) {
            val delay = minOf(index, 10) * 50
            alpha.animateTo(1f, animationSpec = tween(300, delayMillis = delay))
        }
    }
    LaunchedEffect(Unit) {
        if (!hasAppeared) {
            val delay = minOf(index, 10) * 50
            translationY.animateTo(0f, animationSpec = tween(300, delayMillis = delay))
            hasAppeared = true
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}
