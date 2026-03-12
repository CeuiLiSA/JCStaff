package ceui.lisa.jcstaff.components.animations

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Fullscreen context menu overlay triggered by long press.
 * Features blur background (SDK >= 31) and staggered scale-in menu items.
 *
 * @param visible Whether the overlay is shown
 * @param onDismiss Called when the overlay is dismissed (tap outside)
 * @param anchorOffset Screen position where the menu should anchor
 * @param items List of composable menu items
 */
@Composable
fun ContextMenuOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    anchorOffset: DpOffset = DpOffset.Zero,
    items: List<@Composable () -> Unit>
) {
    if (!visible) return

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= 31) {
                    Modifier.blur(20.dp)
                } else {
                    Modifier
                }
            )
            .background(
                if (Build.VERSION.SDK_INT >= 31) {
                    Color.Black.copy(alpha = 0.3f)
                } else {
                    Color.Black.copy(alpha = 0.6f)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Surface(
            modifier = Modifier
                .padding(
                    start = with(density) { anchorOffset.x.coerceAtLeast(16.dp) },
                    top = with(density) { anchorOffset.y.coerceAtLeast(16.dp) }
                )
                .widthIn(min = 180.dp, max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                items.forEachIndexed { index, item ->
                    var itemVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(visible) {
                        if (visible) {
                            delay(index * 30L)
                            itemVisible = true
                        } else {
                            itemVisible = false
                        }
                    }

                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        item()
                    }
                }
            }
        }
    }
}
