package ceui.lisa.jcstaff.components.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MAX_PULL_PX = 400f
private const val TRIGGER_THRESHOLD_PX = 200f

/**
 * Elastic pull-to-refresh with rubber-band effect and custom rotating arrow indicator.
 * Replaces PullToRefreshBox with a more polished interaction.
 */
@Composable
fun ElasticPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isTriggered by remember { mutableStateOf(false) }
    val animatedOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Reset when refresh completes
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullOffset > 0f) {
            pullOffset = 0f
            animatedOffset.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            isTriggered = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When pulling up while overscrolled, consume scroll to reduce offset
                if (pullOffset > 0f && available.y < 0f) {
                    val consumed = available.y
                    pullOffset = (pullOffset + consumed).coerceAtLeast(0f)
                    coroutineScope.launch { animatedOffset.snapTo(pullOffset) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Only engage when scrolling down and there's leftover
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    // Rubber-band: diminishing returns
                    val rubberBand = available.y * (1f - pullOffset / MAX_PULL_PX)
                    pullOffset = (pullOffset + rubberBand).coerceIn(0f, MAX_PULL_PX)
                    coroutineScope.launch { animatedOffset.snapTo(pullOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset > TRIGGER_THRESHOLD_PX || abs(available.y) > 1000f && pullOffset > 50f) {
                    isTriggered = true
                    onRefresh()
                    // Snap to a small refreshing position
                    animatedOffset.animateTo(
                        80f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    pullOffset = 80f
                } else if (pullOffset > 0f) {
                    pullOffset = 0f
                    animatedOffset.animateTo(
                        0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    val displayOffset = animatedOffset.value
    val progress = (displayOffset / TRIGGER_THRESHOLD_PX).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Content with translation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, displayOffset.toInt()) }
        ) {
            content()
        }

        // Indicator
        if (displayOffset > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (displayOffset - 56.dp.toPx()).toInt().coerceAtLeast(0)) },
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val rotation = if (progress >= 1f) 180f else progress * 180f
                    val indicatorScale = (0.5f + progress * 0.5f).coerceAtMost(1f)
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(indicatorScale)
                            .rotate(rotation)
                    )
                }
            }
        }
    }
}
