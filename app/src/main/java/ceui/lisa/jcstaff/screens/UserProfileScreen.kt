package ceui.lisa.jcstaff.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.User
import ceui.lisa.jcstaff.network.UserProfile
import ceui.lisa.jcstaff.profile.UserProfileViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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
    val context = LocalContext.current

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    // 加载用户数据
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    // 检测 shared element transition
    val isTransitionActive = sharedTransitionScope.isTransitionActive

    // 设置
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val showIllustInfo by SettingsStore.showIllustInfo.collectAsState(initial = true)
    val illustCornerRadius by SettingsStore.illustCardCornerRadius.collectAsState(initial = 8)

    val density = LocalDensity.current
    val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
    val horizontalPadding = if (gridSpacingEnabled) 8.dp else 0.dp

    val gridState = rememberPersistentLazyStaggeredGridState("user_profile_$userId")

    // 检测滚动到底部加载更多
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.user?.name ?: "用户主页",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
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
                isRefreshing = state.isLoadingProfile && state.user != null,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isTransitionActive
                ) {
                    // 用户头部信息
                    item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                        UserProfileHeader(
                            user = state.user,
                            profile = state.profile,
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
                                    text = state.illustsError ?: "加载失败",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // 空状态
                    if (!state.isLoadingIllusts && state.illusts.isEmpty() && state.illustsError == null) {
                        item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无作品",
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
        }

        // Selection top bar overlay
        SelectionTopBar(
            selectionManager = selectionManager,
            allIllusts = state.illusts
        )
    }
}

@Composable
private fun UserProfileHeader(
    user: User?,
    profile: UserProfile?,
    isLoading: Boolean,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 加载中显示占位
        if (isLoading && user == null) {
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            return@Column
        }

        // 头像
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(user?.profile_image_urls?.medium)
                .crossfade(true)
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build(),
            contentDescription = user?.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 用户名
        Text(
            text = user?.name ?: "",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // @account
        Text(
            text = "@${user?.account ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 统计数据
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            StatItem(
                value = profile?.total_illusts ?: 0,
                label = "作品"
            )
            Spacer(modifier = Modifier.width(32.dp))
            StatItem(
                value = profile?.total_follow_users ?: 0,
                label = "关注"
            )
            Spacer(modifier = Modifier.width(32.dp))
            StatItem(
                value = profile?.total_illust_bookmarks_public ?: 0,
                label = "收藏"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 关注按钮
        val isFollowed = user?.is_followed == true
        Button(
            onClick = onFollowClick,
            enabled = !isFollowing && user != null,
            colors = if (isFollowed) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.width(120.dp)
        ) {
            if (isFollowing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isFollowed) "已关注" else "关注")
            }
        }

        // 简介
        if (!user?.comment.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user?.comment ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatItem(
    value: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatNumber(value),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1fw", num / 10000.0)
        num >= 1000 -> String.format("%.1fk", num / 1000.0)
        else -> num.toString()
    }
}
