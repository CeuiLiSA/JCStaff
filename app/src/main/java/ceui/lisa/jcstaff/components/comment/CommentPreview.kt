package ceui.lisa.jcstaff.components.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.comment.CommentViewModel
import ceui.lisa.jcstaff.network.Comment
import ceui.lisa.jcstaff.network.PixivClient

@Composable
fun CommentPreviewSection(
    objectId: Long,
    objectType: String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var comments by remember { mutableStateOf<List<Comment>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(objectId, objectType) {
        val cached = CommentViewModel.commentsCache[CommentViewModel.cacheKey(objectType, objectId)]
        if (cached != null) {
            comments = cached
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val response = if (objectType == "illust") {
                PixivClient.pixivApi.getIllustComments(objectId)
            } else {
                PixivClient.pixivApi.getNovelComments(objectId)
            }
            CommentViewModel.commentsCache[CommentViewModel.cacheKey(objectType, objectId)] = response.comments
            comments = response.comments
        } catch (_: Exception) {
            comments = emptyList()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(top = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.comments),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onViewAll) {
                Text(
                    text = stringResource(R.string.view_all_comments),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            comments.isNullOrEmpty() -> {
                Text(
                    text = stringResource(R.string.no_comments),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            else -> {
                comments!!.take(3).forEach { comment ->
                    CompactCommentCard(
                        comment = comment,
                        onClick = onViewAll
                    )
                }
            }
        }
    }
}
