package ceui.lisa.jcstaff.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.BookmarkTag
import ceui.lisa.jcstaff.network.PixivClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 收藏标签列表状态
 */
data class BookmarkTagsState(
    val tags: List<BookmarkTag> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null
)

/**
 * 收藏标签 ViewModel，支持分页加载
 */
class BookmarkTagsViewModel(
    private val userId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(BookmarkTagsState())
    val state: StateFlow<BookmarkTagsState> = _state.asStateFlow()

    private var nextUrl: String? = null
    private val gson = Gson()

    fun load() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = PixivClient.pixivApi.getUserBookmarkTags(userId)
                nextUrl = response.next_url
                _state.update {
                    it.copy(
                        tags = response.bookmark_tags,
                        isLoading = false,
                        canLoadMore = response.next_url != null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun loadMore() {
        val url = nextUrl ?: return
        if (_state.value.isLoadingMore) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val responseBody = PixivClient.pixivApi.getNextPage(url)
                val response = gson.fromJson(
                    responseBody.string(),
                    ceui.lisa.jcstaff.network.BookmarkTagsResponse::class.java
                )
                nextUrl = response.next_url
                _state.update {
                    it.copy(
                        tags = it.tags + response.bookmark_tags,
                        isLoadingMore = false,
                        canLoadMore = response.next_url != null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoadingMore = false, error = e.message)
                }
            }
        }
    }

    fun refresh() {
        nextUrl = null
        _state.update { it.copy(tags = emptyList()) }
        load()
    }

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

            if (state.isLoading && state.tags.isEmpty()) {
                // 首次加载
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
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
                        state.tags.forEach { bookmarkTag ->
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
                    if (state.tags.isEmpty() && !state.isLoading) {
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
