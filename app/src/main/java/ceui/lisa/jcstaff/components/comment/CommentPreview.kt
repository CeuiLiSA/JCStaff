package ceui.lisa.jcstaff.components.comment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.comment.CommentViewModel
import ceui.lisa.jcstaff.components.EmptyState
import ceui.lisa.jcstaff.components.ErrorRetryState
import ceui.lisa.jcstaff.components.LoadingIndicator

@Composable
fun CommentPreviewSection(
    objectId: Long,
    objectType: String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CommentViewModel = viewModel(
        key = "comments_${objectType}_$objectId",
        factory = CommentViewModel.factory(objectId, objectType)
    )
    val state by viewModel.pagedState.collectAsState()

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
            state.isLoading && state.isEmpty -> {
                LoadingIndicator()
            }

            state.hasError && state.isEmpty -> {
                ErrorRetryState(
                    error = state.error ?: stringResource(R.string.load_error),
                    onRetry = { viewModel.refresh() },
                    scrollable = false,
                    showPullToRefreshHint = false
                )
            }

            state.isEmpty -> {
                EmptyState(
                    text = stringResource(R.string.no_comments)
                )
            }

            else -> {
                state.items.take(3).forEach { comment ->
                    CompactCommentCard(
                        comment = comment,
                        onClick = onViewAll
                    )
                }
            }
        }
    }
}
