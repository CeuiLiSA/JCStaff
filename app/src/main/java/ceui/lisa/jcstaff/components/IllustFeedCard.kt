package ceui.lisa.jcstaff.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.components.illust.tagGradients
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.utils.formatCount
import ceui.lisa.jcstaff.utils.formatRelativeDate
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 社交信息流风格的插画卡片 — MD3 Fancy 版
 *
 * Self-contained：内部处理导航、收藏、分享，外部只需传 illust + onClick
 *
 * 视觉层次（从重到轻）：
 * 1. 图片 + 渐变遮罩 + 统计 overlay + 浮动收藏按钮
 * 2. 用户头像和名称 - 内容来源
 * 3. 标题 - 作品信息
 * 4. 渐变 pill 标签
 * 5. 操作栏（FilledTonalIconButton）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IllustFeedCard(
    illust: Illust,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navViewModel = LocalNavigationViewModel.current
    val coroutineScope = rememberCoroutineScope()
    val user = illust.user

    // ── Bookmark state (optimistic UI) ──
    var isBookmarked by remember(illust.id, illust.is_bookmarked) {
        mutableStateOf(illust.is_bookmarked == true)
    }
    var isBookmarking by remember { mutableStateOf(false) }

    // ── Double-tap heart animation ──
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            heartScale.snapTo(0f)
            heartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            delay(400)
            heartScale.animateTo(0f)
            showDoubleTapHeart = false
        }
    }

    // ── Shared bookmark toggle logic ──
    fun performBookmarkToggle() {
        if (isBookmarking) return
        val wasBookmarked = isBookmarked
        isBookmarked = !wasBookmarked // optimistic flip
        coroutineScope.launch {
            isBookmarking = true
            try {
                if (wasBookmarked) {
                    PixivClient.pixivApi.deleteBookmark(illust.id)
                } else {
                    PixivClient.pixivApi.addBookmark(illust.id, "public")
                }
                val updatedIllust = illust.copy(is_bookmarked = !wasBookmarked)
                ObjectStore.put(updatedIllust)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isBookmarked = wasBookmarked // rollback
                e.printStackTrace()
            } finally {
                isBookmarking = false
            }
        }
    }

    // ── Share logic ──
    fun performShare() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://www.pixiv.net/artworks/${illust.id}")
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    // ── Navigation helpers ──
    fun navigateToUser() {
        user?.id?.let { userId ->
            navViewModel.navigate(NavRoute.UserProfile(userId = userId))
        }
    }

    fun navigateToTag(tag: Tag) {
        BrowseHistoryRepository.recordSearch(tag)
        navViewModel.navigate(NavRoute.TagDetail(tag = tag))
    }

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
                CircleAvatar(
                    imageUrl = user?.profile_image_urls?.findAvatarUrl(),
                    size = 48.dp,
                    contentDescription = user?.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = ::navigateToUser)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user?.name ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(onClick = ::navigateToUser)
                        )

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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${user?.account ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = " \u00b7 ",
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
            // 主图区域 + 渐变遮罩 + 统计 overlay + 浮动收藏按钮 + 双击心形
            // ═══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(illust.id) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onDoubleTap = {
                                if (!isBookmarked) {
                                    performBookmarkToggle()
                                }
                                showDoubleTapHeart = true
                            }
                        )
                    }
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

                // 底部渐变遮罩（透明 → 半透明黑）
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // 统计数据 overlay（图片底部左侧 pill）
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 浏览量
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatCount(illust.total_view ?: 0),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // 收藏数
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatCount(illust.total_bookmarks ?: 0),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // 浮动收藏按钮（图片右下角）
                FeedBookmarkButton(
                    isBookmarked = isBookmarked,
                    onClick = ::performBookmarkToggle,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )

                // 左上角：类型/AI 标记
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (illust.illust_ai_type == 2) {
                        BadgeChip(
                            text = stringResource(R.string.ai),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    if (illust.isGif()) {
                        BadgeChip(
                            text = "GIF",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (illust.isManga()) {
                        BadgeChip(
                            text = stringResource(R.string.manga),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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

                // 双击大心形动画
                if (showDoubleTapHeart) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                            .scale(heartScale.value)
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // 标签（渐变 pill，最多显示 5 个）
            // ═══════════════════════════════════════════════════════════════
            val tags = illust.tags?.take(5) ?: emptyList()
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEachIndexed { index, tag ->
                        val gradientIndex = index % tagGradients.size
                        val gradientColors = tagGradients[gradientIndex]
                        val cornerRadius = 8.dp

                        Text(
                            text = "#${tag.name ?: ""}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = gradientColors[0],
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            gradientColors[0].copy(alpha = 0.12f),
                                            gradientColors[1].copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .drawBehind {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.3f),
                                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                .clickable { navigateToTag(tag) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * 小标签芯片（用于 AI、GIF、漫画等标记）
 */
@Composable
private fun BadgeChip(
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
 * 图片上浮动的收藏按钮（圆形半透明底 + spring 缩放 + 颜色动画）
 */
@Composable
private fun FeedBookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "feed_bm_scale"
    )

    val tint by animateColorAsState(
        targetValue = if (isBookmarked) Color(0xFFE91E63) else Color.White,
        label = "feed_bm_color"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isBookmarked) "取消收藏" else "收藏",
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 操作栏中的收藏按钮（FilledTonalIconButton + spring 动画 + 颜色动画）
 */
@Composable
private fun ActionBarBookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "action_bm_scale"
    )

    val bookmarkPink = Color(0xFFE91E63)

    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .scale(scale),
        colors = if (isBookmarked) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = bookmarkPink.copy(alpha = 0.15f),
                contentColor = bookmarkPink
            )
        } else {
            IconButtonDefaults.filledTonalIconButtonColors()
        }
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(if (isBookmarked) R.string.following else R.string.follow),
            modifier = Modifier.size(20.dp)
        )
    }
}
