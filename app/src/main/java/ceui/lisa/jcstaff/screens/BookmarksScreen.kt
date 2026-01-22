package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import ceui.lisa.jcstaff.core.SettingsStore
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.IllustListViewModel
import ceui.lisa.jcstaff.core.IllustLoader
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.PixivClient

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BookmarksScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    userId: Long,
    onBackClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    viewModel: IllustListViewModel = viewModel(key = "bookmarks_$userId")
) {
    val state by viewModel.state.collectAsState()
    val selectionManager = rememberSelectionManager()

    // 检测 shared element transition 是否正在进行
    val isTransitionActive = sharedTransitionScope.isTransitionActive

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    // 绑定加载器
    LaunchedEffect(userId) {
        viewModel.bind(IllustLoader {
            PixivClient.pixivApi.getUserBookmarks(userId)
        })
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("我的收藏") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    state.hasError && state.isEmpty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.error ?: "加载失败",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "下拉刷新重试",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    state.isEmpty && state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        val gridState = rememberPersistentLazyStaggeredGridState("bookmarks_$userId")
                        val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
                        val density = LocalDensity.current
                        val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
                        val contentPadding = if (gridSpacingEnabled) PaddingValues(8.dp) else PaddingValues(0.dp)

                        // 检测是否滚动到底部
                        LaunchedEffect(gridState, state.canLoadMore) {
                            snapshotFlow {
                                val layoutInfo = gridState.layoutInfo
                                val totalItems = layoutInfo.totalItemsCount
                                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                totalItems > 0 && lastVisibleItem >= totalItems - 4
                            }
                                .distinctUntilChanged()
                                .filter { it && state.canLoadMore }
                                .collect {
                                    viewModel.loadMore()
                                }
                        }

                        val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
                        val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = gridState,
                            contentPadding = contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalItemSpacing = spacing,
                            modifier = Modifier.fillMaxSize(),
                            // 在 shared element transition 动画进行时禁用滚动
                            userScrollEnabled = !isTransitionActive
                        ) {
                            items(state.illusts, key = { it.id }) { illust ->
                                IllustCard(
                                    illust = illust,
                                    onClick = { onIllustClick(illust) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    isSelectionMode = selectionManager.isSelectionMode,
                                    isSelected = selectionManager.isSelected(illust.id),
                                    onLongPress = { selectionManager.onLongPress(illust) },
                                    onSelectionToggle = { selectionManager.toggleSelection(illust) },
                                    showIllustInfo = showIllustInfo,
                                    cornerRadius = illustCornerRadius
                                )
                            }

                            // 加载更多指示器
                            if (state.isLoadingMore) {
                                item(span = StaggeredGridItemSpan.FullLine) {
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
            }
        }

        // Selection top bar overlay
        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = state.illusts
        )
    }
}
