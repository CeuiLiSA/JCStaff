package ceui.lisa.jcstaff.components.appswitcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ceui.lisa.jcstaff.navigation.AppSwitcherState
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.navigation.getTitle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AppSwitcherOverlay(
    backStack: List<NavRoute>,
    screenshotStore: ScreenshotStore,
    state: AppSwitcherState,
    onCardClick: (Int) -> Unit,
    onDeleteCard: (Int) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Continuous scroll position in card-index space (e.g. 3.0 = card 3 centered).
    // Using a continuous float avoids the discrete selectedIndex snap that caused
    // surrounding cards to jump when their left/right spacing recalculated.
    var isDragging by remember { mutableStateOf(false) }
    var dragScrollPos by remember { mutableFloatStateOf(0f) }
    val animScrollPos = remember { Animatable(0f) }

    var deleteCardIndex by remember { mutableStateOf<Int?>(null) }
    var deleteOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.selectedIndex, state.isVisible) {
        if (state.isVisible) {
            val target = state.selectedIndex.toFloat()
            dragScrollPos = target
            animScrollPos.snapTo(target)
            deleteCardIndex = null
            deleteOffset = 0f
            isDragging = false
        }
    }

    if (!state.isVisible) return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        val screenAspectRatio = screenHeightPx / screenWidthPx
        val cardWidthPx = screenWidthPx * 0.70f
        val cardHeightPx = cardWidthPx * screenAspectRatio
        val cardWidthDp = with(density) { cardWidthPx.toDp() }
        val cardHeightDp = with(density) { cardHeightPx.toDp() }
        // iOS-style asymmetric spacing:
        // Left cards are tightly stacked (small peek), right cards are far apart
        // so the selected card is nearly fully visible.
        val leftSpacingPx = cardWidthPx * 0.28f
        val rightSpacingPx = cardWidthPx * 0.95f
        // Drag sensitivity: how many pixels of drag = 1 card scroll.
        // Larger value = need to drag further to switch one card.
        val dragSpacingPx = cardWidthPx * 0.65f
        val titleHeightPx = with(density) { 32.dp.toPx() }
        val centerX = screenWidthPx / 2f
        val centerY = screenHeightPx / 2f

        val scrollPos = if (isDragging) dragScrollPos else animScrollPos.value
        val currentIndex = scrollPos.roundToInt()
            .coerceIn(0, (backStack.size - 1).coerceAtLeast(0))

        // Position card based on continuous scrollPos — no discrete jumps.
        // Overscroll region uses rightSpacingPx uniformly so the rubber-band
        // visual displacement is equally strong on both edges.
        val maxScrollIndex = (backStack.size - 1).coerceAtLeast(0).toFloat()
        fun cardCenterX(index: Int, sp: Float = scrollPos): Float {
            val clampedSp = sp.coerceIn(0f, maxScrollIndex)
            val overscroll = sp - clampedSp // <0 past left, >0 past right
            val relPos = index.toFloat() - clampedSp
            val baseX = centerX + relPos * (if (relPos <= 0f) leftSpacingPx else rightSpacingPx)
            return baseX - overscroll * rightSpacingPx
        }

        // 1) Cards — iOS style: all same size, opaque, with shadow, right stacks on top of left
        backStack.forEachIndexed { index, route ->
            val baseX = cardCenterX(index)

            // iOS-style: right card always on top of left card
            val zIndex = index.toFloat()

            val cardDeleteOffset = if (deleteCardIndex == index) deleteOffset else 0f
            val dismissProgress = (cardDeleteOffset / -300f).coerceIn(0f, 1f)
            val dismissScale = 1f - dismissProgress * 0.2f
            val totalHeight = cardHeightPx + titleHeightPx

            Column(
                modifier = Modifier
                    .zIndex(zIndex)
                    .offset {
                        IntOffset(
                            x = (baseX - cardWidthPx / 2f).roundToInt(),
                            y = (centerY - totalHeight / 2f + cardDeleteOffset).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        scaleX = dismissScale
                        scaleY = dismissScale
                        this.alpha = 1f - dismissProgress
                    }
            ) {
                Text(
                    text = route.getTitle(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(cardWidthDp)
                        .padding(bottom = 8.dp)
                )
                AppSwitcherCard(
                    screenshot = screenshotStore.getScreenshot(route.stableKey),
                    onClick = { },
                    modifier = Modifier
                        .width(cardWidthDp)
                        .height(cardHeightDp)
                )
            }
        }

        // 2) Transparent gesture layer (rendered last = on top, intercepts all touch)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(Float.MAX_VALUE)
                .pointerInput(backStack.size) {
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var dragDirection: Int? = null
                    val velocityTracker = VelocityTracker()

                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            totalDragX = 0f
                            totalDragY = 0f
                            dragDirection = null
                            velocityTracker.resetTracking()
                            dragScrollPos = animScrollPos.value
                        },
                        onDragEnd = {
                            when (dragDirection) {
                                0 -> {
                                    val velocityX = velocityTracker.calculateVelocity().x
                                    // Convert pixel velocity to index-space velocity
                                    val velocityInIndex = -velocityX / dragSpacingPx
                                    val projected = dragScrollPos + velocityInIndex * 0.25f
                                    val targetIndex = projected.roundToInt()
                                        .coerceIn(0, backStack.size - 1)

                                    val maxIndex = (backStack.size - 1).coerceAtLeast(0)
                                    val isOverscrolled =
                                        dragScrollPos < 0f || dragScrollPos > maxIndex.toFloat()

                                    coroutineScope.launch {
                                        isDragging = false
                                        animScrollPos.snapTo(dragScrollPos)
                                        animScrollPos.animateTo(
                                            targetValue = targetIndex.toFloat(),
                                            initialVelocity = if (isOverscrolled) 0f else velocityInIndex,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                        onSelectedIndexChange(targetIndex)
                                    }
                                }

                                1 -> {
                                    if (deleteOffset < -150f && backStack.size > 1 && deleteCardIndex != null) {
                                        onDeleteCard(deleteCardIndex!!)
                                    }
                                    deleteCardIndex = null
                                    deleteOffset = 0f
                                    isDragging = false
                                }

                                else -> {
                                    isDragging = false
                                }
                            }
                        },
                        onDragCancel = {
                            val target = dragScrollPos.roundToInt()
                                .coerceIn(0, (backStack.size - 1).coerceAtLeast(0))
                            coroutineScope.launch {
                                isDragging = false
                                animScrollPos.snapTo(dragScrollPos)
                                animScrollPos.animateTo(target.toFloat())
                            }
                            deleteCardIndex = null
                            deleteOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (dragDirection == null && (abs(totalDragX) > 15f || abs(totalDragY) > 15f)) {
                                dragDirection = if (abs(totalDragX) > abs(totalDragY)) 0 else 1
                            }

                            when (dragDirection) {
                                0 -> {
                                    val maxIndex = (backStack.size - 1).coerceAtLeast(0).toFloat()
                                    val delta = -dragAmount.x / dragSpacingPx
                                    // Rubber-band: 30% friction when past the edge
                                    val friction = if (
                                        (dragScrollPos <= 0f && delta < 0f) ||
                                        (dragScrollPos >= maxIndex && delta > 0f)
                                    ) 0.3f else 1f
                                    dragScrollPos += delta * friction
                                }

                                1 -> {
                                    if (dragAmount.y < 0 || deleteOffset < 0) {
                                        deleteOffset =
                                            (deleteOffset + dragAmount.y).coerceAtMost(0f)
                                        deleteCardIndex = dragScrollPos.roundToInt()
                                            .coerceIn(0, backStack.size - 1)
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(backStack.size) {
                    detectTapGestures(
                        onTap = { offset ->
                            val tapX = offset.x
                            val tapY = offset.y
                            val totalHeight = cardHeightPx + titleHeightPx

                            // Read the latest scroll position at tap time
                            val sp = animScrollPos.value
                            var tappedIndex: Int? = null
                            // Check from highest z-index (rightmost) to lowest
                            for (i in backStack.indices.reversed()) {
                                val cx = cardCenterX(i, sp)
                                val cardLeft = cx - cardWidthPx / 2f
                                val cardRight = cx + cardWidthPx / 2f
                                val cardTop = centerY - totalHeight / 2f
                                val cardBottom = cardTop + totalHeight

                                if (tapX in cardLeft..cardRight && tapY in cardTop..cardBottom) {
                                    tappedIndex = i
                                    break
                                }
                            }

                            if (tappedIndex != null) {
                                onCardClick(tappedIndex)
                            } else {
                                onDismiss()
                            }
                        }
                    )
                }
        )
    }
}
