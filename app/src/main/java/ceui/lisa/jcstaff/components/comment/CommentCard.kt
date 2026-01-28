package ceui.lisa.jcstaff.components.comment

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.Comment
import ceui.lisa.jcstaff.utils.CommentPart
import ceui.lisa.jcstaff.utils.formatRelativeDate
import ceui.lisa.jcstaff.utils.parseCommentWithEmojis
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun CommentCard(
    comment: Comment,
    currentUserId: Long,
    isChild: Boolean = false,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onUserClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarSize = if (isChild) 32.dp else 40.dp
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isChild) 48.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(comment.user.profile_image_urls?.findAvatarUrl())
                .crossfade(true)
                .build(),
            contentDescription = comment.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable { onUserClick(comment.user.id) }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = comment.user.name ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onUserClick(comment.user.id) }
                )
                comment.date?.let { dateStr ->
                    formatRelativeDate(dateStr)?.let { formatted ->
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (comment.stamp != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(comment.stamp.stamp_url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.height(120.dp)
                )
            } else if (comment.comment != null) {
                CommentText(text = comment.comment)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onReply) {
                    Text(
                        text = stringResource(R.string.add_comment),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (comment.user.id == currentUserId) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.delete_comment),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentText(text: String) {
    val parts = parseCommentWithEmojis(text)
    val hasEmojis = parts.any { it is CommentPart.EmojiPart }

    if (!hasEmojis) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        return
    }

    val context = LocalContext.current
    var emojiIndex = 0
    val annotatedString = buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is CommentPart.TextPart -> append(part.text)
                is CommentPart.EmojiPart -> {
                    appendInlineContent("emoji_$emojiIndex")
                    emojiIndex++
                }
            }
        }
    }

    val inlineContent = mutableMapOf<String, InlineTextContent>()
    var idx = 0
    parts.forEach { part ->
        if (part is CommentPart.EmojiPart) {
            val emoji = part.emoji
            inlineContent["emoji_$idx"] = InlineTextContent(
                placeholder = Placeholder(
                    width = 24.sp,
                    height = 24.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(emoji.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = emoji.name,
                    modifier = Modifier.size(24.dp)
                )
            }
            idx++
        }
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun CompactCommentCard(
    comment: Comment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(comment.user.profile_image_urls?.findAvatarUrl())
                .crossfade(true)
                .build(),
            contentDescription = comment.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = comment.user.name ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                comment.date?.let { dateStr ->
                    formatRelativeDate(dateStr)?.let { formatted ->
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (comment.stamp != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(comment.stamp.stamp_url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.height(48.dp)
                )
            } else if (comment.comment != null) {
                Text(
                    text = comment.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
