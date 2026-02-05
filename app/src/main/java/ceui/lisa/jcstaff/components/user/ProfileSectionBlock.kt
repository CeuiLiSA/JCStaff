package ceui.lisa.jcstaff.components.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.CircleAvatar
import ceui.lisa.jcstaff.components.EmptyState
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.UserPreview
import ceui.lisa.jcstaff.utils.formatNumber
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 大标题分组（如 "创作"、"收藏"）
 */
@Composable
fun ProfileSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * 子区块（如 "插画"、"漫画"、"小说"），带小标题 + 数量 + 查看全部 + 预览内容
 */
@Composable
fun ProfileSubSection(
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))

        // 小标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (count > 0) {
                Text(
                    text = formatNumber(count),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = stringResource(R.string.view_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 横向滚动预览
        content()

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 插画/漫画预览行
 */
@Composable
fun IllustPreviewRow(
    illusts: List<Illust>,
    modifier: Modifier = Modifier
) {
    if (illusts.isEmpty()) {
        EmptyState()
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(illusts.take(10), key = { it.id }) { illust ->
            val imageUrl = illust.image_urls?.square_medium ?: illust.previewUrl()
            val context = LocalContext.current
            val imageRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

/**
 * 小说预览行
 */
@Composable
fun NovelPreviewRow(
    novels: List<Novel>,
    modifier: Modifier = Modifier
) {
    if (novels.isEmpty()) {
        EmptyState()
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(novels.take(10), key = { it.id }) { novel ->
            val imageUrl = novel.image_urls?.medium ?: novel.image_urls?.square_medium
            val context = LocalContext.current
            val imageRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            }
            Column(
                modifier = Modifier.width(110.dp),
                horizontalAlignment = Alignment.Start
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = novel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = novel.title ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 关注用户预览行
 */
@Composable
fun UserPreviewRow(
    users: List<UserPreview>,
    modifier: Modifier = Modifier
) {
    if (users.isEmpty()) {
        EmptyState()
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(users.take(20), key = { it.user?.id ?: 0L }) { userPreview ->
            val avatarUrl = userPreview.user?.profile_image_urls?.findAvatarUrl()
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleAvatar(
                    imageUrl = avatarUrl,
                    size = 60.dp,
                    contentDescription = userPreview.user?.name,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = userPreview.user?.name ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

