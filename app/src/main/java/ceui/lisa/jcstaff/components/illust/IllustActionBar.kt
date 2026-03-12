package ceui.lisa.jcstaff.components.illust

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.components.BookmarkTagsViewModel
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.downloadToGallery
import ceui.lisa.jcstaff.core.saveFromCacheToGallery
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.ugoira.UgoiraViewModel
import ceui.lisa.jcstaff.components.animations.AnimatedCounter
import ceui.lisa.jcstaff.utils.formatCount
import kotlinx.coroutines.launch

/**
 * 作品操作按钮栏组件
 * 包含收藏、下载和浏览数显示
 */
@Composable
fun IllustActionBar(
    illust: Illust,
    isBookmarked: Boolean,
    downloadUrl: String,
    onBookmarkStateChanged: (Boolean, Illust) -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: Long = 0
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isBookmarking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 收藏按钮 (Split Button 样式)
        var showBookmarkMenu by remember { mutableStateOf(false) }

        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        Box {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.TonalLeadingButton(
                        onClick = {
                            coroutineScope.launch {
                                isBookmarking = true
                                try {
                                    if (isBookmarked) {
                                        PixivClient.pixivApi.deleteBookmark(illust.id)
                                    } else {
                                        PixivClient.pixivApi.addBookmark(illust.id, "public")
                                    }
                                    val newBookmarkState = !isBookmarked
                                    val updatedIllust =
                                        illust.copy(is_bookmarked = newBookmarkState)
                                    ObjectStore.put(updatedIllust)
                                    onBookmarkStateChanged(newBookmarkState, updatedIllust)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isBookmarking = false
                                }
                            }
                        },
                        enabled = !isBookmarking
                    ) {
                        if (isBookmarking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        AnimatedCounter(
                            count = formatCount(illust.total_bookmarks ?: 0),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TonalTrailingButton(
                        checked = showBookmarkMenu,
                        onCheckedChange = { showBookmarkMenu = it },
                        enabled = !isBookmarking
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.bookmark_options),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // 收藏选项菜单
            DropdownMenu(
                expanded = showBookmarkMenu,
                onDismissRequest = { showBookmarkMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bookmark_public)) },
                    onClick = {
                        showBookmarkMenu = false
                        coroutineScope.launch {
                            isBookmarking = true
                            try {
                                PixivClient.pixivApi.addBookmark(illust.id, "public")
                                val updatedIllust = illust.copy(is_bookmarked = true)
                                ObjectStore.put(updatedIllust)
                                onBookmarkStateChanged(true, updatedIllust)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isBookmarking = false
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bookmark_private)) },
                    onClick = {
                        showBookmarkMenu = false
                        coroutineScope.launch {
                            isBookmarking = true
                            try {
                                PixivClient.pixivApi.addBookmark(illust.id, "private")
                                val updatedIllust = illust.copy(is_bookmarked = true)
                                ObjectStore.put(updatedIllust)
                                onBookmarkStateChanged(true, updatedIllust)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isBookmarking = false
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bookmark_with_tags)) },
                    onClick = {
                        showBookmarkMenu = false
                        showTagDialog = true
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        // 下载按钮 (Ugoira 保存为 GIF, 其他保存原图)
        val isUgoira = illust.isGif()
        val ugoiraViewModel: UgoiraViewModel? = if (isUgoira) {
            viewModel(key = "ugoira_${illust.id}")
        } else null

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = !isDownloading) {
                    coroutineScope.launch {
                        isDownloading = true
                        val result = if (isUgoira && ugoiraViewModel != null) {
                            // Ugoira: 保存 GIF
                            ugoiraViewModel.saveToGallery(context)
                        } else {
                            // 普通图片: 保存原图
                            val fileName = "pixiv_${illust.id}_${System.currentTimeMillis()}"
                            val cachedFilePath = LoadTaskManager.getCachedFilePath(downloadUrl)
                            if (cachedFilePath != null) {
                                saveFromCacheToGallery(
                                    context = context,
                                    cachedFilePath = cachedFilePath,
                                    fileName = fileName
                                )
                            } else {
                                downloadToGallery(
                                    context = context,
                                    imageUrl = downloadUrl,
                                    fileName = fileName
                                )
                            }
                        }
                        isDownloading = false
                        if (result.isSuccess) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.saved_to_gallery),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.save_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = if (isUgoira) stringResource(R.string.save_gif) else stringResource(R.string.download),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 浏览数
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            AnimatedCounter(
                count = formatCount(illust.total_view ?: 0),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    // 按标签收藏对话框
    if (showTagDialog) {
        AddBookmarkDialog(
            userId = currentUserId,
            onDismiss = { showTagDialog = false },
            onConfirm = { selectedTags, restrict ->
                showTagDialog = false
                coroutineScope.launch {
                    isBookmarking = true
                    try {
                        if (selectedTags.isNotEmpty()) {
                            PixivClient.pixivApi.addBookmarkWithTags(
                                illustId = illust.id,
                                restrict = restrict,
                                tags = selectedTags
                            )
                        } else {
                            PixivClient.pixivApi.addBookmark(
                                illustId = illust.id,
                                restrict = restrict
                            )
                        }
                        val updatedIllust = illust.copy(is_bookmarked = true)
                        ObjectStore.put(updatedIllust)
                        onBookmarkStateChanged(true, updatedIllust)
                        Toast.makeText(
                            context,
                            context.getString(R.string.bookmark_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            context.getString(R.string.bookmark_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isBookmarking = false
                    }
                }
            }
        )
    }
}

/**
 * 添加收藏 BottomSheet（支持选择标签）
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddBookmarkDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, String) -> Unit
) {
    val viewModel: BookmarkTagsViewModel = viewModel(
        key = "add_bookmark_tags_$userId",
        factory = BookmarkTagsViewModel.factory(userId)
    )
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.75f).dp

    val selectedTags = remember { mutableStateListOf<String>() }
    var newTagText by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    // 每次打开 dialog 时刷新标签列表
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // 检测滚动到底部，加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val maxScroll = scrollState.maxValue
            val currentScroll = scrollState.value
            maxScroll > 0 && currentScroll >= maxScroll - 100
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && state.canLoadMore && !state.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.bookmark_with_tags),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 可滚动内容
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                // 公开/私密选择
                Text(
                    text = stringResource(R.string.bookmark_visibility),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !isPrivate,
                        onClick = { isPrivate = false },
                        label = { Text(stringResource(R.string.bookmark_public)) },
                        leadingIcon = if (!isPrivate) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = isPrivate,
                        onClick = { isPrivate = true },
                        label = { Text(stringResource(R.string.bookmark_private)) },
                        leadingIcon = if (isPrivate) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 新建标签输入
                Text(
                    text = stringResource(R.string.add_new_tag),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        placeholder = { Text(stringResource(R.string.enter_tag_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val tag = newTagText.trim()
                            if (tag.isNotEmpty() && !selectedTags.contains(tag)) {
                                selectedTags.add(tag)
                                newTagText = ""
                            }
                        },
                        enabled = newTagText.trim().isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }

                // 已选择的标签
                if (selectedTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.selected_tags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedTags.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedTags.remove(tag) },
                                label = { Text(tag) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                // 已有的收藏标签
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.existing_tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.isLoading && state.isEmpty) {
                    LoadingIndicator()
                } else if (state.isEmpty) {
                    Text(
                        text = stringResource(R.string.no_bookmark_tags),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.items.forEach { bookmarkTag ->
                            val tagName = bookmarkTag.name ?: return@forEach
                            val isSelected = selectedTags.contains(tagName)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedTags.remove(tagName)
                                    } else {
                                        selectedTags.add(tagName)
                                    }
                                },
                                label = {
                                    Text("$tagName (${bookmarkTag.count})")
                                }
                            )
                        }
                    }

                    // 加载更多指示器
                    if (state.isLoadingMore) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // 底部按钮
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onConfirm(selectedTags.toList(), if (isPrivate) "private" else "public")
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
