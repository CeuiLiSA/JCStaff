package ceui.lisa.jcstaff.components.comment

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.auth.AccountRegistry
import ceui.lisa.jcstaff.comment.CommentViewModel
import ceui.lisa.jcstaff.components.CircleAvatar
import ceui.lisa.jcstaff.components.EmptyState
import ceui.lisa.jcstaff.components.ErrorRetryState
import ceui.lisa.jcstaff.components.LoadingIndicator
import kotlinx.coroutines.launch

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
    val interaction by viewModel.interactionState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val allAccounts by AccountRegistry.allAccounts.collectAsState(initial = emptyList())
    val activeUserId by AccountRegistry.activeUserId.collectAsState(initial = null)
    val myAvatarUrl = remember(allAccounts, activeUserId) {
        allAccounts.find { it.userId == activeUserId }?.avatarUrl
    }
    var inputText by remember { mutableStateOf("") }

    val sendComment = {
        if (inputText.isNotBlank() && !interaction.isPosting) {
            val text = inputText
            inputText = ""
            keyboardController?.hide()
            scope.launch {
                try {
                    viewModel.postComment(text)
                    Toast.makeText(
                        context,
                        context.getString(R.string.comment_posted),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.comment_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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

        // 评论输入框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(
                imageUrl = myAvatarUrl,
                size = 40.dp,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.comment_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendComment() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { sendComment() },
                enabled = inputText.isNotBlank() && !interaction.isPosting
            ) {
                if (interaction.isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
