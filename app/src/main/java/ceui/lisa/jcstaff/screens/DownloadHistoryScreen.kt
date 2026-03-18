package ceui.lisa.jcstaff.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.cache.DownloadStatus
import ceui.lisa.jcstaff.cache.DownloadTaskEntity
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

    // Section grouping — derived to minimize recomposition
    val downloadingTask by remember { derivedStateOf { state.tasks.firstOrNull { it.isDownloading } } }
    val pendingTasks by remember {
        derivedStateOf { state.tasks.filter { it.isPending }.sortedBy { it.createdAt } }
    }
    val failedTasks by remember { derivedStateOf { state.tasks.filter { it.isFailed } } }
    val completedTasks by remember { derivedStateOf { state.tasks.filter { it.isCompleted } } }

    val hasActive by remember { derivedStateOf { downloadingTask != null || pendingTasks.isNotEmpty() } }
    val hasFailed by remember { derivedStateOf { failedTasks.isNotEmpty() } }
    val hasCompleted by remember { derivedStateOf { completedTasks.isNotEmpty() } }
    val isEmpty by remember {
        derivedStateOf { !hasActive && !hasFailed && !hasCompleted && !state.isLoading }
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = stringResource(R.string.download_queue),
                            style = MaterialTheme.typography.titleMedium
                        )
                        val summaryParts = buildList {
                            if (state.downloadingCount > 0) add("下载中 ${state.downloadingCount}")
                            if (state.pendingCount > 0) add("等待 ${state.pendingCount}")
                            if (state.failedCount > 0) add("失败 ${state.failedCount}")
                        }
                        if (summaryParts.isNotEmpty()) {
                            Text(
                                text = summaryParts.joinToString(" · "),
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
                    if (state.failedCount > 0) {
                        IconButton(onClick = { viewModel.retryAllFailed() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.retry_all_failed)
                            )
                        }
                    }
                    if (state.tasks.isNotEmpty()) {
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
                                if (state.completedCount > 0) {
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = stringResource(R.string.no_download_history),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── 正在下载 section ──────────────────────────────────
                if (hasActive) {
                    item(key = "header_active") {
                        DownloadSectionHeader(
                            title = stringResource(R.string.section_downloading),
                            count = (if (downloadingTask != null) 1 else 0) + pendingTasks.size
                        )
                    }

                    downloadingTask?.let { task ->
                        item(key = "downloading_${task.illustId}") {
                            DownloadQueueRow(
                                task = task,
                                queuePosition = null,
                                onActionClick = { viewModel.stopTask(task.illustId) },
                                onRowClick = { navigateToIllust(navViewModel, task) },
                                modifier = Modifier.animateItem()
                            )
                            RowDivider()
                        }
                    }

                    items(
                        items = pendingTasks,
                        key = { "pending_${it.illustId}" }
                    ) { task ->
                        val position = pendingTasks.indexOf(task) + 1
                        DownloadQueueRow(
                            task = task,
                            queuePosition = position,
                            onActionClick = { viewModel.deleteTask(task.illustId) },
                            onRowClick = { navigateToIllust(navViewModel, task) },
                            onResumeQueue = if (!state.isProcessing && position == 1) ({ viewModel.resumeQueue() }) else null,
                            modifier = Modifier.animateItem()
                        )
                        RowDivider()
                    }
                }

                // ── 下载失败 section ──────────────────────────────────
                if (hasFailed) {
                    item(key = "header_failed") {
                        DownloadSectionHeader(
                            title = stringResource(R.string.section_failed),
                            count = failedTasks.size
                        )
                    }

                    items(
                        items = failedTasks,
                        key = { "failed_${it.illustId}" }
                    ) { task ->
                        DownloadQueueRow(
                            task = task,
                            queuePosition = null,
                            onActionClick = { viewModel.retryTask(task.illustId) },
                            onRowClick = { viewModel.retryTask(task.illustId) },
                            modifier = Modifier.animateItem()
                        )
                        RowDivider()
                    }
                }

                // ── 已完成 section ────────────────────────────────────
                if (hasCompleted) {
                    item(key = "header_completed") {
                        DownloadSectionHeader(
                            title = stringResource(R.string.section_completed),
                            count = completedTasks.size
                        )
                    }

                    items(
                        items = completedTasks,
                        key = { "completed_${it.illustId}" }
                    ) { task ->
                        DownloadQueueRow(
                            task = task,
                            queuePosition = null,
                            onActionClick = { viewModel.deleteTask(task.illustId) },
                            onRowClick = { navigateToIllust(navViewModel, task) },
                            modifier = Modifier.animateItem()
                        )
                        RowDivider()
                    }
                }
            }
        }
    }

    // Confirm clear all dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_download_history_title)) },
            text = { Text(stringResource(R.string.clear_download_history_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) {
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Row: pending / downloading / failed
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadQueueRow(
    task: DownloadTaskEntity,
    queuePosition: Int?,
    onActionClick: () -> Unit,
    onRowClick: () -> Unit,
    onResumeQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail — prefer square_medium (smallest), fallback to previewUrl for old records
        AsyncImage(
            model = task.squareUrl.ifEmpty { task.previewUrl },
            contentDescription = task.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info column
        Column(modifier = Modifier.weight(1f).animateContentSize()) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.userName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (task.totalPages > 1) {
                    Text(
                        text = "${task.totalPages}P",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Queue position — only for pending items
            if (task.isPending && queuePosition != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "# $queuePosition",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Progress bar — only visible while downloading
            if (task.isDownloading) {
                Spacer(modifier = Modifier.height(6.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = task.progress,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "progress"
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        strokeCap = StrokeCap.Round,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    val progressText = if (task.totalPages > 1) {
                        "${task.downloadedPages}/${task.totalPages} · ${(task.progress * 100).toInt()}%"
                    } else {
                        "${task.currentPageProgress}%"
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action widget
        DownloadActionWidget(
            task = task,
            queuePosition = queuePosition,
            onActionClick = onActionClick,
            onResumeQueue = onResumeQueue
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action widget — the right-side control per item state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DownloadActionWidget(
    task: DownloadTaskEntity,
    queuePosition: Int?,
    onActionClick: () -> Unit,
    onResumeQueue: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        when (task.status) {

            // ● Downloading — circular progress ring with stop button
            DownloadStatus.DOWNLOADING -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = task.progress,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "ring_progress"
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.5.dp,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = stringResource(R.string.pause_download),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Pending — play button to resume queue (if queue is stopped), else "等待中"
            DownloadStatus.PENDING -> {
                if (onResumeQueue != null) {
                    IconButton(
                        onClick = onResumeQueue,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.resume_queue),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.status_pending),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ✕ Failed — error icon, tap to retry
            DownloadStatus.FAILED -> {
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = stringResource(R.string.retry),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ✓ Completed — handled in CompletedQueueRow
            DownloadStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.status_completed),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 84.dp, end = 0.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun navigateToIllust(navViewModel: ceui.lisa.jcstaff.navigation.NavigationViewModel, task: DownloadTaskEntity) {
    val illust = runCatching {
        Gson().fromJson(task.illustJson, Illust::class.java)
    }.getOrNull() ?: return

    navViewModel.navigate(
        NavRoute.IllustDetail(
            illustId = illust.id,
            title = illust.title ?: "",
            previewUrl = illust.previewUrl(),
            aspectRatio = illust.aspectRatio()
        )
    )
}
