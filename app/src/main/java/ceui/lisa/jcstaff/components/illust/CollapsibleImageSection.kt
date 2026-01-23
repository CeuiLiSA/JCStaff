package ceui.lisa.jcstaff.components.illust

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.IllustBoundsTransform
import ceui.lisa.jcstaff.components.ProgressiveImage
import ceui.lisa.jcstaff.network.Illust

/**
 * 可折叠图片区域组件
 * 收起状态：图片区域高度固定为屏幕75%，超出部分裁剪
 * 展开状态：所有图片完整展示
 * 右下角悬浮按钮控制展开/收起
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollapsibleImageSection(
    illustId: Long,
    title: String,
    previewUrl: String,
    aspectRatio: Float,
    illust: Illust?,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onImageClick: ((previewUrl: String, originalUrl: String?, sharedElementKey: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // 计算屏幕75%高度作为收起时的最大高度
    val screenHeightDp = configuration.screenHeightDp.dp
    val collapsedMaxHeight = screenHeightDp * 0.75f
    val collapsedMaxHeightPx = with(density) { collapsedMaxHeight.roundToPx() }

    // 追踪内容实际高度
    var contentHeightPx by remember { mutableIntStateOf(0) }

    // 判断是否需要显示展开/收起按钮
    val needsExpandButton = contentHeightPx > collapsedMaxHeightPx

    // 计算目标高度
    val targetHeightPx = if (isExpanded || !needsExpandButton) {
        contentHeightPx.coerceAtLeast(collapsedMaxHeightPx)
    } else {
        collapsedMaxHeightPx
    }
    val targetHeight = with(density) { targetHeightPx.toDp() }

    // 动画过渡高度
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = 350),
        label = "image_section_height"
    )

    // 获取第一张原图URL
    val firstOriginalUrl = remember(illust) {
        illust?.let { loadedIllust ->
            if (loadedIllust.page_count == 1) {
                loadedIllust.meta_single_page?.original_image_url
            } else {
                loadedIllust.meta_pages?.firstOrNull()?.image_urls?.original
            }
        }
    }

    Box(modifier = modifier) {
        // 使用自定义 Layout 来测量真实内容高度，同时应用动画高度约束
        Layout(
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 第一张图片 - 带共享元素动画
                    val firstImageKey = "${illustId}_0"
                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "illust-$illustId"),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = IllustBoundsTransform
                                )
                        ) {
                            ProgressiveImage(
                                previewUrl = previewUrl,
                                originalUrl = firstOriginalUrl,
                                contentDescription = title,
                                modifier = Modifier.fillMaxWidth(),
                                sharedElementKey = firstImageKey,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope,
                                onClick = {
                                    onImageClick?.invoke(
                                        previewUrl,
                                        firstOriginalUrl,
                                        firstImageKey
                                    )
                                }
                            )
                        }
                    }

                    // 多P作品的额外图片
                    illust?.let { loadedIllust ->
                        if (loadedIllust.page_count > 1) {
                            val additionalPages = loadedIllust.meta_pages?.drop(1) ?: emptyList()
                            additionalPages.forEachIndexed { index, page ->
                                val pageIndex = index + 1
                                val imageKey = "${illustId}_$pageIndex"
                                val largeUrl = page.image_urls?.large ?: ""
                                val originalUrl = page.image_urls?.original

                                ProgressiveImage(
                                    previewUrl = largeUrl,
                                    originalUrl = originalUrl,
                                    contentDescription = loadedIllust.title,
                                    modifier = Modifier.fillMaxWidth(),
                                    sharedElementKey = imageKey,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    onClick = {
                                        onImageClick?.invoke(largeUrl, originalUrl, imageKey)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
        ) { measurables, constraints ->
            // 测量内容时不限制高度，以获取真实高度
            val unconstrainedConstraints = constraints.copy(maxHeight = Constraints.Infinity)
            val placeable = measurables.first().measure(unconstrainedConstraints)

            // 更新真实内容高度
            if (placeable.height > 0) {
                contentHeightPx = placeable.height
            }

            // 使用动画高度作为布局高度
            val animatedHeightPx = with(density) { animatedHeight.roundToPx() }
            val layoutHeight = animatedHeightPx.coerceAtMost(placeable.height)

            layout(placeable.width, layoutHeight) {
                placeable.place(0, 0)
            }
        }

        // 展开/收起按钮
        if (needsExpandButton) {
            Surface(
                onClick = onExpandToggle,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) {
                                stringResource(R.string.collapse_images)
                            } else {
                                stringResource(R.string.expand_images)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (isExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
