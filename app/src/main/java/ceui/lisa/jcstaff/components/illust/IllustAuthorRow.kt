package ceui.lisa.jcstaff.components.illust

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.User
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

/**
 * 作品作者信息行组件
 * 显示头像、用户名、账号名和关注按钮
 */
@Composable
fun IllustAuthorRow(
    user: User?,
    isFollowed: Boolean,
    onFollowStateChanged: (Boolean) -> Unit,
    onUserClick: ((Long) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isFollowing by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onUserClick != null && user != null) {
                user?.let { onUserClick?.invoke(it.id) }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(user?.profile_image_urls?.medium)
                .crossfade(true)
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build(),
            contentDescription = user?.name,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = user?.name ?: "",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user?.account ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 关注按钮
        user?.let { u ->
            if (isFollowed) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isFollowing = true
                            try {
                                PixivClient.pixivApi.unfollowUser(u.id)
                                onFollowStateChanged(false)
                                val updatedUser = u.copy(is_followed = false)
                                ObjectStore.put(updatedUser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "取消关注失败", Toast.LENGTH_SHORT).show()
                            } finally {
                                isFollowing = false
                            }
                        }
                    },
                    enabled = !isFollowing,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isFollowing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "已关注",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            isFollowing = true
                            try {
                                PixivClient.pixivApi.followUser(u.id)
                                onFollowStateChanged(true)
                                val updatedUser = u.copy(is_followed = true)
                                ObjectStore.put(updatedUser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "关注失败", Toast.LENGTH_SHORT).show()
                            } finally {
                                isFollowing = false
                            }
                        }
                    },
                    enabled = !isFollowing,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isFollowing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "关注",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
