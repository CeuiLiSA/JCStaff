package ceui.lisa.jcstaff.components.animations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Elastic tab indicator that stretches during swipe via PagerState offset.
 * The indicator width expands up to 1.5x at the midpoint of a page transition.
 */
@Composable
fun ElasticTabIndicator(
    pagerState: PagerState,
    tabCount: Int,
    modifier: Modifier = Modifier,
    baseWidth: Dp = 32.dp,
    height: Dp = 3.dp
) {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier.height(height),
        contentAlignment = Alignment.BottomStart
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val fraction = pagerState.currentPageOffsetFraction
        val currentPage = pagerState.currentPage
        val absFraction = abs(fraction)

        // Stretch factor: 1.0 at rest, up to ~1.5 at midpoint
        val stretchFactor = 1f + 2f * absFraction * (1f - absFraction)
        val indicatorWidth = baseWidth * stretchFactor

        // Position: center of the current tab interpolated with offset
        val tabWidthPx = totalWidthPx / tabCount
        val centerXPx = (currentPage + fraction) * tabWidthPx + tabWidthPx / 2f
        val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
        val offsetXPx = (centerXPx - indicatorWidthPx / 2f).toInt()

        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(height)
                .offset { IntOffset(offsetXPx, 0) }
                .background(
                    color = color,
                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
        )
    }
}
