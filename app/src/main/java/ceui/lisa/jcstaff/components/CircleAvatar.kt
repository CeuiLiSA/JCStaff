package ceui.lisa.jcstaff.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 统一的圆形头像组件，自动根据尺寸调整 border 宽度
 * - 超大头像 (>= 80dp): border 3dp
 * - 大头像 (>= 64dp): border 2dp
 * - 中等头像 (48-63dp): border 1.5dp
 * - 小头像 (< 48dp): border 1dp
 */
@Composable
fun CircleAvatar(
    imageUrl: String?,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
) {
    val context = LocalContext.current
    val borderWidth = when {
        size >= 80.dp -> 3.dp
        size >= 64.dp -> 2.dp
        size >= 48.dp -> 1.5.dp
        else -> 1.dp
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .border(borderWidth, borderColor, CircleShape)
            .clip(CircleShape)
    )
}
