package ceui.lisa.jcstaff.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.cache.DownloadStatus
import ceui.lisa.jcstaff.cache.DownloadTaskEntity
import ceui.lisa.jcstaff.components.ActionMenu
import ceui.lisa.jcstaff.components.ActionMenuItem
import ceui.lisa.jcstaff.history.DownloadHistoryViewModel
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import coil.compose.AsyncImage
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    viewModel: DownloadHistoryViewModel = viewModel()
) {
    val navViewModel = LocalNavigationViewModel.current
    val state by viewModel.state.collectAsState()

    // 使用 derivedStateOf 缓存计算结果，减少不必要的重组
    val failedCount by remember { derivedStateOf { state.failedCount } }
    val completedCount by remember { derivedStateOf { state.completedCount } }
    val tasks by remember { derivedStateOf { state.tasks } }
    val isEmpty by remember { derivedStateOf { state.isEmpty } }

    var showClearDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var menuTask by remember { mutableStateOf<DownloadTaskEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.download_history))
                        if (state.isProcessing) {
                            Text(
                                text = stringResource(R.string.downloading_status, state.downloadingCount, state.pendingCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navViewModel.goBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (failedCount > 0) {
                        IconButton(onClick = { viewModel.retryAllFailed() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.retry_all_failed)
                            )
                        }
                    }
                    if (tasks.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = stringResource(R.string.more)
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                if (completedCount > 0) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.clear_completed)) },
                                        onClick = {
                                            viewModel.clearCompleted()
                                            showMoreMenu = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.clear_all_tasks)) },
                                    onClick = {
                                        showMoreMenu = false
                                        showClearDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_download_history),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.illustId }) { task ->
                    DownloadTaskCard(
                        task = task,
                        isCurrentDownload = state.currentDownloadId == task.illustId,
                        onTaskClick = {
                            val gson = Gson()
                            val illust = gson.fromJson(task.illustJson, Illust::class.java)
                            if (illust != null) {
                                navViewModel.navigate(
                                    NavRoute.IllustDetail(
                                        illustId = illust.id,
                                        title = illust.title ?: "",
                                        previewUrl = illust.previewUrl(),
                                        aspectRatio = illust.aspectRatio()
                                    )
                                )
                            }
                        },
                        onTaskLongClick = { menuTask = task },
                        onRetryClick = { viewModel.retryTask(task.illustId) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_download_history_title)) },
            text = { Text(stringResource(R.string.clear_download_history_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val currentMenuTask = menuTask
    if (currentMenuTask != null) {
        val task = currentMenuTask
        val menuItems = mutableListOf<ActionMenuItem>()

        if (task.isFailed) {
            menuItems.add(
                ActionMenuItem(
                    icon = Icons.Default.Refresh,
                    label = stringResource(R.string.retry),
                    onClick = {
                        viewModel.retryTask(task.illustId)
                        menuTask = null
                    }
                )
            )
        }

        menuItems.add(
            ActionMenuItem(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.delete),
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    viewModel.deleteTask(task.illustId)
                    menuTask = null
                }
            )
        )

        ActionMenu(
            onDismiss = { menuTask = null },
            items = menuItems
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadTaskCard(
    task: DownloadTaskEntity,
    isCurrentDownload: Boolean,
    onTaskClick: () -> Unit,
    onTaskLongClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = onTaskClick,
                onLongClick = onTaskLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            AsyncImage(
                model = task.previewUrl,
                contentDescription = task.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = task.userName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 状态和进度
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (task.status) {
                        DownloadStatus.PENDING -> {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_pending),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DownloadStatus.DOWNLOADING -> {
                            if (task.totalPages > 1) {
                                Text(
                                    text = "${task.downloadedPages}/${task.totalPages}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { task.progress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp),
                                    strokeCap = StrokeCap.Round
                                )
                            } else {
                                Text(
                                    text = "${task.currentPageProgress}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { task.currentPageProgress / 100f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }

                        DownloadStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_completed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (task.totalPages > 1) {
                                Text(
                                    text = " (${task.totalPages}P)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DownloadStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_failed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 操作按钮
            if (task.isFailed) {
                IconButton(
                    onClick = onRetryClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.retry),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
