package ceui.lisa.jcstaff.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import ceui.lisa.jcstaff.core.SettingsStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.network.Illust
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun IllustCard(
    illust: Illust,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val context = LocalContext.current
    val previewUrl = illust.previewUrl()
    val aspectRatio = illust.aspectRatio()
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val cornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        Box {
            val imageModifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)

            val finalModifier = if (sharedTransitionScope != null && animatedContentScope != null) {
                with(sharedTransitionScope) {
                    imageModifier.sharedElement(
                        state = rememberSharedContentState(key = "illust-${illust.id}"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = IllustBoundsTransform
                    )
                }
            } else {
                imageModifier
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(previewUrl)
                    .crossfade(true)
                    .addHeader("Referer", "https://app-api.pixiv.net/")
                    .build(),
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = finalModifier
            )

            // Page count badge
            if (illust.page_count > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${illust.page_count}P",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Bookmark indicator
            if (illust.is_bookmarked == true) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "已收藏",
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }

        if (showIllustInfo) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = illust.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = illust.user?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * iOS 风格的 shared element 过渡动画
 * 使用 spring 动画实现自然的弹性效果
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val IllustBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}
