package ceui.lisa.jcstaff.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * IllustGrid 的简化版本，直接接收 PagedState
 * 点击事件默认导航到 IllustDetail
 */
@Composable
fun IllustGrid(
    state: PagedState<Illust>,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    columns: Int = 2,
    gridState: LazyStaggeredGridState? = null,
    contentPadding: PaddingValues? = null,
    headerContent: (LazyStaggeredGridScope.() -> Unit)? = null
) {
    val navViewModel = LocalNavigationViewModel.current
    IllustGrid(
        illusts = state.items,
        onIllustClick = { illust ->
            navViewModel.navigate(NavRoute.IllustDetail(
                illustId = illust.id,
                title = illust.title ?: "",
                previewUrl = illust.previewUrl(),
                aspectRatio = illust.aspectRatio()
            ))
        },
        modifier = modifier,
        isLoading = state.isLoading,
        isLoadingMore = state.isLoadingMore,
        canLoadMore = state.canLoadMore,
        error = state.error,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        columns = columns,
        gridState = gridState,
        contentPadding = contentPadding,
        headerContent = headerContent
    )
}

/**
 * 通用的 Illust 网格列表组件
 *
 * 特性：
 * - 支持下拉刷新
 * - 支持无限滚动加载更多
 * - 支持选择模式
 * - 自动应用设置（间距、圆角、信息显示）
 * - 持久化滚动位置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IllustGrid(
    illusts: List<Illust>,
    onIllustClick: (Illust) -> Unit,
    modifier: Modifier = Modifier,
    // 加载状态
    isLoading: Boolean = false,
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = false,
    error: String? = null,
    // 回调
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    // Grid 配置
    columns: Int = 2,
    gridState: LazyStaggeredGridState? = null,
    contentPadding: PaddingValues? = null,
    // 自定义头部内容
    headerContent: (LazyStaggeredGridScope.() -> Unit)? = null
) {
    // 选择管理器
    val selectionManager = LocalSelectionManager.current

    // 设置
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    val density = LocalDensity.current
    val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
    val defaultContentPadding = if (gridSpacingEnabled) PaddingValues(8.dp) else PaddingValues(0.dp)
    val finalContentPadding = contentPadding ?: defaultContentPadding

    // Grid state: 优先使用外部传入的 state，否则使用标准 rememberLazyStaggeredGridState
    // rememberLazyStaggeredGridState 内部使用 rememberSaveable，配合 SaveableStateProvider 自动保持滚动位置
    val actualGridState = gridState ?: rememberLazyStaggeredGridState()

    // 检测是否滚动到底部，触发加载更多
    if (onLoadMore != null) {
        LaunchedEffect(actualGridState, canLoadMore) {
            snapshotFlow {
                val layoutInfo = actualGridState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleItem >= totalItems - 4
            }
                .distinctUntilChanged()
                .filter { it && canLoadMore }
                .collect {
                    onLoadMore()
                }
        }
    }

    val content: @Composable () -> Unit = {
        when {
            error != null && illusts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (onRefresh != null) {
                            Text(
                                text = "下拉刷新重试",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            illusts.isEmpty() && isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    state = actualGridState,
                    contentPadding = finalContentPadding,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalItemSpacing = spacing,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 自定义头部内容
                    headerContent?.invoke(this)

                    // Illust 列表
                    items(illusts, key = { it.id }) { illust ->
                        IllustCard(
                            illust = illust,
                            onClick = { onIllustClick(illust) },
                            showIllustInfo = showIllustInfo,
                            cornerRadius = illustCornerRadius
                        )
                    }

                    // 加载更多指示器
                    if (isLoadingMore) {
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

    if (onRefresh != null) {
        var userPulled by remember { mutableStateOf(false) }

        // 当 isLoading 结束时，重置 userPulled
        LaunchedEffect(isLoading) {
            if (!isLoading) userPulled = false
        }

        PullToRefreshBox(
            isRefreshing = userPulled && isLoading,
            onRefresh = {
                if (!isLoading) {
                    userPulled = true
                    onRefresh()
                }
            },
            modifier = modifier.fillMaxSize()
        ) {
            content()
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
    }
}
