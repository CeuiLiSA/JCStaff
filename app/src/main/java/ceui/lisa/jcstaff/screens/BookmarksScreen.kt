package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.BookmarkTagDialog
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.IllustListViewModel
import ceui.lisa.jcstaff.auth.AccountRegistry
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.network.PixivClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    userId: Long
) {
    val navViewModel = LocalNavigationViewModel.current
    val selectionManager = LocalSelectionManager.current
    val currentUserId by AccountRegistry.activeUserId.collectAsState(initial = null)
    val isMyBookmarks = currentUserId == userId

    // 收藏标签相关状态（仅自己的收藏页可用）
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }

    // 根据选中的标签创建对应的 ViewModel
    val viewModel: IllustListViewModel = viewModel(
        key = "bookmarks_${userId}_${selectedTag ?: "all"}",
        factory = IllustListViewModel.factory(
            loadFirstPage = { PixivClient.pixivApi.getUserBookmarks(userId, tag = selectedTag) }
        )
    )
    val state by viewModel.state.collectAsState()

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                if (isMyBookmarks) R.string.my_bookmarks else R.string.bookmarks
                            )
                        )
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
                        if (isMyBookmarks) {
                            IconButton(onClick = { showTagDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter_by_tag)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            IllustGrid(
                illusts = state.illusts,
                onIllustClick = { illust ->
                    navViewModel.navigate(NavRoute.IllustDetail(
                        illustId = illust.id,
                        title = illust.title ?: "",
                        previewUrl = illust.previewUrl(),
                        aspectRatio = illust.aspectRatio()
                    ))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                isLoading = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                error = state.error,
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
                headerContent = if (isMyBookmarks) {
                    {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                FilterChip(
                                    selected = true,
                                    onClick = { showTagDialog = true },
                                    label = {
                                        Text(selectedTag ?: stringResource(R.string.all_bookmarks))
                                    }
                                )
                            }
                        }
                    }
                } else null
            )
        }

        // Selection top bar overlay
        SelectionTopBar(allIllusts = state.illusts)
    }

    // 标签选择对话框（仅自己的收藏页）
    if (isMyBookmarks && showTagDialog) {
        BookmarkTagDialog(
            userId = userId,
            selectedTag = selectedTag,
            onDismiss = { showTagDialog = false },
            onTagSelected = { tag ->
                selectedTag = tag
                showTagDialog = false
            }
        )
    }
}
