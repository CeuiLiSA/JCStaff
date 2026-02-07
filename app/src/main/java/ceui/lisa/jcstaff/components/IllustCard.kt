package ceui.lisa.jcstaff.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IllustCard(
    illust: Illust,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showIllustInfo: Boolean = true,
    cornerRadius: Int = 8
) {
    val context = LocalContext.current
    val selectionManager = LocalSelectionManager.current
    val navViewModel = LocalNavigationViewModel.current
    val coroutineScope = rememberCoroutineScope()
    val isSelectionMode = selectionManager.isSelectionMode
    val isSelected = selectionManager.isSelected(illust.id)
    val previewUrl = illust.previewUrl()
    val aspectRatio = illust.aspectRatio()

    // Bookmark state (optimistic UI)
    var isBookmarked by remember(illust.id, illust.is_bookmarked) {
        mutableStateOf(illust.is_bookmarked == true)
    }
    var isBookmarking by remember { mutableStateOf(false) }

    // Selection scale animation
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "selection_scale"
    )

    // Bookmark button animations
    val bookmarkScale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bookmark_scale"
    )
    val bookmarkTint by animateColorAsState(
        targetValue = if (isBookmarked) Color(0xFFE91E63) else Color.White,
        label = "bookmark_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(cornerRadius.dp)
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
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(previewUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
            )

            // Selection indicator (top-start)
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(
                                alpha = 0.4f
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        ),
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

            // Badges (GIF, page count) — top-end
            if (illust.isGif() || illust.page_count > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (illust.isGif()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00BCD4).copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "GIF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    if (illust.page_count > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
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
                                    text = "${illust.page_count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bookmark button — bottom-end, always visible
            IconButton(
                onClick = {
                    if (isBookmarking) return@IconButton
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
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .scale(bookmarkScale)
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

        if (showIllustInfo) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = illust.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = illust.user?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        illust.user?.id?.let { userId ->
                            navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                        }
                    }
                )
            }
        }
    }
}
