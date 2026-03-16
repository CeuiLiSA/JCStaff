package ceui.lisa.jcstaff.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceui.lisa.jcstaff.components.animations.ParticleBurst
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IllustCard(
    illust: Illust,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val selectionManager = LocalSelectionManager.current

    val isSelectionMode by remember { derivedStateOf { selectionManager.isSelectionMode } }
    val isSelected = if (isSelectionMode) selectionManager.isSelected(illust.id) else false

    val previewUrl = remember(illust.id) { illust.previewUrl() }
    val aspectRatio = remember(illust.id) { illust.aspectRatio() }

    val painter = rememberAsyncImagePainter(model = previewUrl)

    val scale = if (isSelectionMode) {
        val animatedScale by animateFloatAsState(
            targetValue = if (isSelected) 0.92f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "selection_scale"
        )
        animatedScale
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        selectionManager.toggleSelection(illust)
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        selectionManager.onLongPress(illust)
                    }
                }
            )
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )

        if (isSelectionMode) {
            SelectionIndicator(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        if (illust.isGif() || illust.page_count > 1) {
            BadgeRow(
                isGif = illust.isGif(),
                pageCount = illust.page_count,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(
                    alpha = 0.4f
                ),
                shape = CircleShape
            )
            .border(width = 2.dp, color = Color.White, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "已选中",
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private val BadgeShape = RoundedCornerShape(6.dp)
private val GifBadgeColor = Color(0xFF00BCD4).copy(alpha = 0.9f)
private val PageCountBadgeColor = Color.Black.copy(alpha = 0.7f)

@Composable
private fun BadgeRow(
    isGif: Boolean,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isGif) {
            Text(
                text = "GIF",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .background(GifBadgeColor, BadgeShape)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .background(PageCountBadgeColor, BadgeShape)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "$pageCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * 收藏按钮 — 极简版，首次组合仅创建静态图标
 * 动画状态（scale/color/particle）仅在用户点击后才创建
 */
@Composable
private fun BookmarkButton(
    illust: Illust,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isBookmarked by remember(illust.id, illust.is_bookmarked) {
        mutableStateOf(illust.is_bookmarked == true)
    }
    var isBookmarking by remember { mutableStateOf(false) }

    // 是否曾经被点击过 — 只有点击后才创建动画状态
    var hasInteracted by remember { mutableStateOf(false) }
    var showParticleBurst by remember { mutableStateOf(false) }

    // 动画仅在交互后创建
    val bookmarkScale = if (hasInteracted) {
        val animated by animateFloatAsState(
            targetValue = if (isBookmarked) 1.15f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "bookmark_scale"
        )
        animated
    } else {
        1f
    }
    val bookmarkTint = if (hasInteracted) {
        val animated by animateColorAsState(
            targetValue = if (isBookmarked) Color(0xFFE91E63) else Color.White,
            label = "bookmark_color"
        )
        animated
    } else {
        if (isBookmarked) Color(0xFFE91E63) else Color.White
    }

    Box(modifier = modifier) {
        if (hasInteracted) {
            ParticleBurst(
                trigger = showParticleBurst,
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(bookmarkScale)
                .clickable {
                    if (isBookmarking) return@clickable
                    hasInteracted = true
                    val wasBookmarked = isBookmarked
                    isBookmarked = !wasBookmarked
                    if (!wasBookmarked) showParticleBurst = true
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
                            isBookmarked = wasBookmarked
                            e.printStackTrace()
                        } finally {
                            isBookmarking = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isBookmarked) "取消收藏" else "收藏",
                    tint = bookmarkTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
