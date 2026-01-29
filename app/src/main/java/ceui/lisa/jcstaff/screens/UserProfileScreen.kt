package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.CircularProgressIndicator
import ceui.lisa.jcstaff.components.LoadingIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.cache.UserBrowseHistoryManager
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.components.FloatingTopBar
import ceui.lisa.jcstaff.components.user.UserProfileHeader
import ceui.lisa.jcstaff.core.SettingsStore
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.profile.UserProfileViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Long,
    viewModel: UserProfileViewModel = viewModel(key = "user_profile_$userId")
) {
    val navViewModel = LocalNavigationViewModel.current
    val state by viewModel.state.collectAsState()
    val selectionManager = LocalSelectionManager.current

    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    // 记录用户浏览历史
    LaunchedEffect(state.user) {
        state.user?.let { UserBrowseHistoryManager.recordView(it) }
    }

    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    val gridState = rememberLazyStaggeredGridState()

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

    Box {
        PullToRefreshBox(
            isRefreshing = state.isLoadingProfile && state.user != null,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = 0.dp,
                    bottom = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalItemSpacing = 4.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                // 沉浸式头部
                item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                    UserProfileHeader(
                        user = state.user,
                        profile = state.profile,
                        workspace = state.workspace,
                        isLoading = state.isLoadingProfile,
                        isFollowing = state.isFollowing,
                        onFollowClick = { viewModel.toggleFollow() }
                    )
                }

                // 加载中状态
                if (state.isLoadingIllusts && state.illusts.isEmpty()) {
                    item(key = "loading", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                }

                // 错误状态
                if (state.illustsError != null && state.illusts.isEmpty()) {
                    item(key = "error", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.illustsError ?: stringResource(R.string.load_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // 空状态
                if (!state.isLoadingIllusts && state.illusts.isEmpty() && state.illustsError == null && state.user != null) {
                    item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_works),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 作品列表
                items(state.illusts, key = { it.id }) { illust ->
                    IllustCard(
                        illust = illust,
                        onClick = {
                            navViewModel.navigate(NavRoute.IllustDetail(
                                illustId = illust.id,
                                title = illust.title ?: "",
                                previewUrl = illust.previewUrl(),
                                aspectRatio = illust.aspectRatio()
                            ))
                        },
                        showIllustInfo = showIllustInfo,
                        cornerRadius = illustCornerRadius
                    )
                }

                // 加载更多指示器
                if (state.isLoadingMore) {
                    item(key = "loading_more", span = StaggeredGridItemSpan.FullLine) {
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

        // 浮动顶部栏
        FloatingTopBar(
            shareUrl = "https://www.pixiv.net/users/$userId",
            shareTitle = state.user?.name ?: ""
        )

        SelectionTopBar(allIllusts = state.illusts)
    }
}
