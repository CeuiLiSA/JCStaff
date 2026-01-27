package ceui.lisa.jcstaff.home

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.NovelList
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.rememberSelectionManager
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.User
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope?,
    currentUser: User?,
    onLogoutClick: () -> Unit,
    recommendedViewModel: RecommendedViewModel = viewModel(),
    trendingViewModel: TrendingViewModel = viewModel(),
    followingViewModel: FollowingViewModel = viewModel(),
    novelsViewModel: RecommendedNovelsViewModel = viewModel()
) {
    val navViewModel = LocalNavigationViewModel.current
    val recommendedState by recommendedViewModel.state.collectAsState()
    val trendingState by trendingViewModel.state.collectAsState()
    val followingState by followingViewModel.state.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectionManager = rememberSelectionManager()

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    // 当前页面的 illusts
    val currentIllusts = when (pagerState.currentPage) {
        0 -> recommendedState.illusts
        1 -> trendingState.illusts
        2 -> followingState.illusts
        else -> emptyList()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !selectionManager.isSelectionMode,
        drawerContent = {
            DrawerContent(
                user = currentUser,
                onUserProfileClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        currentUser?.let { user ->
                            navViewModel.navigate(NavRoute.UserProfile(userId = user.id))
                        }
                    }
                },
                onBookmarksClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        currentUser?.let { user ->
                            navViewModel.navigate(NavRoute.Bookmarks(userId = user.id))
                        }
                    }
                },
                onFollowingClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        pagerState.animateScrollToPage(2)
                    }
                },
                onBrowseHistoryClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.BrowseHistory)
                    }
                },
                onSettingsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.Settings)
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
        Box {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            ) {
                                UserAvatar(
                                    user = currentUser,
                                    size = 36
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentUser?.name ?: stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    if (currentUser?.account != null) {
                                        Text(
                                            text = "@${currentUser.account}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                navViewModel.navigate(NavRoute.Search)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_discover)) }
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_trending)) }
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 2,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_following)) }
                        )
                    }
                }
            ) { innerPadding ->
                // 为三个 Tab 分别创建持久化的滚动状态
                val recommendedGridState = rememberPersistentLazyStaggeredGridState("home_recommended")
                val trendingGridState = rememberPersistentLazyStaggeredGridState("home_trending")
                val followingGridState = rememberPersistentLazyStaggeredGridState("home_following")
                val novelListState = rememberLazyListState()

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { page ->
                    when (page) {
                        0 -> DiscoverPage(
                            recommendedViewModel = recommendedViewModel,
                            novelsViewModel = novelsViewModel,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            selectionManager = selectionManager,
                            recommendedGridState = recommendedGridState,
                            novelListState = novelListState
                        )
                        1 -> {
                            IllustGrid(
                                illusts = trendingState.illusts,
                                onIllustClick = { illust ->
                                    navViewModel.navigate(NavRoute.IllustDetail(
                                        illustId = illust.id,
                                        title = illust.title ?: "",
                                        previewUrl = illust.previewUrl(),
                                        aspectRatio = illust.aspectRatio()
                                    ))
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope,
                                isLoading = trendingState.isLoading,
                                isLoadingMore = trendingState.isLoadingMore,
                                canLoadMore = trendingState.canLoadMore,
                                error = trendingState.error,
                                onRefresh = { trendingViewModel.load() },
                                onLoadMore = { trendingViewModel.loadMore() },
                                selectionManager = selectionManager,
                                gridState = trendingGridState
                            )
                        }
                        2 -> {
                            IllustGrid(
                                illusts = followingState.illusts,
                                onIllustClick = { illust ->
                                    navViewModel.navigate(NavRoute.IllustDetail(
                                        illustId = illust.id,
                                        title = illust.title ?: "",
                                        previewUrl = illust.previewUrl(),
                                        aspectRatio = illust.aspectRatio()
                                    ))
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope,
                                isLoading = followingState.isLoading,
                                isLoadingMore = followingState.isLoadingMore,
                                canLoadMore = followingState.canLoadMore,
                                error = followingState.error,
                                onRefresh = { followingViewModel.load() },
                                onLoadMore = { followingViewModel.loadMore() },
                                selectionManager = selectionManager,
                                gridState = followingGridState
                            )
                        }
                    }
                }
        }

            // Selection top bar overlay
            SelectionTopBar(
                selectionManager = selectionManager,
                allIllusts = currentIllusts
            )
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
                contentDescription = stringResource(R.string.user_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((size * 0.6).dp)
            )
        }
    }
}

@Composable
private fun DrawerContent(
    user: User?,
    onUserProfileClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBrowseHistoryClick: () -> Unit,
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
                text = user?.name ?: stringResource(R.string.not_logged_in),
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
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.my_profile)) },
            selected = false,
            onClick = onUserProfileClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.my_bookmarks)) },
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
            label = { Text(stringResource(R.string.my_following)) },
            selected = false,
            onClick = onFollowingClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.browse_history)) },
            selected = false,
            onClick = onBrowseHistoryClick,
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
            label = { Text(stringResource(R.string.settings)) },
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
            label = { Text(stringResource(R.string.logout)) },
            selected = false,
            onClick = onLogoutClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DiscoverPage(
    recommendedViewModel: RecommendedViewModel,
    novelsViewModel: RecommendedNovelsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope?,
    selectionManager: ceui.lisa.jcstaff.core.SelectionManager,
    recommendedGridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    novelListState: androidx.compose.foundation.lazy.LazyListState
) {
    val navViewModel = LocalNavigationViewModel.current
    val recommendedState by recommendedViewModel.state.collectAsState()
    val novelsState by novelsViewModel.state.collectAsState()

    val innerPagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Inner TabRow for Illusts / Novels
        TabRow(
            selectedTabIndex = innerPagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = innerPagerState.currentPage == 0,
                onClick = {
                    coroutineScope.launch { innerPagerState.animateScrollToPage(0) }
                },
                text = { Text(stringResource(R.string.tab_illust)) }
            )
            Tab(
                selected = innerPagerState.currentPage == 1,
                onClick = {
                    coroutineScope.launch { innerPagerState.animateScrollToPage(1) }
                },
                text = { Text(stringResource(R.string.tab_novel)) }
            )
        }

        // Inner HorizontalPager
        HorizontalPager(
            state = innerPagerState,
            modifier = Modifier.fillMaxSize()
        ) { innerPage ->
            when (innerPage) {
                0 -> IllustGrid(
                    illusts = recommendedState.illusts,
                    onIllustClick = { illust ->
                        navViewModel.navigate(NavRoute.IllustDetail(
                            illustId = illust.id,
                            title = illust.title ?: "",
                            previewUrl = illust.previewUrl(),
                            aspectRatio = illust.aspectRatio()
                        ))
                    },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    isLoading = recommendedState.isLoading,
                    isLoadingMore = recommendedState.isLoadingMore,
                    canLoadMore = recommendedState.canLoadMore,
                    error = recommendedState.error,
                    onRefresh = { recommendedViewModel.refresh() },
                    onLoadMore = { recommendedViewModel.loadMore() },
                    selectionManager = selectionManager,
                    gridState = recommendedGridState,
                    headerContent = if (recommendedState.rankingIllusts.isNotEmpty()) {
                        {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                RankingCarousel(
                                    illusts = recommendedState.rankingIllusts,
                                    onIllustClick = { illust ->
                                        navViewModel.navigate(NavRoute.IllustDetail(
                                            illustId = illust.id,
                                            title = illust.title ?: "",
                                            previewUrl = illust.previewUrl(),
                                            aspectRatio = illust.aspectRatio()
                                        ))
                                    }
                                )
                            }
                        }
                    } else null
                )
                1 -> NovelList(
                    novels = novelsState.novels,
                    onNovelClick = { novel ->
                        navViewModel.navigate(NavRoute.NovelDetail(novelId = novel.id))
                    },
                    isLoading = novelsState.isLoading,
                    isLoadingMore = novelsState.isLoadingMore,
                    canLoadMore = novelsState.canLoadMore,
                    error = novelsState.error,
                    onRefresh = { novelsViewModel.refresh() },
                    onLoadMore = { novelsViewModel.loadMore() },
                    listState = novelListState
                )
            }
        }
    }
}

@Composable
private fun RankingCarousel(
    illusts: List<Illust>,
    onIllustClick: (Illust) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.daily_ranking),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(illusts, key = { _, illust -> "ranking_${illust.id}" }) { index, illust ->
                RankingCard(
                    illust = illust,
                    rank = index + 1,
                    onClick = { onIllustClick(illust) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun RankingCard(
    illust: Illust,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(illust.image_urls?.square_medium ?: illust.previewUrl())
                    .crossfade(true)
                    .addHeader("Referer", "https://app-api.pixiv.net/")
                    .build(),
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
            )

            // Rank badge
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Title
        Text(
            text = illust.title ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
