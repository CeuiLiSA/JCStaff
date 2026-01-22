package ceui.lisa.jcstaff.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.SelectionManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import ceui.lisa.jcstaff.network.Illust
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 通用的 Illust 网格列表组件
 *
 * 特性：
 * - 自动处理 shared element transition 动画期间的滚动禁用
 * - 支持下拉刷新
 * - 支持无限滚动加载更多
 * - 支持选择模式
 * - 自动应用设置（间距、圆角、信息显示）
 * - 持久化滚动位置
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun IllustGrid(
    illusts: List<Illust>,
    onIllustClick: (Illust) -> Unit,
    modifier: Modifier = Modifier,
    // Shared transition
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    // 加载状态
    isLoading: Boolean = false,
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = false,
    error: String? = null,
    // 回调
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    // 选择模式
    selectionManager: SelectionManager? = null,
    // Grid 配置
    columns: Int = 2,
    gridStateKey: String? = null,
    gridState: LazyStaggeredGridState? = null,
    contentPadding: PaddingValues? = null,
    // 自定义头部内容
    headerContent: (LazyStaggeredGridScope.() -> Unit)? = null
) {
    // 检测 shared element transition 是否正在进行
    val isTransitionActive = sharedTransitionScope?.isTransitionActive ?: false

    // 设置
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    val density = LocalDensity.current
    val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
    val defaultContentPadding = if (gridSpacingEnabled) PaddingValues(8.dp) else PaddingValues(0.dp)
    val finalContentPadding = contentPadding ?: defaultContentPadding

    // Grid state: 优先使用传入的 state，其次使用 key 创建持久化 state，最后使用默认 state
    val actualGridState = gridState ?: if (gridStateKey != null) {
        rememberPersistentLazyStaggeredGridState(gridStateKey)
    } else {
        rememberLazyStaggeredGridState()
    }

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
                    modifier = Modifier.fillMaxSize(),
                    // 在 shared element transition 动画进行时禁用滚动
                    userScrollEnabled = !isTransitionActive
                ) {
                    // 自定义头部内容
                    headerContent?.invoke(this)

                    // Illust 列表
                    items(illusts, key = { it.id }) { illust ->
                        IllustCard(
                            illust = illust,
                            onClick = { onIllustClick(illust) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            isSelectionMode = selectionManager?.isSelectionMode ?: false,
                            isSelected = selectionManager?.isSelected(illust.id) ?: false,
                            onLongPress = selectionManager?.let { { it.onLongPress(illust) } },
                            onSelectionToggle = selectionManager?.let { { it.toggleSelection(illust) } },
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
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
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
