package ceui.lisa.jcstaff.components.illust

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.ImageDownloader
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.utils.formatCount
import kotlinx.coroutines.launch

/**
 * 作品操作按钮栏组件
 * 包含收藏、下载和浏览数显示
 */
@Composable
fun IllustActionBar(
    illust: Illust,
    isBookmarked: Boolean,
    downloadUrl: String,
    onBookmarkStateChanged: (Boolean, Illust) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isBookmarking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 收藏按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = !isBookmarking) {
                    coroutineScope.launch {
                        isBookmarking = true
                        try {
                            if (isBookmarked) {
                                PixivClient.pixivApi.deleteBookmark(illust.id)
                            } else {
                                PixivClient.pixivApi.addBookmark(illust.id)
                            }
                            val newBookmarkState = !isBookmarked
                            val updatedIllust = illust.copy(is_bookmarked = newBookmarkState)
                            ObjectStore.put(updatedIllust)
                            onBookmarkStateChanged(newBookmarkState, updatedIllust)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isBookmarking = false
                        }
                    }
                }
                .background(
                    if (isBookmarked) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isBookmarked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = formatCount(illust.total_bookmarks ?: 0),
                style = MaterialTheme.typography.labelLarge,
                color = if (isBookmarked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        // 下载按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = !isDownloading) {
                    coroutineScope.launch {
                        isDownloading = true
                        val fileName = "pixiv_${illust.id}_${System.currentTimeMillis()}"
                        val cachedFilePath = LoadTaskManager.getCachedFilePath(downloadUrl)
                        val result = if (cachedFilePath != null) {
                            ImageDownloader.saveFromCacheToGallery(
                                context = context,
                                cachedFilePath = cachedFilePath,
                                fileName = fileName
                            )
                        } else {
                            ImageDownloader.downloadToGallery(
                                context = context,
                                imageUrl = downloadUrl,
                                fileName = fileName
                            )
                        }
                        isDownloading = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "下载",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 浏览数
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = formatCount(illust.total_view ?: 0),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
