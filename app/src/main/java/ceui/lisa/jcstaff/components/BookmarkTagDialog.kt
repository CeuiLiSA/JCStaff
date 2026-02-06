package ceui.lisa.jcstaff.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.core.CacheConfig
import ceui.lisa.jcstaff.core.PagedViewModel
import ceui.lisa.jcstaff.network.BookmarkTag
import ceui.lisa.jcstaff.network.BookmarkTagsResponse
import ceui.lisa.jcstaff.network.PixivClient

/**
 * 收藏标签 ViewModel，支持分页加载
 */
class BookmarkTagsViewModel(
    userId: Long
) : PagedViewModel<BookmarkTag, BookmarkTagsResponse>(
    cacheConfig = CacheConfig(
        path = "/v1/user/bookmark-tags/illust",
        queryParams = mapOf("user_id" to userId.toString())
    ),
    responseClass = BookmarkTagsResponse::class.java,
    loadFirstPage = { PixivClient.pixivApi.getUserBookmarkTags(userId) }
) {
    companion object {
        fun factory(userId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BookmarkTagsViewModel(userId) as T
            }
        }
    }
}

/**
 * 收藏标签选择 BottomSheet
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookmarkTagDialog(
    userId: Long,
    selectedTag: String?,
    onDismiss: () -> Unit,
    onTagSelected: (String?) -> Unit
) {
    val viewModel: BookmarkTagsViewModel = viewModel(
        key = "bookmark_tags_$userId",
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

    // 每次打开 dialog 时刷新标签列表
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // 检测滚动到底部，加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val maxScroll = scrollState.maxValue
            val currentScroll = scrollState.value
            // 距离底部 100px 时触发加载
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
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.filter_by_tag),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (state.isLoading && state.isEmpty) {
                // 首次加载
                LoadingIndicator()
            } else {
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // "全部" 选项
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { onTagSelected(null) },
                            label = { Text(stringResource(R.string.all_bookmarks)) },
                            leadingIcon = if (selectedTag == null) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )

                        // 各个标签
                        state.items.forEach { bookmarkTag ->
                            val tagName = bookmarkTag.name ?: return@forEach
                            FilterChip(
                                selected = selectedTag == tagName,
                                onClick = { onTagSelected(tagName) },
                                label = {
                                    Text("$tagName (${bookmarkTag.count})")
                                },
                                leadingIcon = if (selectedTag == tagName) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null
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

                    // 无标签提示
                    if (state.isEmpty && !state.isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_bookmark_tags),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
