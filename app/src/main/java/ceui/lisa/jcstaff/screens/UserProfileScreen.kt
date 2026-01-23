package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.components.FloatingTopBar
import ceui.lisa.jcstaff.components.user.UserProfileHeader
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.profile.UserProfileViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    userId: Long,
    onBackClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    viewModel: UserProfileViewModel = viewModel(key = "user_profile_$userId")
) {
    val state by viewModel.state.collectAsState()
    val selectionManager = rememberSelectionManager()

    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    val density = LocalDensity.current
    val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
    val horizontalPadding = if (gridSpacingEnabled) 8.dp else 0.dp

    val gridState = rememberPersistentLazyStaggeredGridState("user_profile_$userId")

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
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 0.dp,
                    bottom = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
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
                            CircularProgressIndicator()
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
            shareTitle = state.user?.name ?: "",
            onBackClick = onBackClick
        )

        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = state.illusts
        )
    }
}
