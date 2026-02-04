package ceui.lisa.jcstaff.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.auth.AccountEntry
import ceui.lisa.jcstaff.cache.BrowseHistoryRepository
import ceui.lisa.jcstaff.components.ErrorRetryState
import ceui.lisa.jcstaff.components.IllustFeed
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.LoadingIndicator
import ceui.lisa.jcstaff.components.NovelList
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.LocalSelectionManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.navigation.LocalNavigationViewModel
import ceui.lisa.jcstaff.navigation.NavRoute
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.TrendingTag
import ceui.lisa.jcstaff.network.User
import ceui.lisa.jcstaff.network.UserPreview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: User?,
    allAccounts: List<AccountEntry> = emptyList(),
    activeUserId: Long? = null,
    onSwitchAccount: (Long) -> Unit = {},
    onAddAccount: () -> Unit = {},
) {
    val navViewModel = LocalNavigationViewModel.current

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectionManager = LocalSelectionManager.current

    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    // 侧边栏展开时，返回手势优先收起侧边栏
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !selectionManager.isSelectionMode,
        drawerContent = {
            DrawerContent(
                user = currentUser,
                allAccounts = allAccounts,
                activeUserId = activeUserId,
                onSwitchAccount = { userId ->
                    coroutineScope.launch {
                        drawerState.close()
                        onSwitchAccount(userId)
                    }
                },
                onAddAccount = {
                    coroutineScope.launch {
                        drawerState.close()
                        onAddAccount()
                    }
                },
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
                onBrowseHistoryClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.BrowseHistory)
                    }
                },
                onDownloadHistoryClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.DownloadHistory)
                    }
                },
                onUgoiraRankingClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.UgoiraRanking)
                    }
                },
                onLatestWorksClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.LatestWorks)
                    }
                },
                onSauceNaoClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.SauceNao)
                    }
                },
                onSettingsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.Settings)
                    }
                },
                onShaderDemoClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        navViewModel.navigate(NavRoute.ShaderDemo)
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
                                        text = currentUser?.name
                                            ?: stringResource(R.string.app_name),
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
                            label = { Text(stringResource(R.string.tab_recommended)) }
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_discover)) }
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 2,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            },
                            icon = { Icon(Icons.Default.FiberNew, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_general)) }
                        )
                        NavigationBarItem(
                            selected = pagerState.currentPage == 3,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            label = { Text(stringResource(R.string.tab_new_works)) }
                        )
                    }
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    userScrollEnabled = false
                ) { page ->
                    when (page) {
                        0 -> RecommendedTabPage()
                        1 -> DiscoverTabPage()
                        2 -> GeneralTabPage()
                        3 -> NewWorksTabPage()
                    }
                }
            }

            SelectionTopBar(allIllusts = emptyList())
        }
    }
}

// ==================== Tab 1: 推荐 ====================

@Composable
private fun RecommendedTabPage() {
    val navViewModel = LocalNavigationViewModel.current
    val innerPagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.tab_illust),
        stringResource(R.string.manga),
        stringResource(R.string.tab_novel)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = innerPagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (innerPagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[innerPagerState.currentPage]),
                        width = 32.dp,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = innerPagerState.currentPage == index,
                    onClick = { coroutineScope.launch { innerPagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (innerPagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = innerPagerState,
            modifier = Modifier.fillMaxSize()
        ) { innerPage ->
            when (innerPage) {
                0 -> {
                    val vm: RecommendedContentViewModel = viewModel(
                        key = "recommended_illust",
                        factory = RecommendedContentViewModel.factory("illust")
                    )
                    val state by vm.state.collectAsState()
                    IllustGrid(
                        illusts = state.illusts,
                        onIllustClick = { illust ->
                            navViewModel.navigate(
                                NavRoute.IllustDetail(
                                    illustId = illust.id,
                                    title = illust.title ?: "",
                                    previewUrl = illust.previewUrl(),
                                    aspectRatio = illust.aspectRatio()
                                )
                            )
                        },
                        isLoading = state.isLoading,
                        isLoadingMore = state.isLoadingMore,
                        canLoadMore = state.canLoadMore,
                        error = state.error,
                        onRefresh = { vm.refresh() },
                        onLoadMore = { vm.loadMore() },
                        headerContent = {
                            if (state.rankingIllusts.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    RankingCarousel(
                                        illusts = state.rankingIllusts,
                                        onIllustClick = { illust ->
                                            navViewModel.navigate(
                                                NavRoute.IllustDetail(
                                                    illustId = illust.id,
                                                    title = illust.title ?: "",
                                                    previewUrl = illust.previewUrl(),
                                                    aspectRatio = illust.aspectRatio()
                                                )
                                            )
                                        },
                                        onViewAllClick = {
                                            navViewModel.navigate(NavRoute.RankingDetail(objectType = "illust"))
                                        }
                                    )
                                }
                            }
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader(
                                    title = stringResource(R.string.recommended_for_you),
                                    subtitle = stringResource(R.string.recommended_illust_subtitle)
                                )
                            }
                        }
                    )
                }

                1 -> {
                    val vm: RecommendedContentViewModel = viewModel(
                        key = "recommended_manga",
                        factory = RecommendedContentViewModel.factory("manga")
                    )
                    val state by vm.state.collectAsState()
                    IllustGrid(
                        illusts = state.illusts,
                        onIllustClick = { illust ->
                            navViewModel.navigate(
                                NavRoute.IllustDetail(
                                    illustId = illust.id,
                                    title = illust.title ?: "",
                                    previewUrl = illust.previewUrl(),
                                    aspectRatio = illust.aspectRatio()
                                )
                            )
                        },
                        isLoading = state.isLoading,
                        isLoadingMore = state.isLoadingMore,
                        canLoadMore = state.canLoadMore,
                        error = state.error,
                        onRefresh = { vm.refresh() },
                        onLoadMore = { vm.loadMore() },
                        headerContent = {
                            if (state.rankingIllusts.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    RankingCarousel(
                                        illusts = state.rankingIllusts,
                                        onIllustClick = { illust ->
                                            navViewModel.navigate(
                                                NavRoute.IllustDetail(
                                                    illustId = illust.id,
                                                    title = illust.title ?: "",
                                                    previewUrl = illust.previewUrl(),
                                                    aspectRatio = illust.aspectRatio()
                                                )
                                            )
                                        },
                                        onViewAllClick = {
                                            navViewModel.navigate(NavRoute.RankingDetail(objectType = "manga"))
                                        }
                                    )
                                }
                            }
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader(
                                    title = stringResource(R.string.recommended_for_you),
                                    subtitle = stringResource(R.string.recommended_manga_subtitle)
                                )
                            }
                        }
                    )
                }

                2 -> {
                    val vm: RecommendedNovelsViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    NovelList(
                        state = state,
                        onRefresh = { vm.refresh() },
                        onLoadMore = { vm.loadMore() },
                    )
                }
            }
        }
    }
}

// ==================== Tab 2: 发现 ====================

@Composable
private fun DiscoverTabPage() {
    val navViewModel = LocalNavigationViewModel.current
    val trendingIllustTagsViewModel: TrendingIllustTagsViewModel = viewModel()
    val trendingNovelTagsViewModel: TrendingNovelTagsViewModel = viewModel()
    val recommendedUsersViewModel: RecommendedUsersViewModel = viewModel()
    val illustTagsState by trendingIllustTagsViewModel.state.collectAsState()
    val novelTagsState by trendingNovelTagsViewModel.state.collectAsState()
    val usersState by recommendedUsersViewModel.state.collectAsState()

    val innerPagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.tab_illust_manga),
        stringResource(R.string.tab_novel),
        stringResource(R.string.tab_recommended_users)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = innerPagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (innerPagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[innerPagerState.currentPage]),
                        width = 32.dp,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = innerPagerState.currentPage == index,
                    onClick = { coroutineScope.launch { innerPagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (innerPagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = innerPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> TrendingTagGrid(
                    tags = illustTagsState.items,
                    isLoading = illustTagsState.isLoading,
                    error = illustTagsState.error,
                    onRefresh = { trendingIllustTagsViewModel.refresh() },
                    onTagClick = { tag ->
                        val navTag = Tag(name = tag.tag, translated_name = tag.translated_name)
                        BrowseHistoryRepository.recordSearch(navTag)
                        navViewModel.navigate(NavRoute.WebTagDetail(tag = navTag))
                    }
                )

                1 -> TrendingTagGrid(
                    tags = novelTagsState.items,
                    isLoading = novelTagsState.isLoading,
                    error = novelTagsState.error,
                    onRefresh = { trendingNovelTagsViewModel.refresh() },
                    onTagClick = { tag ->
                        val navTag = Tag(name = tag.tag, translated_name = tag.translated_name)
                        BrowseHistoryRepository.recordSearch(navTag)
                        navViewModel.navigate(NavRoute.TagDetail(tag = navTag, initialTab = 1))
                    }
                )

                2 -> RecommendedUsersList(
                    usersState = usersState,
                    onRefresh = { recommendedUsersViewModel.refresh() },
                    onLoadMore = { recommendedUsersViewModel.loadMore() },
                    onUserClick = { userId ->
                        navViewModel.navigate(NavRoute.UserProfile(userId = userId))
                    },
                    onIllustClick = { illust ->
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
        }
    }
}

// ==================== Tab 3: 新作 ====================

@Composable
private fun NewWorksTabPage() {
    val innerPagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.tab_following_illust_manga),
        stringResource(R.string.tab_following_novel)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = innerPagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (innerPagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[innerPagerState.currentPage]),
                        width = 32.dp,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = innerPagerState.currentPage == index,
                    onClick = { coroutineScope.launch { innerPagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (innerPagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = innerPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    val vm: FollowingViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    IllustFeed(
                        state = state,
                        onRefresh = { vm.refresh() },
                        onLoadMore = { vm.loadMore() }
                    )
                }

                1 -> {
                    val vm: FollowingNovelsViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    NovelList(
                        state = state,
                        onRefresh = { vm.refresh() },
                        onLoadMore = { vm.loadMore() },
                    )
                }
            }
        }
    }
}

// ==================== 共用组件 ====================

@Composable
private fun UserAvatar(
    user: User?,
    size: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val avatarUrl = user?.profile_image_urls?.findAvatarUrl()
    if (avatarUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = user?.name,
            contentScale = ContentScale.Crop,
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
private fun AccountAvatarSmall(
    avatarUrl: String?,
    name: String,
    size: Int = 32,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (avatarUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size.dp)
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ),
                    CircleShape
                )
                .padding(2.dp)
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
                contentDescription = name,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((size * 0.6).dp)
            )
        }
    }
}

@Composable
private fun DrawerContent(
    user: User?,
    allAccounts: List<AccountEntry>,
    activeUserId: Long?,
    onSwitchAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onUserProfileClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onBrowseHistoryClick: () -> Unit,
    onDownloadHistoryClick: () -> Unit,
    onUgoiraRankingClick: () -> Unit,
    onLatestWorksClick: () -> Unit,
    onSauceNaoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShaderDemoClick: () -> Unit,
    onAppSwitcherDemoClick: () -> Unit
) {
    var accountListExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier.width(304.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header with gradient background ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = statusBarTop + 16.dp,
                            bottom = 20.dp
                        )
                ) {
                    // Avatar with gradient ring + premium badge
                    Box {
                        val avatarUrl = user?.profile_image_urls?.findAvatarUrl()
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = user?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        // Premium badge
                        if (user?.is_premium == true) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name + account switcher toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { accountListExpanded = !accountListExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: stringResource(R.string.not_logged_in),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (user?.account != null) {
                                Text(
                                    text = "@${user.account}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (allAccounts.isNotEmpty()) {
                            val rotation by animateFloatAsState(
                                targetValue = if (accountListExpanded) 180f else 0f,
                                animationSpec = tween(300),
                                label = "arrow_rotation"
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.switch_account),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer { rotationZ = rotation }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Account switcher (expandable) ────────────────────────
            AnimatedVisibility(
                visible = accountListExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                        .padding(vertical = 8.dp)
                ) {
                    allAccounts.forEach { account ->
                        val isActive = account.userId == activeUserId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isActive) onSwitchAccount(account.userId)
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AccountAvatarSmall(
                                avatarUrl = account.avatarUrl,
                                name = account.userName,
                                size = 36
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.userName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${account.userAccount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Add account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddAccount() }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.add_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Menu items ───────────────────────────────────────────
            DrawerMenuItem(
                icon = Icons.Default.AccountCircle,
                label = stringResource(R.string.my_profile),
                onClick = onUserProfileClick
            )
            DrawerMenuItem(
                icon = Icons.Default.Favorite,
                label = stringResource(R.string.my_bookmarks),
                onClick = onBookmarksClick
            )

            // ── 历史记录分区 ──────────────────────────────────────────
            DrawerSectionHeader(title = stringResource(R.string.drawer_section_history))

            DrawerMenuItem(
                icon = Icons.Default.History,
                label = stringResource(R.string.browse_history),
                onClick = onBrowseHistoryClick
            )
            DrawerMenuItem(
                icon = Icons.Default.Download,
                label = stringResource(R.string.download_history),
                onClick = onDownloadHistoryClick
            )

            // ── 更多功能 ────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))

            DrawerMenuItem(
                icon = Icons.Default.PlayCircle,
                label = stringResource(R.string.ugoira_ranking),
                onClick = onUgoiraRankingClick
            )
            DrawerMenuItem(
                icon = Icons.Default.FiberNew,
                label = stringResource(R.string.latest_works),
                onClick = onLatestWorksClick
            )
            DrawerMenuItem(
                icon = Icons.Default.ImageSearch,
                label = stringResource(R.string.sauce_nao),
                onClick = onSauceNaoClick
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer ───────────────────────────────────────────────
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            DrawerMenuItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.settings),
                onClick = onSettingsClick
            )
            DrawerMenuItem(
                icon = Icons.Default.AutoAwesome,
                label = "Shader Demo",
                onClick = onShaderDemoClick
            )
            DrawerMenuItem(
                icon = Icons.Default.Layers,
                label = "卡片切换测试",
                onClick = onAppSwitcherDemoClick
            )

            Spacer(
                modifier = Modifier.height(
                    16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }

}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor
        )
    }
}

@Composable
private fun DrawerSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

// ==================== Ranking Carousel ====================

private val RankGold = Color(0xFFFFD700)
private val RankSilver = Color(0xFFC0C0C0)
private val RankBronze = Color(0xFFCD7F32)

@Composable
private fun RankingCarousel(
    illusts: List<Illust>,
    onIllustClick: (Illust) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Check if grid spacing is enabled to determine layout behavior
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)

    // Calculate header padding to align with LazyRow cards
    // When grid spacing enabled: Grid has 8dp padding, so header needs 8dp more = 16dp total
    // When disabled: Grid has 0dp padding, so header needs 10dp
    val headerHorizontalPadding = if (gridSpacingEnabled) 8.dp else 10.dp

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // App Store style header - padding aligns with cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = headerHorizontalPadding,
                    end = headerHorizontalPadding,
                    top = 20.dp,
                    bottom = 12.dp
                )
        ) {
            // Subtitle (uppercase, small)
            Text(
                text = stringResource(R.string.ranking_subtitle).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large bold title
                Text(
                    text = stringResource(R.string.daily_ranking),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // See All button
                Surface(
                    onClick = onViewAllClick,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.view_all),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Cards carousel - extend to screen edges when grid spacing is enabled
        // contentPadding should align first card with grid content
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(if (gridSpacingEnabled) 8.dp else 1.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = if (gridSpacingEnabled) 16.dp else 10.dp
            ),
            modifier = if (gridSpacingEnabled) {
                Modifier.layout { measurable, constraints ->
                    // Extend LazyRow to fill screen edge-to-edge
                    val horizontalPadding = 8.dp.roundToPx()
                    val expandedConstraints = constraints.copy(
                        maxWidth = constraints.maxWidth + horizontalPadding * 2
                    )
                    val placeable = measurable.measure(expandedConstraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(-horizontalPadding, 0)
                    }
                }
            } else {
                Modifier
            }
        ) {
            itemsIndexed(illusts, key = { _, illust -> "ranking_${illust.id}" }) { index, illust ->
                RankingCard(
                    illust = illust,
                    rank = index + 1,
                    onClick = { onIllustClick(illust) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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

    // Rank colors for top 3
    val rankColor = when (rank) {
        1 -> RankGold
        2 -> RankSilver
        3 -> RankBronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val rankTextColor = when (rank) {
        1, 2, 3 -> Color.Black.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (rank <= 3) 8.dp else 4.dp,
        tonalElevation = if (rank <= 3) 2.dp else 0.dp
    ) {
        Column {
            Box {
                // Main image
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(illust.image_urls?.medium ?: illust.previewUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = illust.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )

                // Rank badge - special design for top 3
                if (rank <= 3) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        rankColor,
                                        rankColor.copy(alpha = 0.8f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.2f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$rank",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = rankTextColor
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Author avatar at bottom right of image
                illust.user?.let { user ->
                    val avatarUrl = user.profile_image_urls?.findAvatarUrl()
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .border(
                                    width = 2.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            // Title and author info
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = illust.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = illust.user?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stats row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(illust.total_bookmarks ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 10000 -> String.format("%.1fK", count / 1000.0)
        count >= 1000 -> String.format("%.1fK", count / 1000.0)
        else -> count.toString()
    }
}

// ==================== Section Header (App Store Style) ====================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gridSpacingEnabled by SettingsStore.gridSpacingEnabled.collectAsState(initial = true)
    // When grid spacing enabled: Grid has 8dp padding, so need 8dp more = 16dp total (align with RankingCarousel)
    // When disabled: Grid has 0dp padding, so need 10dp
    val horizontalPadding = if (gridSpacingEnabled) 8.dp else 10.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 0.dp)
            .padding(top = 20.dp, bottom = 12.dp)
    ) {
        // Subtitle (appears above title in App Store style)
        if (subtitle != null) {
            Text(
                text = subtitle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large bold title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // See All link
            if (onSeeAllClick != null) {
                Text(
                    text = stringResource(R.string.view_all),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onSeeAllClick)
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                )
            }
        }
    }
}

// ==================== Trending Tag Grid ====================

@Composable
private fun TrendingTagGrid(
    tags: List<TrendingTag>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onTagClick: (TrendingTag) -> Unit,
    gridState: LazyGridState = rememberLazyGridState()
) {
    if (isLoading && tags.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
        return
    }

    if (error != null && tags.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.load_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRefresh) {
                Text(stringResource(R.string.retry))
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // First item spans full width
        if (tags.isNotEmpty()) {
            item(
                key = "${tags[0].tag}_${tags[0].illust?.id ?: 0}_hero",
                span = { GridItemSpan(3) }
            ) {
                TrendingTagCard(
                    tag = tags[0],
                    isFirst = true,
                    onClick = { onTagClick(tags[0]) }
                )
            }
        }

        // Remaining items in 3-column grid
        val remaining = if (tags.size > 1) tags.subList(1, tags.size) else emptyList()
        itemsIndexed(
            items = remaining,
            key = { index, tag -> "${tag.tag}_${tag.illust?.id ?: (index + 1)}" }
        ) { _, tag ->
            TrendingTagCard(
                tag = tag,
                isFirst = false,
                onClick = { onTagClick(tag) }
            )
        }
    }
}

@Composable
private fun TrendingTagCard(
    tag: TrendingTag,
    isFirst: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageUrl = tag.illust?.image_urls?.large
        ?: tag.illust?.image_urls?.medium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        // Background image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = tag.tag,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark scrim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Tag text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            if (!tag.translated_name.isNullOrEmpty()) {
                Text(
                    text = tag.translated_name,
                    style = if (isFirst) MaterialTheme.typography.bodyMedium
                    else MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "#${tag.tag ?: ""}",
                style = if (isFirst) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== Recommended Users List ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedUsersList(
    usersState: RecommendedUsersUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (Long) -> Unit,
    onIllustClick: (Illust) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    // Detect scroll to bottom
    LaunchedEffect(listState, usersState.canLoadMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it && usersState.canLoadMore }
            .collect { onLoadMore() }
    }

    val content: @Composable () -> Unit = {
        when {
            usersState.error != null && usersState.users.isEmpty() -> {
                ErrorRetryState(
                    error = usersState.error,
                    onRetry = onRefresh
                )
            }

            usersState.users.isEmpty() && usersState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(usersState.users, key = { it.user?.id ?: 0L }) { userPreview ->
                        UserPreviewCard(
                            userPreview = userPreview,
                            onUserClick = { userPreview.user?.id?.let(onUserClick) },
                            onIllustClick = onIllustClick
                        )
                    }
                    if (usersState.isLoadingMore) {
                        item {
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

    var userPulled by remember { mutableStateOf(false) }
    LaunchedEffect(usersState.isLoading) {
        if (!usersState.isLoading) userPulled = false
    }

    PullToRefreshBox(
        isRefreshing = userPulled && usersState.isLoading,
        onRefresh = {
            if (!usersState.isLoading) {
                userPulled = true
                onRefresh()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
private fun UserPreviewCard(
    userPreview: UserPreview,
    onUserClick: () -> Unit,
    onIllustClick: (Illust) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user = userPreview.user ?: return

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ========== Header Section: Avatar + User Info + Follow Button ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUserClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar with premium ring
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    val avatarUrl = user.profile_image_urls?.findAvatarUrl()
                    val isPremium = user.is_premium == true

                    // Avatar border - gradient for premium, subtle for regular
                    val borderBrush = if (isPremium) {
                        Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFFD700), // Gold
                                Color(0xFFFFA500), // Orange
                                Color(0xFFFF6347), // Tomato
                                Color(0xFFFF69B4), // Hot pink
                                Color(0xFFFFD700)  // Gold (loop)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                            )
                        )
                    }

                    if (avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .border(
                                    width = if (isPremium) 2.5.dp else 2.dp,
                                    brush = borderBrush,
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .border(
                                    width = 2.dp,
                                    brush = borderBrush,
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Premium badge
                    if (isPremium) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFD700),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Premium",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // User info column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Name
                    Text(
                        text = user.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Account handle
                    if (!user.account.isNullOrBlank()) {
                        Text(
                            text = "@${user.account}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Illust titles (up to 3)
                    val illustTitles = userPreview.illusts.take(3).mapNotNull { it.title }
                        .filter { it.isNotBlank() }
                    if (illustTitles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = illustTitles.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bio/Comment (if available)
                    if (!user.comment.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Follow status indicator
                val isFollowed = user.is_followed == true
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isFollowed)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isFollowed)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(if (isFollowed) R.string.following else R.string.follow),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isFollowed)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ========== Sample Illusts Gallery ==========
            if (userPreview.illusts.isNotEmpty()) {
                val illusts = userPreview.illusts.take(3)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    illusts.forEachIndexed { index, illust ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(
                                    when (index) {
                                        0 -> RoundedCornerShape(
                                            topStart = 12.dp,
                                            bottomStart = 12.dp,
                                            topEnd = 4.dp,
                                            bottomEnd = 4.dp
                                        )

                                        illusts.size - 1 -> RoundedCornerShape(
                                            topStart = 4.dp,
                                            bottomStart = 4.dp,
                                            topEnd = 12.dp,
                                            bottomEnd = 12.dp
                                        )

                                        else -> RoundedCornerShape(4.dp)
                                    }
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onIllustClick(illust) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(illust.image_urls?.square_medium ?: illust.previewUrl())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = illust.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bookmark count overlay on each illust
                            val bookmarks = illust.total_bookmarks ?: 0
                            if (bookmarks > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = Color(0xFFFF6B6B)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = formatNumber(bookmarks),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = androidx.compose.ui.unit.TextUnit(
                                                10f,
                                                androidx.compose.ui.unit.TextUnitType.Sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to format large numbers
private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000.0)
        num >= 10_000 -> String.format("%.1fW", num / 10_000.0)
        num >= 1_000 -> String.format("%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}

// ==================== Tab 4: 综合 ====================

@Composable
private fun GeneralTabPage() {
    val innerPagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.tab_spotlight),
        stringResource(R.string.coming_soon)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = innerPagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (innerPagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[innerPagerState.currentPage]),
                        width = 32.dp,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = innerPagerState.currentPage == index,
                    onClick = { coroutineScope.launch { innerPagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (innerPagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = innerPagerState,
            modifier = Modifier.fillMaxSize()
        ) { innerPage ->
            when (innerPage) {
                0 -> SpotlightPage()
                1 -> EmptyPlaceholderPage()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotlightPage() {
    val navViewModel = LocalNavigationViewModel.current
    val vm: SpotlightViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.items.isNotEmpty(),
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }

            state.error != null && state.items.isEmpty() -> {
                ErrorRetryState(
                    error = state.error ?: stringResource(R.string.load_error),
                    onRetry = { vm.refresh() }
                )
            }

            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.spotlight_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                val listState = rememberLazyListState()

                // Load more when near the end
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .distinctUntilChanged()
                        .filter { lastIndex ->
                            lastIndex != null &&
                                    lastIndex >= state.items.size - 3 &&
                                    state.canLoadMore &&
                                    !state.isLoadingMore
                        }
                        .collect { vm.loadMore() }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(
                        count = state.items.size,
                        key = { index -> state.items[index].id }
                    ) { index ->
                        SpotlightCard(
                            article = state.items[index],
                            onClick = {
                                // Navigate to spotlight detail page
                                navViewModel.navigate(NavRoute.SpotlightDetail(article = state.items[index]))
                            }
                        )
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightCard(
    article: ceui.lisa.jcstaff.network.SpotlightArticle,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp
    ) {
        Box {
            // 背景图片
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(article.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = article.pure_title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            )

            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // 左上角分类标签
            if (article.subcategory_label != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = article.subcategory_label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // 底部内容区
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = article.pure_title ?: article.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.1f
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 日期
                if (article.publish_date != null) {
                    ceui.lisa.jcstaff.utils.formatRelativeDate(article.publish_date)
                        ?.let { dateText ->
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                }
            }
        }
    }
}


@Composable
private fun EmptyPlaceholderPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
