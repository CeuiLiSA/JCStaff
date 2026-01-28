package ceui.lisa.jcstaff.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.comment.CommentViewModel
import ceui.lisa.jcstaff.components.comment.CommentCard
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.utils.getAllEmojis
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    objectId: Long,
    objectType: String,
    currentUserId: Long,
    commentViewModel: CommentViewModel = viewModel(key = "comments_${objectType}_$objectId")
) {
    val navViewModel = LocalNavigationViewModel.current
    val state by commentViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputText by rememberSaveable { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(objectId, objectType) {
        commentViewModel.loadComments(objectId, objectType)
    }

    // Auto load more when reaching bottom
    LaunchedEffect(listState, state.nextUrl) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it && state.nextUrl != null && !state.isLoading }
            .collect { commentViewModel.loadMore() }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { commentId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_comment)) },
            text = { Text(stringResource(R.string.delete_comment_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = null
                        scope.launch {
                            try {
                                commentViewModel.deleteComment(commentId)
                                Toast.makeText(context, context.getString(R.string.comment_deleted), Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, context.getString(R.string.comment_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.delete_comment),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.comments.isNotEmpty()) {
                            "${stringResource(R.string.comments)} (${state.comments.size})"
                        } else {
                            stringResource(R.string.comments)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navViewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                // Reply target indicator
                state.replyTarget?.let { target ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.reply_to, target.user.name ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { commentViewModel.setReplyTarget(null) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Emoji picker
                AnimatedVisibility(visible = showEmojiPicker) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(getAllEmojis()) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            inputText += "(${emoji.name})"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(emoji.url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = emoji.name,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Input bar
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEmotions,
                                contentDescription = null,
                                tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                val hint = if (state.replyTarget != null) {
                                    stringResource(R.string.reply_hint, state.replyTarget?.user?.name ?: "")
                                } else {
                                    stringResource(R.string.comment_hint)
                                }
                                Text(hint, style = MaterialTheme.typography.bodyMedium)
                            },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !state.isPosting) {
                                    val text = inputText
                                    val parentId = state.replyTarget?.id
                                    inputText = ""
                                    showEmojiPicker = false
                                    scope.launch {
                                        try {
                                            commentViewModel.postComment(text, parentId)
                                            Toast.makeText(context, context.getString(R.string.comment_posted), Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {
                                            Toast.makeText(context, context.getString(R.string.comment_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = inputText.isNotBlank() && !state.isPosting
                        ) {
                            if (state.isPosting) {
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
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.comments.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.comments.isEmpty() && !state.isLoading) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_comments),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(state.comments, key = { "comment_${it.id}" }) { comment ->
                CommentCard(
                    comment = comment,
                    currentUserId = currentUserId,
                    isChild = false,
                    onReply = { commentViewModel.setReplyTarget(comment) },
                    onDelete = { showDeleteDialog = comment.id },
                    onUserClick = { userId ->
                        navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                    }
                )

                // View replies button
                if (comment.has_replies) {
                    val isExpanded = state.expandedReplies.containsKey(comment.id)
                    val isLoadingReplies = state.loadingReplies.contains(comment.id)

                    Row(
                        modifier = Modifier.padding(start = 68.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { commentViewModel.expandReplies(comment.id) }
                        ) {
                            if (isLoadingReplies) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = stringResource(R.string.view_replies),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Expanded replies
                val replies = state.expandedReplies[comment.id]
                if (replies != null) {
                    replies.forEach { reply ->
                        CommentCard(
                            comment = reply,
                            currentUserId = currentUserId,
                            isChild = true,
                            onReply = { commentViewModel.setReplyTarget(reply) },
                            onDelete = { showDeleteDialog = reply.id },
                            onUserClick = { userId ->
                                navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                            },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                RoundedCornerShape(8.dp)
                            )
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Loading more indicator
            if (state.isLoading && state.comments.isNotEmpty()) {
                item(key = "loading_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
