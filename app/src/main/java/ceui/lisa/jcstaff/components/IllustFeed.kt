package ceui.lisa.jcstaff.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.animations.ElasticPullToRefresh
import ceui.lisa.jcstaff.components.animations.SkeletonFeedCard
import ceui.lisa.jcstaff.components.animations.staggeredFadeIn
import ceui.lisa.jcstaff.core.PagedState
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 社交信息流风格的插画列表
 * 使用 LazyColumn 纵向排列 IllustFeedCard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IllustFeed(
    state: PagedState<Illust>,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val navViewModel = LocalNavigationViewModel.current

    // 检测滚动到底部，触发加载更多
    if (onLoadMore != null) {
        LaunchedEffect(listState, state.canLoadMore) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleItem >= totalItems - 3
            }
                .distinctUntilChanged()
                .filter { it && state.canLoadMore && !state.isLoadingMore }
                .collect {
                    onLoadMore()
                }
        }
    }

    val content: @Composable () -> Unit = {
        when {
            state.error != null && state.items.isEmpty() -> {
                if (onRefresh != null) {
                    ErrorRetryState(
                        error = state.error,
                        onRetry = onRefresh
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            state.items.isEmpty() && state.isLoading -> {
                // Shimmer skeleton loading
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    items(6) { index ->
                        SkeletonFeedCard(
                            modifier = Modifier.staggeredFadeIn(index)
                        )
                    }
                }
            }
            state.items.isEmpty() -> {
                EmptyRefreshableState(onRefresh = onRefresh)
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = state.items,
                        key = { it.id }
                    ) { illust ->
                        IllustFeedCard(
                            modifier = Modifier.animateItem(),
                            illust = illust,
                            onClick = {
                                navViewModel.navigate(
                                    NavRoute.IllustDetail(
                                        illustId = illust.id,
                                        title = illust.title ?: "",
                                        previewUrl = illust.previewUrl(),
                                        aspectRatio = illust.aspectRatio()
                                    )
                                )
                            }
                        )
                    }

                    // 加载更多指示器
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
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

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) userPulled = false
        }

        ElasticPullToRefresh(
            isRefreshing = userPulled && state.isLoading,
            onRefresh = {
                if (!state.isLoading) {
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
