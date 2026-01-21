package ceui.lisa.jcstaff.home

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.core.SettingsStore
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustCard
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.User
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

data class IllustClickData(
    val id: Long,
    val title: String,
    val previewUrl: String,
    val aspectRatio: Float
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope?,
    currentUser: User?,
    onIllustClick: (IllustClickData) -> Unit,
    onBookmarksClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val tabs = listOf("推荐", "关注")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                user = currentUser,
                onBookmarksClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        onBookmarksClick()
                    }
                },
                onFollowingClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        pagerState.animateScrollToPage(1)
                    }
                },
                onSettingsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        onSettingsClick()
                    }
                },
                onLogoutClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        onLogoutClick()
                    }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("JCStaff") },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            UserAvatar(
                                user = currentUser,
                                size = 32
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }

                // 为两个 Tab 分别创建持久化的滚动状态
                val recommendedGridState = rememberPersistentLazyStaggeredGridState("home_recommended")
                val followingGridState = rememberPersistentLazyStaggeredGridState("home_following")

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> IllustGrid(
                            illusts = uiState.recommendedIllusts,
                            isLoading = uiState.isLoadingRecommended,
                            error = uiState.recommendedError,
                            gridState = recommendedGridState,
                            onRefresh = { homeViewModel.loadRecommendedIllusts() },
                            onIllustClick = { illust, previewUrl, aspectRatio ->
                                onIllustClick(IllustClickData(
                                    id = illust.id,
                                    title = illust.title ?: "",
                                    previewUrl = previewUrl,
                                    aspectRatio = aspectRatio
                                ))
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                        1 -> IllustGrid(
                            illusts = uiState.followingIllusts,
                            isLoading = uiState.isLoadingFollowing,
                            error = uiState.followingError,
                            gridState = followingGridState,
                            onRefresh = { homeViewModel.loadFollowingIllusts() },
                            onIllustClick = { illust, previewUrl, aspectRatio ->
                                onIllustClick(IllustClickData(
                                    id = illust.id,
                                    title = illust.title ?: "",
                                    previewUrl = previewUrl,
                                    aspectRatio = aspectRatio
                                ))
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun IllustGrid(
    illusts: List<Illust>,
    isLoading: Boolean,
    error: String?,
    gridState: LazyStaggeredGridState,
    onRefresh: () -> Unit,
    onIllustClick: (Illust, String, Float) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope?
) {
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    val density = LocalDensity.current
    val spacing = if (gridSpacingEnabled) 8.dp else with(density) { 1f.toDp() }
    val contentPadding = if (gridSpacingEnabled) PaddingValues(8.dp) else PaddingValues(0.dp)

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
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
                        Text(
                            text = "下拉刷新重试",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
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
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalItemSpacing = spacing,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(illusts, key = { it.id }) { illust ->
                        IllustCard(
                            illust = illust,
                            onClick = {
                                onIllustClick(illust, illust.previewUrl(), illust.aspectRatio())
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(
    user: User?,
    size: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (user?.profile_image_urls?.medium != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(user.profile_image_urls.medium)
                .crossfade(true)
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build(),
            contentDescription = user.name,
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "用户",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((size * 0.6).dp)
            )
        }
    }
}

@Composable
private fun DrawerContent(
    user: User?,
    onBookmarksClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        // User header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(24.dp)
        ) {
            UserAvatar(user = user, size = 64)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user?.name ?: "未登录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (user?.account != null) {
                Text(
                    text = "@${user.account}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Menu items
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null
                )
            },
            label = { Text("我的收藏") },
            selected = false,
            onClick = onBookmarksClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },
            label = { Text("我的关注") },
            selected = false,
            onClick = onFollowingClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            },
            label = { Text("设置") },
            selected = false,
            onClick = onSettingsClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null
                )
            },
            label = { Text("退出登录") },
            selected = false,
            onClick = onLogoutClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

