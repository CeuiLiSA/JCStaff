package ceui.lisa.jcstaff.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.utils.formatRelativeDate
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.Locale

/**
 * 社交信息流风格的插画卡片
 * 参考 Twitter/微博的设计，遵循 Material Design 3 规范
 *
 * 视觉层次（从重到轻）：
 * 1. 图片 - 最吸引眼球的内容
 * 2. 用户头像和名称 - 内容来源
 * 3. 标题 - 作品信息
 * 4. 统计数据和标签 - 辅助信息
 * 5. 操作按钮 - 交互区域
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IllustFeedCard(
    illust: Illust,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    onTagClick: (Tag) -> Unit,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = illust.user

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            // ═══════════════════════════════════════════════════════════════
            // Header: 用户信息 + 时间
            // ═══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 用户头像
                CircleAvatar(
                    imageUrl = user?.profile_image_urls?.findAvatarUrl(),
                    size = 48.dp,
                    contentDescription = user?.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onUserClick)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 用户名和时间
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user?.name ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // 已关注标记
                        if (user?.is_followed == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = 1.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.following),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${user?.account ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        illust.create_date?.let { date ->
                            formatRelativeDate(date)?.let { dateText ->
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 更多按钮
                IconButton(
                    onClick = { /* TODO: 显示更多选项 */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // 标题（如果有）
            // ═══════════════════════════════════════════════════════════════
            if (!illust.title.isNullOrBlank()) {
                Text(
                    text = illust.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // 主图区域
            // ═══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // 主图
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(illust.image_urls?.large ?: illust.previewUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = illust.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            illust.aspectRatio().coerceIn(0.6f, 1.5f)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // 左上角：类型/AI 标记
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // AI 生成标记（仅显示 AI 生成，不显示 AI 辅助）
                    if (illust.illust_ai_type == 2) {
                        BadgeChip(
                            icon = Icons.Default.AutoAwesome,
                            text = stringResource(R.string.ai),
                            containerColor = Color(0xFF9C27B0).copy(alpha = 0.9f),
                            contentColor = Color.White
                        )
                    }

                    // Ugoira (GIF) 标记
                    if (illust.isGif()) {
                        BadgeChip(
                            icon = Icons.Default.Gif,
                            text = "GIF",
                            containerColor = Color(0xFF00BCD4).copy(alpha = 0.9f),
                            contentColor = Color.White
                        )
                    }

                    // 漫画标记
                    if (illust.isManga()) {
                        BadgeChip(
                            icon = Icons.Default.GridView,
                            text = stringResource(R.string.manga),
                            containerColor = Color(0xFFFF9800).copy(alpha = 0.9f),
                            contentColor = Color.White
                        )
                    }
                }

                // 右上角：多图标记
                if (illust.page_count > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${illust.page_count}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

            }

            // ═══════════════════════════════════════════════════════════════
            // 统计数据行
            // ═══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 浏览量
                StatItem(
                    icon = Icons.Default.Visibility,
                    count = illust.total_view ?: 0,
                    contentDescription = "Views"
                )

                // 收藏数
                StatItem(
                    icon = if (illust.is_bookmarked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    count = illust.total_bookmarks ?: 0,
                    contentDescription = "Bookmarks",
                    tint = if (illust.is_bookmarked == true) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (illust.is_bookmarked == true) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // 标签（最多显示 5 个）
            // ═══════════════════════════════════════════════════════════════
            val tags = illust.tags?.take(5) ?: emptyList()
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        Text(
                            text = "#${tag.name ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onTagClick(tag) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ═══════════════════════════════════════════════════════════════
            // 操作栏
            // ═══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 评论按钮
                ActionButton(
                    icon = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = stringResource(R.string.comments),
                    onClick = { /* TODO: 打开评论 */ }
                )

                // 收藏按钮（带动画）
                BookmarkButton(
                    isBookmarked = illust.is_bookmarked == true,
                    onClick = onBookmarkClick
                )

                // 分享按钮
                ActionButton(
                    icon = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share),
                    onClick = onShareClick
                )
            }

            // 分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}

/**
 * 小标签芯片（用于 AI、GIF、漫画等标记）
 */
@Composable
private fun BadgeChip(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * 统计数据项
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    count: Int,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = fontWeight,
            color = tint
        )
    }
}

/**
 * 操作按钮
 */
@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 收藏按钮（带动画效果）
 */
@Composable
private fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isBookmarked) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bookmark_scale"
    )

    val tint by animateColorAsState(
        targetValue = if (isBookmarked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "bookmark_color"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(if (isBookmarked) R.string.following else R.string.follow),
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 格式化数字（K/M 单位）
 */
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 10_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

