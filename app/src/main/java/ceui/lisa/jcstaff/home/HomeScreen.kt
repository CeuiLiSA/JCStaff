package ceui.lisa.jcstaff.home

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import ceui.lisa.jcstaff.core.rememberPersistentLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import ceui.lisa.jcstaff.components.IllustGrid
import ceui.lisa.jcstaff.components.SelectionTopBar
import ceui.lisa.jcstaff.core.rememberSelectionManager
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
    onSearchClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onBrowseHistoryClick: () -> Unit,
    onUserProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectionManager = rememberSelectionManager()

    // 返回键退出选择模式
    BackHandler(enabled = selectionManager.isSelectionMode) {
        selectionManager.clearSelection()
    }

    val tabRecommended = stringResource(R.string.tab_recommended)
    val tabFollowing = stringResource(R.string.tab_following)
    val tabs = listOf(tabRecommended, tabFollowing)

    // 当前页面的 illusts
    val currentIllusts = when (pagerState.currentPage) {
        0 -> uiState.recommendedIllusts
        1 -> uiState.followingIllusts
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
                        onUserProfileClick()
                    }
                },
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
                onBrowseHistoryClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        onBrowseHistoryClick()
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
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
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
                            onIllustClick = { illust ->
                                onIllustClick(IllustClickData(
                                    id = illust.id,
                                    title = illust.title ?: "",
                                    previewUrl = illust.previewUrl(),
                                    aspectRatio = illust.aspectRatio()
                                ))
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            isLoading = uiState.isLoadingRecommended,
                            isLoadingMore = uiState.isLoadingMoreRecommended,
                            canLoadMore = uiState.canLoadMoreRecommended,
                            error = uiState.recommendedError,
                            onRefresh = { homeViewModel.loadRecommendedIllusts() },
                            onLoadMore = { homeViewModel.loadMoreRecommended() },
                            selectionManager = selectionManager,
                            gridState = recommendedGridState
                        )
                        1 -> IllustGrid(
                            illusts = uiState.followingIllusts,
                            onIllustClick = { illust ->
                                onIllustClick(IllustClickData(
                                    id = illust.id,
                                    title = illust.title ?: "",
                                    previewUrl = illust.previewUrl(),
                                    aspectRatio = illust.aspectRatio()
                                ))
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            isLoading = uiState.isLoadingFollowing,
                            isLoadingMore = uiState.isLoadingMoreFollowing,
                            canLoadMore = uiState.canLoadMoreFollowing,
                            error = uiState.followingError,
                            onRefresh = { homeViewModel.loadFollowingIllusts() },
                            onLoadMore = { homeViewModel.loadMoreFollowing() },
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
