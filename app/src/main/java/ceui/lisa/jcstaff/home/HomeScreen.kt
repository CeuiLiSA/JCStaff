package ceui.lisa.jcstaff.home

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.auth.AccountEntry
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
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.TrendingTag
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
    allAccounts: List<AccountEntry> = emptyList(),
    activeUserId: Long? = null,
    onSwitchAccount: (Long) -> Unit = {},
    onAddAccount: () -> Unit = {},
    recommendedViewModel: RecommendedViewModel = viewModel(),
    trendingTagsViewModel: TrendingTagsViewModel = viewModel(),
    followingViewModel: FollowingViewModel = viewModel(),
    novelsViewModel: RecommendedNovelsViewModel = viewModel()
) {
    val navViewModel = LocalNavigationViewModel.current
    val recommendedState by recommendedViewModel.state.collectAsState()
    val trendingTagsState by trendingTagsViewModel.state.collectAsState()
    val followingState by followingViewModel.state.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectionManager = rememberSelectionManager()

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    // 当前页面的 illusts（用于选择模式）
    val currentIllusts = when (pagerState.currentPage) {
        0 -> recommendedState.illusts
        2 -> followingState.illusts
        else -> emptyList()
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
                // 在 Pager 外部提升滚动状态，配合 SaveableStateProvider 自动持久化
                val recommendedGridState = rememberLazyStaggeredGridState()
                val followingGridState = rememberLazyStaggeredGridState()
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
                            TrendingTagsPage(
                                trendingTagsState = trendingTagsState,
                                onRefreshIllust = { trendingTagsViewModel.loadIllustTags() },
                                onRefreshNovel = { trendingTagsViewModel.loadNovelTags() },
                                onTagClick = { tag, initialTab ->
                                    navViewModel.navigate(NavRoute.TagDetail(tag = tag, initialTab = initialTab))
                                }
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

    val avatarUrl = user?.profile_image_urls?.findAvatarUrl()
    if (avatarUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = user?.name,
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
    onFollowingClick: () -> Unit,
    onBrowseHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShaderDemoClick: () -> Unit
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
            DrawerMenuItem(
                icon = Icons.Default.Person,
                label = stringResource(R.string.my_following),
                onClick = onFollowingClick
            )
            DrawerMenuItem(
                icon = Icons.Default.History,
                label = stringResource(R.string.browse_history),
                onClick = onBrowseHistoryClick
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

            Spacer(modifier = Modifier.height(
                16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ))
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
private fun TrendingTagsPage(
    trendingTagsState: TrendingTagsUiState,
    onRefreshIllust: () -> Unit,
    onRefreshNovel: () -> Unit,
    onTagClick: (Tag, Int) -> Unit
) {
    val innerPagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    // 在 Pager 外部提升 grid 滚动状态，页面切换时不会丢失
    val illustGridState = rememberLazyGridState()
    val novelGridState = rememberLazyGridState()

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = { Text(stringResource(R.string.tab_trending_illust)) }
            )
            Tab(
                selected = innerPagerState.currentPage == 1,
                onClick = {
                    coroutineScope.launch { innerPagerState.animateScrollToPage(1) }
                },
                text = { Text(stringResource(R.string.tab_trending_novel)) }
            )
        }

        HorizontalPager(
            state = innerPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> TrendingTagGrid(
                    tags = trendingTagsState.illustTags,
                    isLoading = trendingTagsState.isIllustLoading,
                    error = trendingTagsState.illustError,
                    onRefresh = onRefreshIllust,
                    onTagClick = { tag -> onTagClick(tag, 0) },
                    gridState = illustGridState
                )
                1 -> TrendingTagGrid(
                    tags = trendingTagsState.novelTags,
                    isLoading = trendingTagsState.isNovelLoading,
                    error = trendingTagsState.novelError,
                    onRefresh = onRefreshNovel,
                    onTagClick = { tag -> onTagClick(tag, 1) },
                    gridState = novelGridState
                )
            }
        }
    }
}

@Composable
private fun TrendingTagGrid(
    tags: List<TrendingTag>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onTagClick: (Tag) -> Unit,
    gridState: LazyGridState = rememberLazyGridState()
) {
    if (isLoading && tags.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null && tags.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.load_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    onClick = {
                        val navTag = Tag(
                            name = tags[0].tag,
                            translated_name = tags[0].translated_name
                        )
                        onTagClick(navTag)
                    }
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
                onClick = {
                    val navTag = Tag(
                        name = tag.tag,
                        translated_name = tag.translated_name
                    )
                    onTagClick(navTag)
                }
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
            .clip(RoundedCornerShape(8.dp))
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
