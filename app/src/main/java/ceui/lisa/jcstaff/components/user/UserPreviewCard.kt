package ceui.lisa.jcstaff.components.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.CircleAvatar
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.UserPreview
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun UserPreviewCard(
    userPreview: UserPreview,
    onUserClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = userPreview.user ?: return

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // User info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUserClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleAvatar(
                    imageUrl = user.profile_image_urls?.findAvatarUrl(),
                    size = 48.dp,
                    contentDescription = user.name,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!user.account.isNullOrBlank()) {
                        Text(
                            text = "@${user.account}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (userPreview.illusts.isNotEmpty()) {
                        val illustTitles = userPreview.illusts
                            .take(3)
                            .mapNotNull { it.title }
                            .joinToString(" / ")
                        Text(
                            text = illustTitles,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Illusts preview
            if (userPreview.illusts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    userPreview.illusts.take(3).forEach { illust ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(illust.image_urls?.square_medium ?: illust.previewUrl())
                                .crossfade(true)
                                .build(),
                            contentDescription = illust.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onIllustClick(illust) }
                        )
                    }
                }
            }
        }
    }
}
