package ceui.lisa.jcstaff.components.animations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Animated counter that transitions each digit independently with a vertical slide.
 * When the number increases, digits slide up; when it decreases, digits slide down.
 */
@Composable
fun AnimatedCounter(
    count: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    Row(modifier = modifier) {
        count.forEach { char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    val isIncreasing = targetState > initialState
                    if (isIncreasing) {
                        (slideInVertically { it } + fadeIn()) togetherWith
                                (slideOutVertically { -it } + fadeOut())
                    } else {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut())
                    } using SizeTransform(clip = true)
                },
                label = "digit_$char"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = style,
                    color = color
                )
            }
        }
    }
}
