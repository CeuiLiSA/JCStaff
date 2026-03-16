package ceui.lisa.jcstaff.components.animations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Elastic tab indicator that stretches during swipe via PagerState offset.
 * Must be called within a TabIndicatorScope (PrimaryTabRow's indicator lambda).
 */
@Composable
fun TabIndicatorScope.ElasticTabIndicator(
    pagerState: PagerState,
    tabCount: Int,
    modifier: Modifier = Modifier,
    baseWidth: Dp = 32.dp,
    height: Dp = 3.dp
) {
    val color = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val baseWidthPx = with(density) { baseWidth.toPx() }
    val heightPx = with(density) { height.roundToPx() }

    Box(
        modifier = modifier
            .tabIndicatorLayout { measurable, constraints, tabPositions ->
                val fraction = pagerState.currentPageOffsetFraction
                val currentPage = pagerState.currentPage
                val absFraction = abs(fraction)

                // Stretch factor: 1.0 at rest, up to ~1.5 at midpoint
                val stretchFactor = 1f + 2f * absFraction * (1f - absFraction)
                val indicatorWidthPx = (baseWidthPx * stretchFactor).toInt()

                // Get actual tab centers from tabPositions
                val currentTabCenter = (tabPositions[currentPage].left.toPx() +
                        tabPositions[currentPage].right.toPx()) / 2f
                val targetPage = if (fraction > 0) {
                    (currentPage + 1).coerceAtMost(tabPositions.lastIndex)
                } else {
                    (currentPage - 1).coerceAtLeast(0)
                }
                val targetTabCenter = (tabPositions[targetPage].left.toPx() +
                        tabPositions[targetPage].right.toPx()) / 2f
                val centerX = currentTabCenter +
                        (targetTabCenter - currentTabCenter) * absFraction

                val placeable = measurable.measure(
                    Constraints.fixed(indicatorWidthPx, heightPx)
                )

                layout(constraints.maxWidth, placeable.height) {
                    placeable.placeRelative(
                        x = (centerX - placeable.width / 2f).toInt(),
                        y = 0
                    )
                }
            }
            .background(
                color = color,
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
    )
}
