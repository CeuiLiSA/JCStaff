package ceui.lisa.jcstaff.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheBrowserScreen(
    initialPath: String? = null,
    viewModel: CacheBrowserViewModel = viewModel()
) {
    val context = LocalContext.current
    val navViewModel = LocalNavigationViewModel.current
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<FileItem?>(null) }
    var showCleanDialog by remember { mutableStateOf(false) }

    // 处理系统返回键：如果不在根目录，返回上级目录而非关闭页面
    BackHandler(enabled = !state.isRoot) {
        viewModel.navigateUp()
    }

    // 初始化
    LaunchedEffect(Unit) {
        viewModel.initialize(context, initialPath)
    }

    val currentDir = File(state.currentPath)
    val displayPath = if (state.isRoot) {
        stringResource(R.string.cache_browser_root)
    } else {
        currentDir.name
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayPath,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!state.isLoading) {
                            Text(
                                text = formatSize(state.totalSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.navigateUp()) {
                            navViewModel.goBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // 只在根目录显示一键清理按钮
                    if (state.isRoot && state.cleanableCacheSize > 0 && !state.isCleaning) {
                        IconButton(onClick = { showCleanDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = stringResource(R.string.clean_cache)
                            )
                        }
                    }
                    if (state.isCleaning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    // 加载中显示转圈
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.fileItems.isEmpty() -> {
                    // 空目录
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.cache_browser_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    // 文件列表
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.fileItems, key = { it.file.absolutePath }) { item ->
                            FileItemRow(
                                item = item,
                                onClick = {
                                    if (item.isDirectory) {
                                        viewModel.navigateTo(item.file.absolutePath)
                                    }
                                },
                                onDeleteClick = {
                                    showDeleteDialog = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 一键清理确认对话框
    if (showCleanDialog) {
        AlertDialog(
            onDismissRequest = { showCleanDialog = false },
            title = { Text(stringResource(R.string.clean_cache_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.clean_cache_message,
                        formatSize(state.cleanableCacheSize)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cleanImageCache()
                        showCleanDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.clean_cache_confirm),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 删除确认对话框
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.cache_browser_delete_title)) },
            text = {
                Text(
                    stringResource(
                        if (item.isDirectory) R.string.cache_browser_delete_folder_message
                        else R.string.cache_browser_delete_file_message,
                        item.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        showDeleteDialog = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
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
}

@Composable
private fun FileItemRow(
    item: FileItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val isImage = !item.isDirectory && item.name.lowercase().let {
        it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") ||
                it.endsWith(".gif") || it.endsWith(".webp")
    }

    // 文件夹描述
    val folderDescriptionResId = if (item.isDirectory) getFolderDescriptionResId(item.name) else null
    val folderDescription = folderDescriptionResId?.let { stringResource(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标或缩略图
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (item.isDirectory) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isImage) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.file)
                        .crossfade(true)
                        .size(96)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = getFileIcon(item),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (item.isDirectory) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 文件信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isDirectory) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (folderDescription != null) {
                Text(
                    text = folderDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatSize(item.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.isDirectory && item.childCount > 0) {
                    Text(
                        text = "${item.childCount} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatDate(item.lastModified),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 删除按钮
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getFileIcon(item: FileItem): ImageVector {
    return when {
        item.isDirectory -> Icons.Default.Folder
        item.name.lowercase().let {
            it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") ||
                    it.endsWith(".gif") || it.endsWith(".webp")
        } -> Icons.Default.Image

        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFolderDescriptionResId(name: String): Int? {
    // 精确匹配
    val exactMatch = mapOf(
        "files" to R.string.folder_desc_files,
        "cache" to R.string.folder_desc_cache,
        "databases" to R.string.folder_desc_databases,
        "shared_prefs" to R.string.folder_desc_shared_prefs,
        "code_cache" to R.string.folder_desc_code_cache,
        "no_backup" to R.string.folder_desc_no_backup,
        "app_webview" to R.string.folder_desc_webview,
        "ugoira" to R.string.folder_desc_ugoira,
        "datastore" to R.string.folder_desc_datastore,
        "lib" to R.string.folder_desc_lib
    )

    val matched = exactMatch[name]
    if (matched != null) return matched

    // 前缀匹配
    return when {
        name.startsWith("image_load_cache") -> R.string.folder_desc_image_cache
        name.startsWith("jcstaff_db") -> R.string.folder_desc_user_db
        name.startsWith("auth_prefs") -> R.string.folder_desc_auth
        name.startsWith("settings") -> R.string.folder_desc_settings
        name.startsWith("account") -> R.string.folder_desc_account
        name.startsWith("app_") -> R.string.folder_desc_app_data
        else -> null
    }
}
