package ceui.lisa.jcstaff.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val angle: Float,       // radians
    val speed: Float,       // pixels per unit time
    val color: Color,
    val isHeart: Boolean,
    val size: Float
)

/**
 * Canvas-based particle burst animation for bookmark actions.
 * Spawns 15-20 particles in a burst pattern with parabolic trajectories.
 *
 * @param trigger Set to true to fire the burst. Resets automatically.
 */
@Composable
fun ParticleBurst(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            Color(0xFFE91E63),
            Color(0xFFF44336),
            Color(0xFFFF5252),
            Color(0xFFFF80AB),
            Color(0xFFFF4081)
        )
        List(Random.nextInt(15, 21)) {
            Particle(
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                speed = Random.nextFloat() * 120f + 60f,
                color = colors.random(),
                isHeart = Random.nextBoolean(),
                size = Random.nextFloat() * 4f + 3f
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(600))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val t = progress.value
        val alpha = (1f - t).coerceIn(0f, 1f)
        val gravity = 200f

        particles.forEach { particle ->
            val dx = cos(particle.angle) * particle.speed * t
            val dy = sin(particle.angle) * particle.speed * t + 0.5f * gravity * t * t
            val x = centerX + dx
            val y = centerY + dy

            if (particle.isHeart) {
                drawHeart(
                    center = Offset(x, y),
                    size = particle.size * (1f - t * 0.3f),
                    color = particle.color.copy(alpha = alpha)
                )
            } else {
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size * (1f - t * 0.3f),
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        val s = size
        moveTo(center.x, center.y + s * 0.4f)
        cubicTo(
            center.x - s, center.y - s * 0.2f,
            center.x - s * 0.5f, center.y - s,
            center.x, center.y - s * 0.4f
        )
        cubicTo(
            center.x + s * 0.5f, center.y - s,
            center.x + s, center.y - s * 0.2f,
            center.x, center.y + s * 0.4f
        )
        close()
    }
    drawPath(path, color)
}
