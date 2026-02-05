package ceui.lisa.jcstaff.components.illust

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import ceui.lisa.jcstaff.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.CircleAvatar
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.User
import kotlinx.coroutines.launch

/**
 * 作品作者信息行组件
 * 显示头像、用户名、账号名和关注按钮
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        CircleAvatar(
            imageUrl = user?.profile_image_urls?.findAvatarUrl(),
            size = 40.dp,
            contentDescription = user?.name,
            modifier = Modifier.size(40.dp)
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
                                Toast.makeText(context, context.getString(R.string.unfollow_failed), Toast.LENGTH_SHORT).show()
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
                            text = stringResource(R.string.following),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                var showMenu by remember { mutableStateOf(false) }

                val followAction = { restrict: String ->
                    coroutineScope.launch {
                        isFollowing = true
                        try {
                            PixivClient.pixivApi.followUser(u.id, restrict)
                            onFollowStateChanged(true)
                            val updatedUser = u.copy(is_followed = true)
                            ObjectStore.put(updatedUser)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.follow_failed), Toast.LENGTH_SHORT).show()
                        } finally {
                            isFollowing = false
                        }
                    }
                }

                Box {
                    SplitButtonLayout(
                        leadingButton = {
                            SplitButtonDefaults.TonalLeadingButton(
                                onClick = { followAction("public") },
                                enabled = !isFollowing,
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isFollowing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.follow),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        },
                        trailingButton = {
                            SplitButtonDefaults.TonalTrailingButton(
                                checked = showMenu,
                                onCheckedChange = { showMenu = it },
                                enabled = !isFollowing,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.follow_private)) },
                            onClick = {
                                showMenu = false
                                followAction("private")
                            }
                        )
                    }
                }
            }
        }
    }
}
